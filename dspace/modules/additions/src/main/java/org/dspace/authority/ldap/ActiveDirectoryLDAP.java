/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.ldap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

public class ActiveDirectoryLDAP {
    // private LdapContext m_ctx = null;
    private static Logger LOG = Logger.getLogger(ActiveDirectoryLDAP.class);
    private String host = null;
    private int port = 389;
    private String searchContext = "";
    private String surnameField = "sn";
    private String givennameField = "givenname";
    private String email = "mail";

    public ActiveDirectoryLDAP() {

    }

    // really would like to do this in the constructor but this fails and states
    // that the default.cfg can't be found. Its something to do with spring starting up the bean,
    // that uses this class, before the configuration has been configured.
    
    private void setup() {

	searchContext = ConfigurationManager.getProperty("authentication-ldap", "search_context");
	String provider_url = ConfigurationManager.getProperty("authentication-ldap", "provider_url");
	surnameField = ConfigurationManager.getProperty("authentication-ldap", "surname_field");
	givennameField = ConfigurationManager.getProperty("authentication-ldap", "givenname_field");
	email = ConfigurationManager.getProperty("authentication-ldap", "email_field");
	LOG.info("setup provider_url: " + provider_url);
	if (host == null) {
	    try {
		URI provider = new URI(provider_url);
		host = provider.getHost();
		port = provider.getPort();
		LOG.debug("setup: host: " + host);
		LOG.debug("setup: port: " + port);

	    } catch (URISyntaxException e) {
		LOG.error("setup: ", e);
	    }
	}
    }

    public List<LDAPAuthorityValue> searchForUsers(String term) {
	setup();

	term = term.trim();
	String firstName = "";
	String lastName = "";
	// assume that the format is surname, firstname or surname, initial
	if (term.contains(",")) {
	    
	    String[] names = term.split(",");
	    if(names.length >= 2){
	       	lastName = names[0].trim();
	    	firstName = names[1].trim();
	    }else{
	    	lastName = names[1];
	    }
	   
	} else if (term.contains(" ")) {
	    // assume that the format is surname firstname
		
		 String[] names = term.split(" ");
		    if(names.length >= 2){
		    	lastName = names[0].trim();
		    	firstName = names[1].trim();
		    }else{
		    	lastName = names[0];
		    }

	} else {
	    // just the surname
	    lastName = term;
	}
	
	String filter = "(sn=" + lastName + ")";
	if (!firstName.equals("")) {
	    filter = "(&" + filter + "(givenname=" + firstName + "))";
	}

	List<LDAPAuthorityValue> results = new ArrayList<LDAPAuthorityValue>();

	LDAPConnection conn = new LDAPConnection();
	SearchResult result = null;
	try {
	    conn.connect(host, port);

	    String[] attributes = null;
	    result = conn.search(searchContext, SearchScope.SUB, filter, attributes);
	    for (SearchResultEntry entry : result.getSearchEntries()) {
			LDAPAuthorityValue person = populatePerson(entry);
			if (person != null) {
			    results.add(person);
			}
	    }

	} catch (LDAPSearchException e) {
	    LOG.error("searchForUsers: ", e);
	} catch (LDAPException e) {
	    LOG.error("searchForUsers: ", e);
	}

	return results;
    }

    private LDAPAuthorityValue populatePerson(SearchResultEntry entry) {
	LDAPAuthorityValue person = null;
	// only populate if there is a FedID
	Attribute sAMAccountName = entry.getAttribute("sAMAccountName");
	if (sAMAccountName != null && sAMAccountName.getValue() != null) {
	    person = new LDAPAuthorityValue();
	    person.setId(sAMAccountName.getValue());

	    Attribute lastName = entry.getAttribute(surnameField);
	    person.setLastName(lastName.getValue());
	    Attribute firstName = entry.getAttribute(givennameField);

	    String name = "";
	    if (firstName != null && firstName.getValue() != null) {
		person.setFirstName(firstName.getValue());

	    } else if (entry.getAttribute("initials") != null && entry.getAttribute("initials").getValue() != null) {
		person.setFirstName(entry.getAttribute("initials").getValue());
	    }
	    person.setName(person.getLastName() + ", " + person.getFirstName());

	    Attribute title = entry.getAttribute("title");
	    if (title != null && title.getValue() != null) {
		person.setTitle(title.getValue());
	    }

	    Attribute stfcStatus = entry.getAttribute("stfc-status");
	    if (stfcStatus != null) {
		if (stfcStatus.getValue().contains("STAFF")) {
		    Attribute mail = entry.getAttribute(email);
		    if (mail != null && mail.getValue() != null) {
			person.addEmail(mail.getValue());
		    }
		    Attribute department = entry.getAttribute("department");
		    if (department != null && department.getValue() != null) {
			person.setDepartment(department.getValue());
		    }
		}
	    }
	    Attribute company = entry.getAttribute("company");
	    if (company != null && company.getValue() != null) {
		person.setInstitution(company.getValue());
	    }

	    Attribute pid = entry.getAttribute("stfc-PID");
	    if (pid != null && pid.getValue() != null) {
		person.setPid(pid.getValue());
	    }
	    // some of the contractors entries are kept
	    // hanging
	    // around and are marked as deleted
	    // in the description field????? these should be
	    // marked as inactive
	    Attribute stfcpid = entry.getAttribute("description");
	    if (stfcpid != null && stfcpid.getValue() != null && stfcpid.getValue().contains("DELETED")) {
		person.setIsActive(false);
	    } else {
		person.setIsActive(true);
	    }
	}
	return person;
    }

    protected LDAPAuthorityValue searchForUser(String commonName) {
	LOG.info("searchForUser");
	LDAPAuthorityValue person = null;
	String filter = "(CN=" + commonName.trim() + ")";
	
	setup();
	
	LDAPConnection conn = new LDAPConnection();
	SearchResultEntry result = null;
	try {
	    conn.connect(host, port);

	    String[] attributes = null;
	    result = conn.searchForEntry(searchContext, SearchScope.SUB, filter, attributes);

	    if (result != null) {
		person = populatePerson(result);
	    }

	} catch (LDAPSearchException e) {
	    LOG.error("searchForUser: ", e);
	} catch (LDAPException e) {
	    LOG.error("searchForUser: ", e);
	}
	return person;
    }

}
