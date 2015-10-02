/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.cocoon.servlet;

import org.apache.cocoon.configuration.Settings;
import org.apache.cocoon.servlet.RequestUtil;
import org.apache.cocoon.servlet.ServletSettings;
import org.apache.cocoon.servlet.multipart.MultipartConfigurationHelper;
import org.apache.cocoon.servlet.multipart.MultipartHttpServletRequest;
import org.apache.cocoon.util.AbstractLogEnabled;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.servlet.multipart.DSpaceRequestFactory;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.content.resumable.ResumableUpload;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

/**
 * Servlet filter for handling uploads
 * 
 */
public class DSpaceUploadFilter extends AbstractLogEnabled
                             implements Filter{

	private Logger LOG = Logger.getLogger(DSpaceUploadFilter.class);
    /**
     * The RequestFactory is responsible for wrapping upload 
     * forms and for handing the file payload of incoming requests
     */
    protected DSpaceRequestFactory requestFactory;

    /** Root Cocoon Bean Factory. */
    protected BeanFactory cocoonBeanFactory;

    /** The root settings. */
    protected Settings settings;

    /** The special servlet settings. */
    protected ServletSettings servletSettings;

    /** The servlet context. */
    protected ServletContext servletContext;

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig config) throws ServletException
    {
    	  	
        this.servletContext = config.getServletContext();
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        // nothing to do
    }

    protected synchronized void configure() {
        if (this.cocoonBeanFactory == null) {
            this.cocoonBeanFactory = WebApplicationContextUtils.getRequiredWebApplicationContext(this.servletContext);
            this.settings = (Settings) this.cocoonBeanFactory.getBean(Settings.ROLE);
            this.servletSettings = new ServletSettings(this.settings);
            String containerEncoding;
            final String encoding = this.settings.getContainerEncoding();
            if (encoding == null) {
                containerEncoding = "ISO-8859-1";
            } else {
                containerEncoding = encoding;
            }

            /*final MultipartConfigurationHelper config = new MultipartConfigurationHelper();
            config.configure(this.settings, getLogger());

            this.requestFactory = new DSpaceRequestFactory(config.isAutosaveUploads(),
                                                     new File(config.getUploadDirectory()),
                                                     config.isAllowOverwrite(),
                                                     config.isSilentlyRename(),
                                                     config.getMaxUploadSize(),
                                                     containerEncoding);*/
        }
    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest req, ServletResponse res , FilterChain filterChain)
    throws IOException, ServletException {
        if (this.cocoonBeanFactory == null) {
            this.configure();
        }
        
        boolean complete = false;
        boolean resumableUploadRequest = false;
    	
    	Context context = null;
        // get the request (wrapped if contains multipart-form data)
        HttpServletRequest request = (HttpServletRequest) req;
        try {
        	context = ContextUtil.obtainContext(request);
        	EPerson user = context.getCurrentUser();
        	if( user != null){
        		String name = user.getFirstName() + " " + user.getLastName();
        		
        		
        		////LOG.debug("doFilter: Looks like there is a valid context object :-)");
        		//LOG.debug("doFilter: name " + name);
        		
        		 HttpServletResponse response = (HttpServletResponse) res;
        		 
        		try {
        			// if this is a resumable file upload then service the request
        			if(request.getParameter("resumableChunkSize") != null){
        				resumableUploadRequest= true;
        				LOG.debug("doFilter: Received a resumable request");
	                	ResumableUpload uploader = ResumableUpload.getInstance();
        				if(request.getMethod().equalsIgnoreCase("post")){
	        				
		                	complete = uploader.doUpload(context, request, response);
        				}else{
        					uploader.doGet(request, response);
        				}
        			}else{
        				//LOG.debug("doFilter: Its NOT a resumable request");
        			}
                	
                } catch (Exception e) {
                    if (getLogger().isErrorEnabled()) {
                        getLogger().error("Problem in Big File Upload filter. Unable to complete request.", e);
                    }

                    RequestUtil.manageException(request, response, null, null,
                                                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                                "Problem in creating the Request",
                                                null, null, e, this.servletSettings, getLogger(), this);
                }

        		
        	}else{
        		LOG.debug("doFilter: User is null");
        	}
		} catch (SQLException e1) {
			throw new ServletException(e1);
		}
       
        try{
            if( resumableUploadRequest ){
            	if(complete ){
            		// need to redirect here back to where the request came from if its complete
            		// complete the context for the present need to sort out the context so that the workflow can continue
            		context.complete();
            	}else{
            		// complete the context for the present need to sort out the context so that the workflow can continue
            		context.complete();
                	return;
                }
            }else{
            	filterChain.doFilter(req, res);
            }

        } catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
           
        }
    }
}
