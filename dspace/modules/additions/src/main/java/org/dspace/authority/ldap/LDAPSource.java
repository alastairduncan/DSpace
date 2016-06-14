/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.ldap;

import org.dspace.authority.AuthoritySource;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.ldap.LDAPAuthority;

import java.util.List;


public abstract class LDAPSource implements AuthoritySource
{
    protected ActiveDirectoryLDAP adLDAP;

    public LDAPSource() {
    		this.adLDAP = new ActiveDirectoryLDAP();
    }

   
    public abstract List<AuthorityValue> queryAuthorities(String text, int max);

    public abstract AuthorityValue queryAuthorityID(String id);
}
