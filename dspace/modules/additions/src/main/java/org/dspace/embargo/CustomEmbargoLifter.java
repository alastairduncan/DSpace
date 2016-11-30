package org.dspace.embargo;

import java.sql.SQLException;
import java.io.IOException;
import java.util.Date;
import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.Bundle;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Collection;
import org.dspace.eperson.Group;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Context;
import org.dspace.core.Constants;
import org.dspace.license.CreativeCommons;


/**
 * Custom plugin implementation of the embargo lifting function. Updates policies to have the current date as start date.
 *
 * @author Philip Tootill
 */
public class CustomEmbargoLifter implements EmbargoLifter
{
	private static Logger log = Logger.getLogger(CustomEmbargoLifter.class);
	
	
    public CustomEmbargoLifter()
    {
        super();
    }

    /**
     * Enforce lifting of embargo by setting the policy date to the current date for all relevant policies.
     *
     * @param context the DSpace context
     * @param item    the item to lift embargo on
     */
    @Override
    public void liftEmbargo(Context context, Item item)
            throws SQLException, AuthorizeException, IOException
    {
        log.debug("Running liftEmbargo for " + item.getName());
    	
    	for (Bundle bn : item.getBundles())
        {
            // Skip the LICENSE and METADATA bundles, they stay world-readable
            String bnn = bn.getName();
            if (!(bnn.equals(Constants.LICENSE_BUNDLE_NAME) || bnn.equals(Constants.METADATA_BUNDLE_NAME) || bnn.equals(CreativeCommons.CC_BUNDLE_NAME)))
            {
                resetPolicies(context, bn, item.getOwningCollection());
                for (Bitstream bs : bn.getBitstreams())
                {
                	resetPolicies(context, bs, item.getOwningCollection());
                }
            }
        }
    }
    
    protected void resetPolicies(Context context, DSpaceObject dso, Collection owningCollection) throws SQLException, AuthorizeException {
        Group[] authorizedGroups = AuthorizeManager.getAuthorizedGroups(context, owningCollection, Constants.DEFAULT_ITEM_READ);
        Group anonymous = null;
        Date date = new Date();

        // look for anonymous
        for(Group g : authorizedGroups){
            if(g.getID()==Group.ANONYMOUS_ID){
                anonymous = g;
            }
        }
        
        if(anonymous == null) {
            // add policies for all the groups
            for(Group g : authorizedGroups){
            	log.debug("Attempting to lift embargo (group version) for " + dso.getName());
                ResourcePolicy rp = AuthorizeManager.createOrModifyPolicy(null, context, null, g.getID(), null, date, Constants.READ, null, dso);
                if(rp!=null) {
                	rp.update();
                }
            }
        } else {
            // add policy just for anonymous
        	log.debug("Attempting to lift embargo for " + dso.getName());
            ResourcePolicy rp = AuthorizeManager.createOrModifyPolicy(null, context, null, 0, null, date, Constants.READ, null, dso);
            if(rp!=null) {
                rp.update();
            }
        }
	}
}
