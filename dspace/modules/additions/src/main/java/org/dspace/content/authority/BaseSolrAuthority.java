/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.dspace.authority.AuthoritySearchService;
import org.dspace.authority.AuthoritySource;
import org.dspace.authority.AuthorityValue;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.utils.DSpace;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Alan Kyffin (alan dot kyffin at stfc dot ac dot uk)
 */
public class BaseSolrAuthority implements AuthorityVariantsSupport, ChoiceAuthority
{
    private static final Logger log = Logger.getLogger(BaseSolrAuthority.class);
    protected AuthoritySource source = null;

    private Choices getMatches(String field, String text, int collection, int start, int limit, String locale, boolean bestMatch) {
        if (limit == 0)
            limit = 10;

        Choices result;
        try {
            List<Choice> choices = getExternalResults(text, limit);

            int confidence;
            if (choices.size() == 0)
                confidence = Choices.CF_NOTFOUND;
            else if (choices.size() == 1)
                confidence = Choices.CF_UNCERTAIN;
            else
                confidence = Choices.CF_AMBIGUOUS;

            result = new Choices(choices.toArray(new Choice[choices.size()]), start, start + choices.size(), confidence, false);
        } catch (Exception e) {
            log.error("Error while retrieving authority values {field: " + field + ", prefix:" + text + "}", e);
            result = new Choices(true);
        }

        return result;
    }

    private List<Choice> getExternalResults(String text, int max) {
        List<Choice> choices = new ArrayList<Choice>();

        if (source == null) {
            log.warn("external source for authority not configured");
            return choices;
        }

        List<AuthorityValue> values = source.queryAuthorities(text, max);

        for (AuthorityValue val : values) {
            choices.add(new Choice(val.generateString(), val.getValue(), val.getValue(), val.choiceSelectMap()));
        }

        return choices;
    }

    @Override
    public Choices getMatches(String field, String text, int collection, int start, int limit, String locale) {
        return getMatches(field, text, collection, start, limit, locale, true);
    }

    @Override
    public Choices getBestMatch(String field, String text, int collection, String locale) {
        Choices matches = getMatches(field, text, collection, 0, 1, locale, false);
        if (matches.values.length !=0 && !matches.values[0].value.equalsIgnoreCase(text)) {
            matches = new Choices(false);
        }
        return matches;
    }

    @Override
    public String getLabel(String field, String key, String locale) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("requesting label for key " + key + " using locale " + locale);
            }
            SolrQuery queryArgs = new SolrQuery();
            queryArgs.setQuery("id:" + ClientUtils.escapeQueryChars(key));
            queryArgs.setRows(1);
            QueryResponse searchResponse = getSearchService().search(queryArgs);
            SolrDocumentList docs = searchResponse.getResults();
            if (docs.getNumFound() == 1) {
                String label = null;
                try {
                    label = (String) docs.get(0).getFieldValue("value_" + locale);
                } catch (Exception e) {
                    //ok to fail here
                }
                if (label != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("returning label " + label + " for key " + key + " using locale " + locale + " and fieldvalue " + "value_" + locale);
                    }
                    return label;
                }
                try {
                    label = (String) docs.get(0).getFieldValue("value");
                } catch (Exception e) {
                    log.error("couldn't get field value for key " + key,e);
                }
                if (label != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("returning label " + label + " for key " + key + " using locale " + locale + " and fieldvalue " + "value");
                    }
                    return label;
                }
                try {
                    label = (String) docs.get(0).getFieldValue("value_en");
                } catch (Exception e) {
                    log.error("couldn't get field value for key " + key,e);
                }
                if (label != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("returning label " + label + " for key " + key + " using locale " + locale + " and fieldvalue " + "value_en");
                    }
                    return label;
                }
            }
        } catch (Exception e) {
            log.error("error occurred while trying to get label for key " + key,e);
        }

        return key;
    }


    private static AuthoritySearchService getSearchService() {
        DSpace dspace = new DSpace();

        org.dspace.kernel.ServiceManager manager = dspace.getServiceManager();

        return manager.getServiceByName(AuthoritySearchService.class.getName(), AuthoritySearchService.class);
    }

    @Override
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
