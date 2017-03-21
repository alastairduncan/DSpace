/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.license;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import org.dspace.core.ConfigurationManager;

public class CcLicenses
{
	private static Logger log = Logger.getLogger(CcLicenses.class);

	private CcLicenses() {
	}

	public static List<String> getLicenses() {
		List<String> licenses = new ArrayList<String>();

		for (String license : ConfigurationManager.getProperty("cc-licenses", "licenses").split(",")) {
			licenses.add(license.trim());
		}

		return licenses;
	}

	public static String getLicenseName(String license) {
		return ConfigurationManager.getProperty("cc-licenses", license + ".name").trim();
	}

	public static String getLicenseUri(String license) {
		return ConfigurationManager.getProperty("cc-licenses", license + ".uri").trim();
	}

	public static String getLicenseText(String license) {
		String s;

		if (license.equals("CC0")) {
			s = "To the extent possible under law, the person who associated CC0 with\n"
			  + "this work has waived all copyright and related or neighboring rights to\n"
			  + "the work.\n";
		} else {
			s = "This work is licensed under a\n"
			  + getLicenseName(license) + " License.\n";
		}

		s += "\n";
		s += "See <" + getLicenseUri(license) + "> for full details.\n";

		return s;
	}
}
