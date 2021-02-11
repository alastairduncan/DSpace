/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.step;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.LicenseUtils;
import org.dspace.core.Context;
import org.dspace.license.CcLicenses;
import org.dspace.submit.AbstractProcessingStep;

public class StfcLicenseStep extends AbstractProcessingStep
{
	private static Logger log = Logger.getLogger(StfcLicenseStep.class);
	private static final int STATUS_FREETEXT_REQUIRED = 1;
	private static final int STATUS_FREETEXT_AND_NOT_OTHER = 2;

	public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo subInfo) throws AuthorizeException, IOException, SQLException {
		String license = request.getParameter("license");
		String license_freetext = request.getParameter("license_freetext").trim();

		Item item = subInfo.getSubmissionItem().getItem();

		// Remove any existing license
		item.clearMetadata("dc", "rights", null, null);
		item.clearMetadata("dc", "rights", "uri", null);
		item.removeDSpaceLicense();

		if (!license.isEmpty() && !license.equals("other")) {
			// Set dc.rights
			item.addMetadata("dc", "rights", null, null, CcLicenses.getLicenseName(license));

			// Set dc.rights.uri
			item.addMetadata("dc", "rights", "uri", null, CcLicenses.getLicenseUri(license));

			// Create license.txt
			LicenseUtils.grantLicense(context, item, CcLicenses.getLicenseText(license));
		} else if (!license_freetext.isEmpty()) {
			// 'other' selected: use the freetext field
			item.addMetadata("dc", "rights", null, null, license_freetext);
			LicenseUtils.grantLicense(context, item, license_freetext);
		}

		// Commit changes to the database
		item.update();
		context.commit();

		// Don't proceed if 'other' is selected without the freetext field
		if (!license.equals("other") && !license_freetext.isEmpty()) {
			return STATUS_FREETEXT_REQUIRED;
		}

		// Don't proceed if a CC license is selected and the freetext field is used
		if (license.equals("other") && license_freetext.isEmpty()) {
			return STATUS_FREETEXT_AND_NOT_OTHER;
		}

		return STATUS_COMPLETE;
	}

	public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) {
		return 1;
	}
}
