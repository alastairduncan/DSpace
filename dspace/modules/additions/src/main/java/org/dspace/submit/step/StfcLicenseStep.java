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

	public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo subInfo) throws AuthorizeException, IOException, SQLException {
		String license = request.getParameter("license");

		Item item = subInfo.getSubmissionItem().getItem();

		// Remove any existing license
		item.clearMetadata("dc", "rights", null, null);
		item.clearMetadata("dc", "rights", "uri", null);
		item.removeDSpaceLicense();

		if (!license.isEmpty()) {
			// Set dc.rights
			item.addMetadata("dc", "rights", null, null, CcLicenses.getLicenseName(license));

			// Set dc.rights.uri
			item.addMetadata("dc", "rights", "uri", null, CcLicenses.getLicenseUri(license));

			// Create license.txt
			LicenseUtils.grantLicense(context, item, CcLicenses.getLicenseText(license));
		}

		// Commit changes to the database
		item.update();
		context.commit();

		return STATUS_COMPLETE;
	}

	public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) {
		return 1;
	}
}
