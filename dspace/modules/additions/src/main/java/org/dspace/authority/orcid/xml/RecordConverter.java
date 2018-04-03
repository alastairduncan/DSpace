/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.xml;

import org.dspace.authority.orcid.model.Bio;
import org.dspace.authority.orcid.model.BioExternalIdentifier;
import org.dspace.authority.orcid.model.BioName;
import org.dspace.authority.orcid.model.BioResearcherUrl;
import org.dspace.authority.util.XMLUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.util.Iterator;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class RecordConverter extends Converter {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(RecordConverter.class);

    /**
     * orcid-message XPATHs
     */

    // record
    protected String RECORD = "/record";

    protected String ORCID = RECORD + "/orcid-identifier/path";
    protected String PERSON = RECORD + "/person";
    protected String ACTIVITIES = RECORD + "/activities-summary";

    protected String PERSON_NAME = PERSON + "/name";
    protected String OTHER_NAMES = PERSON + "/other-names";
    protected String PERSON_BIOGRAPHY = PERSON + "/biography";
    protected String RESEARCHER_URLS = PERSON + "/researcher-urls";
    protected String EMAILS = PERSON + "/emails";
    protected String ADDRESSES = PERSON + "/addresses";
    protected String KEYWORDS = PERSON + "/keywords";
    protected String EXTERNAL_IDENTIFIERS = PERSON + "/external-identifiers";

    protected String GIVEN_NAMES = PERSON_NAME + "/given-names";
    protected String FAMILY_NAME = PERSON_NAME + "/family-name";
    protected String CREDIT_NAME = PERSON_NAME + "/credit-name";
    protected String BIOGRAPHY = PERSON_BIOGRAPHY + "/content";
    protected String OTHER_NAME = OTHER_NAMES + "/other-name/content";

    protected String RESEARCHER_URL = RESEARCHER_URLS + "/researcher-url";
    protected String URL_NAME = RESEARCHER_URL + "/url-name";
    protected String URL = RESEARCHER_URL + "/url";

    protected String EMAIL = EMAILS + "/email[@primary='true']/email";
    protected String KEYWORD = KEYWORDS + "/keyword/content";
    protected String COUNTRY = ADDRESSES + "/address/country";

    protected String EXTERNAL_IDENTIFIER = EXTERNAL_IDENTIFIERS + "/external-identifier";
    protected String EXTERNAL_ID_TYPE = EXTERNAL_IDENTIFIERS + "/external-id-type";
    protected String EXTERNAL_ID_VALUE = EXTERNAL_IDENTIFIERS + "/external-id-value";
    protected String EXTERNAL_ID_URL = EXTERNAL_IDENTIFIERS + "/external-id-url";

    protected String EMPLOYMENTS = ACTIVITIES + "/employments/employment-summary";
    protected String AFFILIATION = EMPLOYMENTS + "/organization/name";

    /**
     * Regex
     */

    protected String ORCID_NOT_FOUND = "ORCID [\\d-]* not found";


    public Bio convert(Document xml) {
        Bio result = null;

        if (XMLErrors.check(xml)) {

            try {
                Iterator<Node> iterator = XMLUtils.getNodeListIterator(xml, RECORD);
                if (iterator.hasNext()) {
                    result = convertBio(iterator.next());
                }
            } catch (XPathExpressionException e) {
                log.error("Error in xpath syntax", e);
            }
        } else {
            processError(xml);
        }

        return result;
    }

    private Bio convertBio(Node node) {
        Bio bio = new Bio();

        setOrcid(node,bio);
        setPersonalDetails(node, bio);
        setContactDetails(node, bio);
        setKeywords(node, bio);
        setExternalIdentifiers(node, bio);
        setResearcherUrls(node, bio);
        setBiography(node, bio);
        setAffiliations(node, bio);

        return bio;
    }

    protected void processError(Document xml)  {
        String errorMessage = XMLErrors.getErrorMessage(xml);

        if(errorMessage.matches(ORCID_NOT_FOUND))
        {
            // do something?
        }

        log.error("The orcid-message reports an error: " + errorMessage);
    }


    protected void setOrcid(Node node, Bio bio) {
        try {
            String orcid = XMLUtils.getTextContent(node, ORCID);
            bio.setOrcid(orcid);
        } catch (XPathExpressionException e) {
            log.debug("Error in finding the biography in bio xml.", e);
        }
    }

    protected void setBiography(Node xml, Bio bio) {
        try {
            String biography = XMLUtils.getTextContent(xml, BIOGRAPHY);
            bio.setBiography(biography);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the biography in bio xml.", e);
        }
    }

    protected void setResearcherUrls(Node xml, Bio bio) {
        try {
            NodeList researcher_urls = XMLUtils.getNodeList(xml, RESEARCHER_URL);
            if (researcher_urls != null) {
                for (int i = 0; i < researcher_urls.getLength(); i++) {
                    Node researcher_url = researcher_urls.item(i);
                    if (researcher_url.getNodeType() != Node.TEXT_NODE) {
                        String url_name = XMLUtils.getTextContent(researcher_url, URL_NAME);
                        String url = XMLUtils.getTextContent(researcher_url, URL);
                        BioResearcherUrl researcherUrl = new BioResearcherUrl(url_name, url);
                        bio.addResearcherUrl(researcherUrl);
                    }
                }
            }
        } catch (XPathExpressionException e) {
            log.error("Error in finding the researcher url in bio xml.", e);
        }
    }

    protected void setExternalIdentifiers(Node xml, Bio bio) {
        try {

            Iterator<Node> iterator = XMLUtils.getNodeListIterator(xml, EXTERNAL_IDENTIFIER);
            while (iterator.hasNext()) {
                Node external_identifier = iterator.next();
                String id_orcid = null; // No equivelent of 'external-id-orcid' in newer APIs
                String id_common_name = XMLUtils.getTextContent(external_identifier, EXTERNAL_ID_TYPE);
                String id_reference = XMLUtils.getTextContent(external_identifier, EXTERNAL_ID_VALUE);
                String id_url = XMLUtils.getTextContent(external_identifier, EXTERNAL_ID_URL);
                BioExternalIdentifier externalIdentifier = new BioExternalIdentifier(id_orcid, id_common_name, id_reference, id_url);
                bio.addExternalIdentifier(externalIdentifier);
            }

        } catch (XPathExpressionException e) {
            log.error("Error in finding the external identifier in bio xml.", e);
        }
    }

    protected void setKeywords(Node xml, Bio bio) {
        try {
            NodeList keywords = XMLUtils.getNodeList(xml, KEYWORD);
            if (keywords != null) {
                for (int i = 0; i < keywords.getLength(); i++) {
                    String keyword = keywords.item(i).getTextContent();
                    String[] split = keyword.split(",");
                    for (String k : split) {
                        bio.addKeyword(k.trim());
                    }
                }
            }
        } catch (XPathExpressionException e) {
            log.error("Error in finding the keywords in bio xml.", e);
        }
    }

    protected void setContactDetails(Node xml, Bio bio) {
        try {
            String country = XMLUtils.getTextContent(xml, COUNTRY);
            bio.setCountry(country);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the country in bio xml.", e);
        }

        try {
            String email = XMLUtils.getTextContent(xml, EMAIL);
            bio.setEmail(email);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the email in bio xml.", e);
        }
    }

    protected void setPersonalDetails(Node xml, Bio bio) {
        BioName name = bio.getName();

        try {
            String givenNames = XMLUtils.getTextContent(xml, GIVEN_NAMES);
            name.setGivenNames(givenNames);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the given names in bio xml.", e);
        }

        try {
            String familyName = XMLUtils.getTextContent(xml, FAMILY_NAME);
            name.setFamilyName(familyName);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the family name in bio xml.", e);
        }

        try {
            String creditName = XMLUtils.getTextContent(xml, CREDIT_NAME);
            name.setCreditName(creditName);
        } catch (XPathExpressionException e) {
            log.error("Error in finding the credit name in bio xml.", e);
        }

        try {

            Iterator<Node> iterator = XMLUtils.getNodeListIterator(xml, OTHER_NAME);
            while (iterator.hasNext()) {
                Node otherName = iterator.next();
                String textContent = otherName.getTextContent();
                name.getOtherNames().add(textContent.trim());
            }

        } catch (XPathExpressionException e) {
            log.error("Error in finding the other names in bio xml.", e);
        }
    }

    protected void setAffiliations(Node xml, Bio bio) {
        try {
            Iterator<Node> iterator = XMLUtils.getNodeListIterator(xml, AFFILIATION);
            while (iterator.hasNext()) {
                String affiliation = iterator.next().getTextContent();
                bio.addAffiliation(affiliation);
            }
        } catch (XPathExpressionException e) {
            log.error("Error in finding the affiliations in bio xml.", e);
        }
    }
}
