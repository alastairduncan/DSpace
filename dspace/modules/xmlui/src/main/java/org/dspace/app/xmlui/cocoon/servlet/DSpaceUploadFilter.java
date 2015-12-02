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

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.ServletRequest;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
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
public class DSpaceUploadFilter extends AbstractLogEnabled implements Filter {

	private Logger LOG = Logger.getLogger(DSpaceUploadFilter.class);
	/**
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	public void init(FilterConfig config) throws ServletException {
	}

	/**
	 * @see javax.servlet.Filter#destroy()
	 */
	public void destroy() {
		// nothing to do
	}

	

	/**
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
		

		boolean resumableUploadRequest = false;

		Context context = null;
		// get the request (wrapped if contains multipart-form data)
		HttpServletRequest request = (HttpServletRequest) req;
		
		HttpSession session = request.getSession();
		long sessionCreate = session.getCreationTime();
		long lastAccessed = session.getLastAccessedTime();
		//LOG.debug("doFilter: session creation time: " + sessionCreate);
		//LOG.debug("doFilter: last accessed: " + lastAccessed);
		//LOG.debug("doFilter: time elapsed " + (lastAccessed - sessionCreate));
		//LOG.debug("doFilter: maxInactive time " + session.getMaxInactiveInterval());
		//LOG.debug("doFilter: is new " + session.isNew());
		boolean complete = false;
		HttpServletResponse response = (HttpServletResponse) res;

		try {
			// if this is a resumable file upload then service the request
			if (request.getParameter("resumableChunkSize") != null) {
				
				resumableUploadRequest = true;
				LOG.debug("doFilter: Received a resumable request");
				ResumableUpload uploader = ResumableUpload.getInstance();
				if (request.getMethod().equalsIgnoreCase("post")) {
					context = ContextUtil.obtainContext(request);
					complete = uploader.doUpload(context, request, response);
					context.complete();
					//once the upload is complete the javascript will continue by clicking a hidden button.
					return;					
				} else {
					uploader.doGet(request, response);
					return;
				}
			} else {
				// LOG.debug("doFilter: Its NOT a resumable request");
				filterChain.doFilter(req, res);
			}
		} catch (Exception e) {
			if (getLogger().isErrorEnabled()) {
				getLogger().error("Problem in Big File Upload filter. Unable to complete request.", e);
			}

			//RequestUtil.manageException(request, response, null, null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Problem in creating the Request", null, null, e, this.servletSettings,
				//	getLogger(), this);
		}
	}
}
