/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.gtr;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.AuthorityValueGenerator;

import uk.ac.rcuk.gtr.gtr.api.project.Project;
import uk.ac.rcuk.gtr.gtr.api.project.Projects;
import uk.ac.rcuk.gtr.gtr.api.project.Identifiers;

import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

public class GtrAuthorityValue extends AuthorityValue {

	private static Logger LOG = Logger.getLogger(GtrAuthorityValue.class);
	private String title = null;
	private String identifier = null;
	private String url = null;
	private Map<String, List<String>> otherMetadata = new HashMap<String, List<String>>();

	public GtrAuthorityValue() {
		LOG.debug("GtrAuthorityValue default constructor: ");
	}

	public GtrAuthorityValue(SolrDocument document) {
		LOG.debug("GtrAuthorityValue constructor: ");
		setValues(document);
	}

	public static GtrAuthorityValue getProjectValueFromProjects(String xml) {

		LOG.debug("getProjectValueFromProjects: ");
		Project project = null;

		try {
			JAXBContext gtrContext;
			gtrContext = JAXBContext.newInstance(Projects.class);
			Unmarshaller unmarshaller = gtrContext.createUnmarshaller();
			StringReader reader = new StringReader(xml);
			StreamSource isr = new StreamSource(reader);
			JAXBElement<Projects> root = unmarshaller.unmarshal(isr, Projects.class);
			Projects projectsWrapper = (Projects) root.getValue();

			List<Project> projects = projectsWrapper.getProject();
			if (projects.size() > 0) {

				if (projects.size() > 1) {
					LOG.warn("getProjectValueFromProjects: There's more than 1 project in the xml! " + xml);
				}
				project = projects.get(0);

			} else {
				LOG.debug("getProjectValueFromProjects: no projects in the xml! " + xml);
			}
		} catch (JAXBException e) {
			LOG.error("getProjectValueFromProjects: ", e);
		}
		LOG.debug("getProjectValueFromProjects: parsed the project ok");
		return createAuthorityValue(project);
	}

	private static GtrAuthorityValue createAuthorityValue(Project project) {
		LOG.debug("createAuthorityValue: ");
		GtrAuthorityValue value = new GtrAuthorityValue();
		if (project != null) {
			Identifiers ids = project.getIdentifiers();

			if (ids != null) {
				for (Identifiers.Identifier ident : ids.getIdentifier()) {

					if (ident.getType() != null && ident.getType().equals("RCUK")) {
						value.setIdentifier(ident.getValue());
						value.setValue(ident.getValue());
						break;
					}
				}
			}

			Date date = project.getCreated().toGregorianCalendar().getTime();
			// value.setCreationDate(date);
			value.setId(project.getId());
			value.setTitle(project.getTitle());
			value.setUrl(project.getHref());
			LOG.warn("createAuthorityValue: " + value.toString());
		}
		return value;
	}

	public static GtrAuthorityValue getProjectValue(String xml) {
		LOG.warn("getProjectValue: ");
		Project project = null;
		try {
			JAXBContext gtrContext;

			gtrContext = JAXBContext.newInstance(Project.class);
			Unmarshaller unmarshaller = gtrContext.createUnmarshaller();
			StringReader reader = new StringReader(xml);
			StreamSource isr = new StreamSource(reader);
			JAXBElement<Project> root = unmarshaller.unmarshal(isr, Project.class);
			project = (Project) root.getValue();

		} catch (JAXBException e) {
			LOG.error("getProjectValue: ", e);
		}

		return createAuthorityValue(project);
	}

	@Override
	public AuthorityValue newInstance(String info) {
		LOG.debug("newInstance: ");
		AuthorityValue authorityValue = null;
		if (StringUtils.isNotBlank(info)) {
			GtrAuthority gtrAuth = GtrAuthority.getGtrAuthority();
			authorityValue = gtrAuth.queryAuthorityID(info);
		} else {
			authorityValue = new GtrAuthorityValue();
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

		GtrAuthorityValue that = (GtrAuthorityValue) o;

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
		LOG.debug("getSolrInputDocument: ");
		SolrInputDocument doc = super.getSolrInputDocument();
		if (StringUtils.isNotBlank(url)) {
			doc.addField("label_url", url);
		}
		if (StringUtils.isNotBlank(identifier)) {
			doc.addField("gtr_project_identifier", identifier);
		}
		if (StringUtils.isNotBlank(title)) {
			doc.addField("label_title", title);
		}
		return doc;
	}

	@Override
	public void setValues(SolrDocument document) {
		super.setValues(document);
		LOG.debug("setValues: ");
		identifier = ObjectUtils.toString(document.getFieldValue("label_identifier"));
		url = ObjectUtils.toString(document.getFieldValue("label_url"));
		title = ObjectUtils.toString(document.getFieldValue("label_title"));
	}

	@Override
	public Map<String, String> choiceSelectMap() {
		LOG.debug("choiceSelectMap: ");
		Map<String, String> map = super.choiceSelectMap();

		if (StringUtils.isNotBlank(getId())) {
			map.put("gtr-project", getId());
		}
		if (StringUtils.isNotBlank(title)) {
			map.put("title", title);
		}
		if (StringUtils.isNotBlank(url)) {
			map.put("url", url);
		}
		return map;
	}

	@Override
	public String getAuthorityType() {
		LOG.debug("getAuthorityType: gtr-project ");
		return "gtr-project";
	}

	@Override
	public String generateString() {

		//return AuthorityValueGenerator.GENERATE + getAuthorityType() + AuthorityValueGenerator.SPLIT + getId();
		 return getId();
	}

	@Override
	public String toString() {
		return "GtrAuthorityValue{" + "identifier='" + identifier + '\'' + " title='" + title + '\'' + " url='" + url + '\'' + "} " + super.toString();
	}

	@Override
	public boolean hasTheSameInformationAs(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.hasTheSameInformationAs(o)) {
			return false;
		}

		GtrAuthorityValue that = (GtrAuthorityValue) o;

		if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) {
			return false;
		}
		if (title != null ? !title.equals(that.title) : that.title != null) {
			return false;
		}
		if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) {
			return false;
		}
		if (getUrl() != null ? !getUrl().equals(that.getUrl()) : that.getUrl() != null) {
			return false;
		}

		return true;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
}
