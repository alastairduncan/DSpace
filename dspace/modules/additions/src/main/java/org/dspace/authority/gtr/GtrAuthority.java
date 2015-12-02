/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.gtr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.rest.ProxyRestSource;
import org.dspace.utils.DSpace;




public class GtrAuthority extends ProxyRestSource {
    private static Logger LOG = Logger.getLogger(GtrAuthority.class);

    private static GtrAuthority gtrAuthority;

    public static GtrAuthority getGtrAuthority() {
	if (gtrAuthority == null) {
	    gtrAuthority = new DSpace().getServiceManager().getServiceByName("GTRSource", GtrAuthority.class);
	}
	return gtrAuthority;
    }

    public GtrAuthority(String url) {
	super(url);
    }

    public AuthorityValue getProject(String id) {
	InputStreamReader isr = new InputStreamReader(restConnector.getIS("gtr/api/projects?q=" + id));
	BufferedReader reader = new BufferedReader(isr);

	String line;
	String doc = "";
	try {
	    line = reader.readLine();

	    while (line != null) {
		doc += line;
		line = reader.readLine();
	    }

	} catch (IOException e) {
	    LOG.error("getProject: ", e);
	}
	// gateway to research does not have an api to get a project based on an RCUK id 
	// can only do this for an internal id!
	// as search has to be done first - only one result should be returned and this will be the project that is required
	return GtrAuthorityValue.getProjectValueFromProjects(doc);
    }

    @Override
    public List<AuthorityValue> queryAuthorities(String text, int max) {
	List<AuthorityValue> values = new ArrayList<AuthorityValue>();
	values.add(queryAuthorityID(text));
	return values;
    }

    @Override
    public AuthorityValue queryAuthorityID(String id) {
	LOG.debug("queryAuthorityID: " + id);
	return getProject(id);
    }
}
