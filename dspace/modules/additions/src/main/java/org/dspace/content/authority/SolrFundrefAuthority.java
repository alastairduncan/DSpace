package org.dspace.content.authority;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;

import org.dspace.authority.SolrAuthorityInterface;
import org.dspace.content.authority.AuthorityVariantsSupport;
import org.dspace.utils.DSpace;

public class SolrFundrefAuthority extends SolrAuthority implements AuthorityVariantsSupport
{
    private static final Logger log = Logger.getLogger(SolrFundrefAuthority.class);

    public SolrFundrefAuthority() {
        this.source = new DSpace().getServiceManager().getServiceByName("FundrefAuthoritySource", SolrAuthorityInterface.class);
    }

    public List<String> getVariants(String key, String locale) {
	List<String> list = new ArrayList<String>();
        try {
            if (log.isDebugEnabled()) {
                log.debug("requesting variants for key " + key + " using locale " + locale);
            }
            SolrQuery queryArgs = new SolrQuery();
            queryArgs.setQuery("id:" + ClientUtils.escapeQueryChars(key));
            queryArgs.setRows(1);
            QueryResponse searchResponse = getSearchService().search(queryArgs);
            SolrDocumentList docs = searchResponse.getResults();
            if (docs.getNumFound() == 1) {
                try {
                    for (Object o : docs.get(0).getFieldValues("all_labels_" + locale)) {
                        list.add((String) o);
                    }
                    return list;
                } catch (Exception e) {
                    //ok to fail here
                }
                try {
                    for (Object o : docs.get(0).getFieldValues("all_labels")) {
                        list.add((String) o);
                    }
                    return list;
                } catch (Exception e) {
                    log.error("couldn't get field value for key " + key,e);
                }
                try {
                    for (Object o : docs.get(0).getFieldValues("all_labels_en")) {
                        list.add((String) o);
                    }
                    return list;
                } catch (Exception e) {
                    log.error("couldn't get field value for key " + key,e);
                }
            }
        } catch (Exception e) {
            log.error("error occurred while trying to get variants for key " + key,e);
        }

        return list;
    }
}
