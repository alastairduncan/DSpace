/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.xml;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public abstract class Converter<T> {

    private static Map<Class, JAXBContext> contextMap = new ConcurrentHashMap<Class, JAXBContext>();

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(Converter.class);

    public abstract T convert(InputStream document);

    protected Object unmarshall(InputStream input, Class<?> type) throws SAXException, URISyntaxException {
        try {
            JAXBContext context = contextMap.get(type);
            if (context == null) {
                context = JAXBContext.newInstance(type);
                contextMap.put(type, context);
            }

            Unmarshaller unmarshaller = context.createUnmarshaller();
            return unmarshaller.unmarshal(input);
        } catch (JAXBException e) {
            throw new RuntimeException("Unable to unmarshall orcid message" + e);
        }
    }
}
