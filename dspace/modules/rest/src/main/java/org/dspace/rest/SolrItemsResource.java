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
import org.dspace.rest.common.Item;
import org.dspace.rest.exceptions.ContextException;

/**
 * Class which provide all CRUD methods over items.
 * 
 * @author Rostislav Novak (Computing and Information Centre, CTU in Prague)
 * 
 */
// Every DSpace class used without namespace is from package
// org.dspace.rest.common.*. Otherwise namespace is defined.
@Path("/solrItems")
public class SolrItemsResource extends Resource {

	private static final Logger log = Logger.getLogger(SolrItemsResource.class);

	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Item[] getSolrItems(@QueryParam("q") String searchQuery, @QueryParam("limit") @DefaultValue("100") Integer limit, @QueryParam("offset") @DefaultValue("0") Integer offset,
			@QueryParam("userIP") String user_ip, @QueryParam("userAgent") String user_agent, @QueryParam("xforwardedfor") String xforwardedfor, @Context HttpHeaders headers,
			@Context HttpServletRequest request) throws WebApplicationException {

		log.info("Reading items.(offset=" + offset + ",limit=" + limit + ").");
		org.dspace.core.Context context = null;
		List<Item> items = new ArrayList<Item>();

		try {
			context = createContext(getUser(headers));

			DiscoverQuery query = new DiscoverQuery();
			query.setQuery(searchQuery);
			query.setMaxResults(limit);
			query.setStart(offset);
			DiscoverResult discoverResult = SearchUtils.getSearchService().search(context, query);

			List<DSpaceObject> objects = discoverResult.getDspaceObjects();

			for (DSpaceObject obj : objects) {
				Item i = new Item();
				i.setId(obj.getID());
				items.add(i);
			}

			context.complete();
		} catch (SQLException e) {
			processException("Something went wrong while searching for items from database. Message: " + e, context);
		} catch (ContextException e) {
			processException("Something went wrong while searching for items, ContextException. Message: " + e.getMessage(), context);
		} catch (SearchServiceException e) {
			log.error("Something went wrong while searching for items, SearchServiceException. ", e);
			processException("Something went wrong while searching for items, SearchServiceException. Message: " + e.getMessage(), context);
		} finally {
			processFinally(context);
		}

		log.trace("Items were successfully read.");
		
		return items.toArray(new Item[0]);
	}

	
}
