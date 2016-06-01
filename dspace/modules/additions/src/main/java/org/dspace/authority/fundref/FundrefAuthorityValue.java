/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.fundref;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.dspace.authority.AuthorityValue;


public class FundrefAuthorityValue extends AuthorityValue
{
	private static Logger log = Logger.getLogger(FundrefAuthorityValue.class);

	public FundrefAuthorityValue() {
		super();
	}

	public FundrefAuthorityValue(SolrDocument document) {
		super(document);
	}

	public static FundrefAuthorityValue create(String id, String name) {
		FundrefAuthorityValue value = new FundrefAuthorityValue();
		value.setId(id);
		value.setValue(name);
		value.updateLastModifiedDate();
		value.setCreationDate(new Date());
		return value;
	}

	@Override
	public String getAuthorityType() {
		return "fundref";
	}

	@Override
	public String generateString() {
		// AuthorityValueGenerator.GENERATE + ... will cause the ID to be replaced by a UUID
		return this.getId();
	}

	@Override
	public AuthorityValue newInstance(String info) {
		AuthorityValue authorityValue = null;
		if (StringUtils.isNotBlank(info)) {
			FundrefAuthority fundref = FundrefAuthority.getFundrefAuthority();
			authorityValue = fundref.queryAuthorityID(info);
		} else {
			authorityValue = new FundrefAuthorityValue();
		}
		return authorityValue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		FundrefAuthorityValue that = (FundrefAuthorityValue) o;

		if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return getId() != null ? getId().hashCode() : 0;
	}
}
