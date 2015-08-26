/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.ldap;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.AuthorityValueGenerator;
import org.dspace.authority.PersonAuthorityValue;

import java.util.Map;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.ObjectUtils;

public class LDAPAuthorityValue extends PersonAuthorityValue {
	
	
	private static Logger log = Logger.getLogger(PersonAuthorityValue.class);
	
	
	private String title = null;
	private boolean isActive = true;
	private String pid = null;
	private String department = null;
	
	public void setDepartment(String department){
		this.department = department;
	}
	
	public String getDepartment(){
		return department;
	}
	
	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}
	
	public boolean getIsActive(){
		return isActive;
	}
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public String getTitle(){
		return title;
	}
	
	public void setPid(String pid){
		this.pid = pid;
	}
	
	public String getPid(){
		return pid;
	}
	

	 public LDAPAuthorityValue() {
	    }

	    public LDAPAuthorityValue(SolrDocument document) {
	        super(document);
	        
	        log.info("LDAPAuthorityValue");
	    }
	    
	    @Override
	    public AuthorityValue newInstance(String info) {
	    	AuthorityValue authorityValue = null;
	        if (StringUtils.isNotBlank(info)) {
	            LDAPAuthority ldapAuth = LDAPAuthority.getLDAPAuthority();
	            authorityValue = ldapAuth.queryAuthorityID(info);
	        } else {
	            authorityValue = new LDAPAuthorityValue();
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

	        LDAPAuthorityValue that = (LDAPAuthorityValue) o;

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
	    public SolrInputDocument getSolrInputDocument() {
	    	   	
	    	SolrInputDocument doc = super.getSolrInputDocument();
	        
	        doc.addField("label_is_active", isActive);
	        if(StringUtils.isNotBlank(pid)){
	        	doc.addField("label_pid", pid);
	        }
	        if(StringUtils.isNotBlank(department)){
	        	doc.addField("label_department", department);
	        }
	        if(StringUtils.isNotBlank(title)){
	        	doc.addField("label_title", title);
	        }
	        if(StringUtils.isNotBlank(getInstitution())){
	        	doc.addField("institution", getInstitution());
	        }
	        
	        return doc;
	    }

	    @Override
	    public void setValues(SolrDocument document) {
	        super.setValues(document);
	        setFirstName(ObjectUtils.toString(document.getFieldValue("first_name")));
	        setLastName(ObjectUtils.toString(document.getFieldValue("last_name")));
	       department = ObjectUtils.toString(document.getFieldValue("label_department"));
	       title = ObjectUtils.toString(document.getFieldValue("label_title"));
	       setId(ObjectUtils.toString(document.getFieldValue("id")));
	       
	      String isActive = ObjectUtils.toString(document.getFieldValue("label_is_active"));
	      if(isActive != null && isActive.equals("true")){
	    	  this.isActive = true;
	      }else{
	    	  this.isActive = false;
	      }
	        
	        Collection<Object> document_name_variant = document.getFieldValues("name_variant");
	        if (document_name_variant != null) {
	            for (Object name_variants : document_name_variant) {
	                addNameVariant(String.valueOf(name_variants));
	            }
	        }
	        if (document.getFieldValue("institution") != null) {
	            setInstitution(String.valueOf(document.getFieldValue("institution")));
	        }

	        Collection<Object> emails = document.getFieldValues("email");
	        if (emails != null) {
	            for (Object email : emails) {
	                addEmail(String.valueOf(email));
	            }
	        }
	        setValue(getName());
	    }


	    @Override
	    public Map<String, String> choiceSelectMap() {

	        Map<String, String> map = super.choiceSelectMap();
	        
	        map.put("ldap-person", getId());

	       return map;
	    }

	    @Override
	    public String getAuthorityType() {
	        return "ldap-person";
	    }

	    @Override
	    public String generateString() {
	    
	    	return getId();
	    //return AuthorityValueGenerator.GENERATE + getAuthorityType() + AuthorityValueGenerator.SPLIT + getId();
	        
	        // the part after "AuthorityValueGenerator.GENERATE + getAuthorityType() + AuthorityValueGenerator.SPLIT" is the value of the "info" parameter in public AuthorityValue newInstance(String info)
	    }

	   

	    @Override
	    public String toString() {
	    	
	        return "LDAPAuthorityValue{" +
	                "firstName='" + getFirstName() + '\'' +
	                ", lastName='" + getLastName() + '\'' +
	                ", nameVariants='" + getNameVariants() +
	                ", institution='" + getInstitution() + '\'' +
	                ", emails='" + getEmails() + '\'' +
	                ", department='" + department + '\'' +
	                ", username='" + getId() + '\'' +
	                ", pid='" + pid +
	                "} " + super.toString();
	    }
	    @Override
	    public boolean hasTheSameInformationAs(Object o) {
	        if (this == o) {
	            return true;
	        }
	        if (o == null || getClass() != o.getClass()) {
	            return false;
	        }
	        if(!super.hasTheSameInformationAs(o)){
	            return false;
	        }

	        LDAPAuthorityValue that = (LDAPAuthorityValue) o;
	      

	        if (department != null ? !department.equals(that.department) : that.department != null) {
	            return false;
	        }
	        if (pid != null ? !pid.equals(that.pid) : that.pid != null) {
	            return false;
	        }
	        if (title != null ? !title.equals(that.title) : that.title != null) {
	            return false;
	        }

	        return true;
	    }
}
