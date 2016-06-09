/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.fundref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authority.AuthorityValue;


public class FundrefAuthorityValue extends AuthorityValue
{
	private static Logger log = Logger.getLogger(FundrefAuthorityValue.class);
	private Map<String, List<String>> otherMetadata = new HashMap<String, List<String>>();

	public FundrefAuthorityValue() {
		super();
	}

	public FundrefAuthorityValue(SolrDocument document) {
		super(document);
	}

	public Map<String, List<String>> getOtherMetadata() {
		return otherMetadata;
	}

	public void addOtherMetadata(String label, String data) {
		List<String> strings = otherMetadata.get(label);
		if (strings == null) {
			strings = new ArrayList<String>();
		}
		strings.add(data);
		otherMetadata.put(label, strings);
	}

	@Override
	public SolrInputDocument getSolrInputDocument() {
		FundrefAuthorityValue value = (FundrefAuthorityValue)FundrefAuthority.getFundrefAuthority().queryAuthorityID(getId());
		setId(value.getId());
		setValue(value.getValue());
		otherMetadata = value.getOtherMetadata();

		SolrInputDocument doc = super.getSolrInputDocument();

		for (String t : otherMetadata.keySet()) {
			List<String> data = otherMetadata.get(t);
			for (String data_entry : data) {
				doc.addField("label_" + t, data_entry);
			}
		}
		return doc;
	}

	@Override
	public void setValues(SolrDocument document) {
		super.setValues(document);

		otherMetadata = new HashMap<String, List<String>>();
		for (String fieldName : document.getFieldNames()) {
			String labelPrefix = "label_";
			if (fieldName.startsWith(labelPrefix)) {
				String label = fieldName.substring(labelPrefix.length());
				List<String> list = new ArrayList<String>();
				Collection<Object> fieldValues = document.getFieldValues(fieldName);
				for (Object o : fieldValues) {
					list.add(String.valueOf(o));
				}
				otherMetadata.put(label, list);
			}
		}
	}

	public static FundrefAuthorityValue create(String id, String name) {
		FundrefAuthorityValue value = new FundrefAuthorityValue();
		value.setId(id);
		value.setValue(name);
		value.updateLastModifiedDate();
		value.setCreationDate(new Date());
		return value;
	}

	public static FundrefAuthorityValue create(String id, String name, String shortName) {
		FundrefAuthorityValue value = create(id, name);

		if (shortName != null)
			value.addOtherMetadata("shortName", shortName);

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

	@Override
	public boolean hasTheSameInformationAs(Object o) {
		if (!super.hasTheSameInformationAs(o))
			return false;

		FundrefAuthorityValue that = (FundrefAuthorityValue) o;

		for (String key : otherMetadata.keySet()) {
			if (otherMetadata.get(key) != null){
				List<String> metadata = otherMetadata.get(key);
				List<String> otherMetadata = that.otherMetadata.get(key);
				if (otherMetadata == null) {
					return false;
				} else {
					HashSet<String> metadataSet = new HashSet<String>(metadata);
					HashSet<String> otherMetadataSet = new HashSet<String>(otherMetadata);
					if (!metadataSet.equals(otherMetadataSet)) {
						return false;
					}
				}
			} else {
				if (that.otherMetadata.get(key) != null){
					return false;
				}
			}
		}

		return true;
	}
}
