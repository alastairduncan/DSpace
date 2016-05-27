package org.dspace.content.authority;

import org.dspace.authority.SolrAuthorityInterface;
import org.dspace.utils.DSpace;

public class SolrFundrefAuthority extends SolrAuthority
{
    public SolrFundrefAuthority() {
        this.source = new DSpace().getServiceManager().getServiceByName("FundrefAuthoritySource", SolrAuthorityInterface.class);
    }
}
