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
	protected static final String T_text = "Please select a license under which the work will be distributed. This step requires that the work has been licensed by the copyright holder or that you have the legal authority from the copyright holder to license the work.";

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

		List form = div.addList("select-license", List.TYPE_FORM);
		form.setHead(T_head);
		form.addItem(T_text);

		Radio radio = form.addItem().addRadio("license");
		radio.setLabel("Select license");
		radio.setOptionSelected("BY");

		for (String license : CcLicenses.getLicenses()) {
			radio.addOption(license, CcLicenses.getLicenseName(license));
		}

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
