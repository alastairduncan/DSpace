/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.rest.exceptions.ContextException;

/**
 * Extention of ItemsResorce to add a further method to the api in order to search the discovery system
 * @author ad43
 *
 */
// Every DSpace class used without namespace is from package org.dspace.rest.common.*. Otherwise namespace is defined.
public class SolrItemsResource extends ItemsResource
{

	private static final Logger log = Logger.getLogger(SolrItemsResource.class);
    
    
    /**
     * It returns an array of items in DSpace. You can define how many items in
     * list will be and from which index will start. Items in list are sorted by
     * handle, not by id.
     * 
     * @param limit
     *            How many items in array will be. Default value is 100.
     * @param offset
     *            On which index will array start. Default value is 0.
     * @param headers
     *            If you want to access to item under logged user into context.
     *            In headers must be set header "rest-dspace-token" with passed
     *            token from login method.
     * @return Return array of items, on which has logged user into context
     *         permission.
     * @throws WebApplicationException
     *             It can be thrown by SQLException, when was problem with
     *             reading items from database or ContextException, when was
     *             problem with creating context of DSpace.
     */
    @GET
    @Path("/solrItems")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Integer[] getSolrItems(@QueryParam("q") String searchQuery, @QueryParam("limit") @DefaultValue("100") Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("userIP") String user_ip,
            @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor,
            @Context HttpHeaders headers, @Context HttpServletRequest request) throws WebApplicationException
    {

        log.info("Reading items.(offset=" + offset + ",limit=" + limit + ").");
        org.dspace.core.Context context = null;
        List<Integer> itemIds = new ArrayList<Integer>();
        
        
        try
        {
            context = createContext(getUser(headers));

            DiscoverQuery query = new DiscoverQuery();
			query.setQuery(searchQuery);
			query.setMaxResults(limit);
			query.setStart(offset);
			DiscoverResult discoverResult = SearchUtils.getSearchService().search(context, query);    	
			
			List<DSpaceObject> objects = discoverResult.getDspaceObjects();
			
			for(DSpaceObject obj:objects ){
				
				itemIds.add(obj.getID());
			}
			
            context.complete();
        }
        catch (SQLException e)
        {
            processException("Something went wrong while searching for items from database. Message: " + e, context);
        }
        catch (ContextException e)
        {
            processException("Something went wrong while searching for items, ContextException. Message: " + e.getMessage(), context);
        } catch (SearchServiceException e) {
        	 processException("Something went wrong while searching for items, SearchServiceException. Message: " + e.getMessage(), context);
		}
        finally
        {
            processFinally(context);
        }

        log.trace("Items were successfully read.");
        return itemIds.toArray(new Integer[0]);
    }

    
}
