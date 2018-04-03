/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.xml;
import org.dspace.authority.util.XMLUtils;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SearchResultsConverter extends Converter {

    private static Logger log = Logger.getLogger(SearchResultsConverter.class);

    public List<String> convert(Document xml) {
        List<String> result = new ArrayList<String>();

        if (XMLErrors.check(xml)) {
            try {
                Iterator<Node> iterator = XMLUtils.getNodeListIterator(xml, "/search/result");
                while (iterator.hasNext()) {
                    String orcid = getOrcidId(iterator.next());
                    result.add(orcid);
                }
            } catch (XPathExpressionException e) {
                log.error("Error in xpath syntax", e);
            }
        } else {
            processError(xml);
        }

        return result;
    }

    protected void processError(Document xml) {
        String errorMessage = XMLErrors.getErrorMessage(xml);
        log.error("The orcid-message reports an error: " + errorMessage);
    }

    protected String getOrcidId(Node node) {
        try {
            return XMLUtils.getTextContent(node, "orcid-identifier/path");
        } catch (XPathExpressionException e) {
            log.debug("Error retreiving an ORCID ID from search results", e);
            return null;
        }
    }
}
