/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import org.dspace.authority.AuthorityValue;
import org.dspace.authority.orcid.model.Bio;
import org.dspace.authority.orcid.xml.RecordConverter;
import org.dspace.authority.orcid.xml.SearchResultsConverter;
import org.dspace.authority.rest.RestSource;
import org.dspace.authority.util.XMLUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.utils.DSpace;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.CloseableHttpPipeliningClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.client.util.HttpAsyncClientUtils;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class Orcid extends RestSource {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(Orcid.class);
    private static Orcid orcid;

    private String url;

    public static Orcid getOrcid() {
        if (orcid == null) {
            orcid = new DSpace().getServiceManager().getServiceByName("OrcidSource", Orcid.class);
        }
        return orcid;
    }

    private Orcid(String url) {
        super(url);
        this.url = url;
    }

    public Bio getBio(String id) {
        Document document = restConnector.get("v2.1/" + id + "/record");
        RecordConverter converter = new RecordConverter();
        Bio bio = converter.convert(document);
        bio.setOrcid(id);
        return bio;
    }

    private List<String> search(String name, int start, int rows) {
        // Check if user has searched for an ORCID instead of a name
        Pattern p = Pattern.compile("\\d{4}-\\d{4}-\\d{4}-\\d{3}[\\dX]");
        Matcher m = p.matcher(name);
        if (m.matches()) {
            List<String> result = new ArrayList<String>();
            result.add(m.group());
            return result;
        }

        // Create a query string that checks each part of the name in each of the name fields
        List<String> queryParts = new ArrayList<String>();
        String[] parts = name.trim().split("[ -]");
        for (String part : parts) {
            queryParts.add("(given-names:" + part + " OR family-name:" + part + " OR credit-name:" + part + " OR other-names:" + part + ")");
        }
        String query = StringUtils.join(queryParts, " AND ");

        Document searchResults = restConnector.get("v2.1/search?q=" + URLEncoder.encode(query) + "&start=" + start + "&rows=" + rows);

        SearchResultsConverter converter = new SearchResultsConverter();
        return converter.convert(searchResults);
    }

    @Override
    public List<AuthorityValue> queryAuthorities(String text, int max) {
        List<String> orcids = search(text, 0, max);

        List<AuthorityValue> authorities = new ArrayList<AuthorityValue>();
        List<HttpAsyncRequestProducer> producers = new ArrayList<HttpAsyncRequestProducer>();
        List<HttpAsyncResponseConsumer<HttpResponse>> consumers = new ArrayList<HttpAsyncResponseConsumer<HttpResponse>>();

        for (String orcid : orcids) {
            producers.add(HttpAsyncMethods.createGet("https://pub.orcid.org/v2.1/" + orcid + "/record"));
            consumers.add(HttpAsyncMethods.createConsumer());
        }

        CloseableHttpPipeliningClient httpclient = HttpAsyncClients.createPipelining();
        try {
            httpclient.start();

            // Pipeline the requests then wait for the responses
            Future<List<HttpResponse>> future = httpclient.execute(HttpHost.create(this.url), producers, consumers, null);
            List<HttpResponse> responses = future.get();

            for (HttpResponse response : responses) {
                InputStream result = response.getEntity().getContent();

                String s = IOUtils.toString(result, "UTF-8");

                // get rid of namespaces (XPath can't handle them easily)
                // https://stackoverflow.com/a/6606075
                s = s.replaceAll("(<\\?[^<]*\\?>)?", "") // remove preamble
                     .replaceAll("xmlns.*?(\"|\').*?(\"|\')", "") // remove xmlns declaration
                     .replaceAll("(<)(\\w+:)(.*?>)", "$1$3") // remove opening tag prefix
                     .replaceAll("(</)(\\w+:)(.*?>)", "$1$3"); // remove closing tag prefix

                result = new ByteArrayInputStream(s.getBytes("UTF-8"));

                Document document = XMLUtils.convertStreamToXML(result);
                RecordConverter converter = new RecordConverter();
                Bio bio = converter.convert(document);
                authorities.add(OrcidAuthorityValue.create(bio));
            }
        } catch (InterruptedException e) {
            log.debug(e);
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            HttpAsyncClientUtils.closeQuietly(httpclient);
        }

        return authorities;
    }

    @Override
    public AuthorityValue queryAuthorityID(String id) {
        Bio bio = getBio(id);
        return OrcidAuthorityValue.create(bio);
    }
}
