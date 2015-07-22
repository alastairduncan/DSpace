/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.rest;

import org.apache.http.impl.client.HttpClientBuilder;
import org.dspace.authority.util.XMLUtils;
import org.dspace.core.ConfigurationManager;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import java.io.InputStream;


public class ProxyRESTConnector extends RESTConnector {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(RESTConnector.class);

    private String url = null;
   

    public ProxyRESTConnector(String url) {
	super(url);
	this.url = url;
    }

    public InputStream getIS(String path) {

	log.debug("getIS " + path);
	InputStream result = null;
	path = trimSlashes(path);

	String fullPath = url + '/' + path;
	HttpGet httpGet = new HttpGet(fullPath);
	String proxyHost = ConfigurationManager.getProperty("http.proxy.host");
	int proxyPort = ConfigurationManager.getIntProperty("http.proxy.port", 80);

	if (StringUtils.isNotBlank(proxyHost)) {

	    HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
	    RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
	    httpGet.setConfig(config);
	}
	try {
	    HttpClient httpClient = HttpClientBuilder.create().build();
	    HttpResponse getResponse = httpClient.execute(httpGet);
	    // do not close this httpClient
	    result = getResponse.getEntity().getContent();

	} catch (Exception e) {
	    getGotError(e, fullPath);
	}
	return result;
    }
    
    @Override
    public Document get(String path) {
        Document document = null;

        InputStream result = null;
        path = trimSlashes(path);

        String fullPath = url + '/' + path;
        HttpGet httpGet = new HttpGet(fullPath);
        String proxyHost = ConfigurationManager.getProperty("http.proxy.host");
	int proxyPort = ConfigurationManager.getIntProperty("http.proxy.port", 80);

	if (StringUtils.isNotBlank(proxyHost)) {

	    HttpHost proxy = new HttpHost(proxyHost, proxyPort, "http");
	    RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
	    httpGet.setConfig(config);
	}
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse getResponse = httpClient.execute(httpGet);
            //do not close this httpClient
            result = getResponse.getEntity().getContent();
            document = XMLUtils.convertStreamToXML(result);

        } catch (Exception e) {
            getGotError(e, fullPath);
        }

        return document;
    }

    

}
