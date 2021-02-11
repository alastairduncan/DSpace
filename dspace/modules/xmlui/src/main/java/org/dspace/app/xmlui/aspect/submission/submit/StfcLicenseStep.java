/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.license.CcLicenses;
import org.xml.sax.SAXException;

public class StfcLicenseStep extends AbstractSubmissionStep
{
	private static final Logger log = Logger.getLogger(StfcLicenseStep.class);

	protected static final Message T_head = message("xmlui.Submission.submit.LicenseStep.head");
	protected static final String T_text1 = "Please select a license under which the work will be distributed. The work can only be licensed by the copyright holder or under legal authority from the copyright holder. Co-creators must be consulted before a license is assigned. Once assigned, a license is permanent and cannot be revoked, nor can the content be changed.";
	protected static final String T_text2 = "<a href=\"https://creativecommons.org/about/cclicenses/\" target=\"_blank\">Creative Commons Licenses</a> are <u><b>not suitable</b></u> for software. When submitting software: Please select “Other” and include a plain text file detailing the license information with your software deposit. You should also seek Line Management approval before submitting.";
	protected static final String T_text3 = "Software with commercial potential or that has been licensed by STFC for commercial use, <u><b>must not</b></u> be deposited in eData. If you are unsure about commercial potential or appropriate software licences, please contact <a href=\"mailto:edata@stfc.ac.uk\">eData@stfc.ac.uk</a> or use the <a href=\"https://stfc.inteum.com/stfc/inventorportal/login.aspx\" target=\"_blank\">STFC Inventor portal</a> to request a licence from the Intellectual Property team. For more advice see our guide on the <a href=\"https://stfc.ent.sirsidynix.net.uk/client/en_GB/library/?rm=MANAGING+YOUR+0%7C%7C%7C1%7C%7C%7C0%7C%7C%7Ctrue\" target=\"blank\">library webpage</a>.";

	public StfcLicenseStep() {
		this.requireSubmission = true;
		this.requireStep = true;
	}

	public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
		// standard header
		Collection collection = submission.getCollection();
		String actionURL = contextPath + "/handle/" + collection.getHandle() + "/submit/" + knot.getId() + ".continue";
		Division div = body.addInteractiveDivision("submit-license", actionURL, Division.METHOD_POST, "primary submission");
		div.setHead(T_submission_head);
		addSubmissionProgressList(div);

		// Default values
		String selected_license = "BY";
		String license_freetext = "";

		Item item = submission.getItem();
		for (Metadatum m : item.getMetadataByMetadataString("dc.rights")) {
			// If it already has a license, put it in the license_freetext field
			selected_license = "other";
			license_freetext = m.value;
		}
		for (String license : CcLicenses.getLicenses()) {
			if (CcLicenses.getLicenseName(license).equals(license_freetext)) {
				// If the selected license is a CC license, select it and clear the license_freetext field
				selected_license = license;
				license_freetext = "";
				break;
			}
		}

		List form = div.addList("select-license", List.TYPE_FORM);
		form.setHead(T_head);
		form.addHtml(T_text1);
		form.addHtml(T_text2);
		form.addHtml(T_text3);

		Radio radio = form.addItem().addRadio("license");
		radio.setLabel("Select license");
		radio.setOptionSelected(selected_license);

		for (String license : CcLicenses.getLicenses()) {
			radio.addOption(license, CcLicenses.getLicenseName(license));
		}

		radio.addOption("other", "Other");

		form.addHtml("If you selected 'other', please provide the name of the license e.g. 'Apache License 2.0':");
		Text text = form.addItem().addText("license_freetext");
		text.setValue(license_freetext);

		// add standard control/paging buttons
		addControlButtons(form);
	}

	public List addReviewSection(List reviewList) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
		List reviewSection = reviewList.addList("submit-review-" + this.stepAndPage, List.TYPE_FORM);
		reviewSection.setHead(T_head);

		Item item = submission.getItem();

		for (Metadatum m : item.getMetadataByMetadataString("dc.rights")) {
			reviewSection.addLabel("License Name");
			reviewSection.addItem(m.value);
		}

		for (Metadatum m : item.getMetadataByMetadataString("dc.rights.uri")) {
			reviewSection.addLabel("License URI");
			reviewSection.addItem().addXref(m.value, m.value);
		}

		return reviewSection;
	}
}
