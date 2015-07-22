/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.ldap;

import java.util.ArrayList;
import java.util.List;


import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.utils.DSpace;

public class LDAPAuthority extends LDAPSource {

	private static LDAPAuthority auth = null;
	private static Logger LOG = Logger.getLogger(LDAPAuthority.class);
	
	public LDAPAuthority(){
		
	}

	synchronized public static LDAPAuthority getLDAPAuthority() {
	
        	LOG.info("LDAPAuthority");
        
        	if (auth == null) {
        		auth = new DSpace().getServiceManager().getServiceByName(
        				"LDAPSource", LDAPAuthority.class);
        	}
        	return auth;
	}

	@Override
	public LDAPAuthorityValue queryAuthorityID(String info) {
		LOG.info("queryAuthorityID: " + info);
		LDAPAuthorityValue authValue = null;
		
		authValue = adLDAP.searchForUser(info);
		LOG.info("queryAuthorityID: found auth value"
				+ authValue.getValue());
		
		return authValue;
	}

	@Override
	public List<AuthorityValue> queryAuthorities(String text, int max) {
		List<AuthorityValue> authorities = null;
		
		authorities = new ArrayList<AuthorityValue>();
		for (LDAPAuthorityValue value : adLDAP.searchForUsers(text)) {
			authorities.add(value);
		}
		
		return authorities;
	}

}