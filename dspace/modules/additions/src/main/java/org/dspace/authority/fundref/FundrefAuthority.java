/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.fundref;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.utils.DSpace;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class FundrefAuthority
{
	private static Logger log = Logger.getLogger(FundrefAuthority.class);

	private static FundrefAuthority fundrefAuthority;

	public static FundrefAuthority getFundrefAuthority() {
		if (fundrefAuthority == null) {
			fundrefAuthority = new DSpace().getServiceManager().getServiceByName("FundrefSource", FundrefAuthority.class);
		}
		return fundrefAuthority;
	}

	private String getUrlContent(String urlString) {
		log.debug("Fetching content from " + urlString);
		String content = null;

		try {
			URL url = new URL(urlString);
			URLConnection urlConnection = url.openConnection();
			urlConnection.setConnectTimeout(5000);
			urlConnection.setReadTimeout(5000);
			content = IOUtils.toString(urlConnection.getInputStream());
		} catch (IOException e) {
			log.error("Error fetching content from " + urlString, e);
		}

		return content;
	}

	//@Override
	public List<AuthorityValue> queryAuthorities(String text, int max) {
		List<AuthorityValue> values = new ArrayList<AuthorityValue>();

		String url = "http://api.crossref.org/funders";
		url += "?rows=" + Integer.toString(max);

		try {
			url += "&query=" + URLEncoder.encode(text, "UTF-8");
		} catch (IOException e) {
			;
		}

		String content = getUrlContent(url);
		if (content == null)
			return values;

		try {
			JSONObject obj = new JSONObject(content);

			String status = obj.getString("status");
			if (!status.equals("ok")) {
				throw new JSONException("Unexpected status: " + status);
			}

			String messageType = obj.getString("message-type");
			if (!messageType.equals("funder-list")) {
				throw new JSONException("Unexpected message-type: " + messageType);
			}

			JSONArray arr = obj.getJSONObject("message").getJSONArray("items");

			for (int i = 0; i < arr.length(); i++) {
				String uri = arr.getJSONObject(i).getString("uri");
				String name = arr.getJSONObject(i).getString("name");
				values.add(FundrefAuthorityValue.create(uri, name));
			}
		} catch (JSONException e) {
			log.error("Error decoding content from " + url, e);
		}

		return values;
	}

	//@Override
	public AuthorityValue queryAuthorityID(String id) {
		String content = getUrlContent(id);
		if (content == null)
			return new FundrefAuthorityValue();

		String name = null;
		String shortName = null;

		try {
			JSONObject obj = new JSONObject(content);
			name = obj.getJSONObject("prefLabel").getJSONObject("Label").getJSONObject("literalForm").getString("content");

			List<JSONObject> labels = new ArrayList();

			JSONArray arr = obj.optJSONArray("altLabel");
			if (arr != null) {
				for (int i = 0; i < arr.length(); i++) {
					labels.add(arr.getJSONObject(i).getJSONObject("Label"));
				}
			} else {
				JSONObject altLabel = obj.optJSONObject("altLabel");
				if (altLabel != null) {
					labels.add(altLabel.getJSONObject("Label"));
				}
			}

			for (JSONObject label : labels) {
				JSONObject usageFlag = label.optJSONObject("usageFlag");
				if (usageFlag == null) {
					continue;
				}

				String resource = usageFlag.optString("resource");
				if (resource.equals("http://data.fundref.org/vocabulary/abbrevName")
				 || resource.equals("http://data.crossref.org/fundingdata/vocabulary/abbrevName")) {
					shortName = label.getJSONObject("literalForm").getString("content");
					break;
				}
			}
		} catch (JSONException e) {
			log.error("Error decoding content from " + id, e);
		}

		return FundrefAuthorityValue.create(id, name, shortName);
	}
}
