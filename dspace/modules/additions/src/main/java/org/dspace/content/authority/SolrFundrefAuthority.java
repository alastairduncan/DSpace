/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.dspace.authority.AuthoritySource;
import org.dspace.utils.DSpace;


public class SolrFundrefAuthority extends BaseSolrAuthority
{
    public SolrFundrefAuthority() {
        source = new DSpace().getServiceManager().getServiceByName("FundrefAuthoritySource", AuthoritySource.class);
    }
}
