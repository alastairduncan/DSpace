/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import org.dspace.authority.AuthorityValue;

import java.util.List;

public interface AuthoritySource
{
    public List<AuthorityValue> queryAuthorities(String text, int max);

    public AuthorityValue queryAuthorityID(String id);
}
