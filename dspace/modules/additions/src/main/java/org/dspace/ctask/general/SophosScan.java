/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;
// above package assignment temporary pending better aysnch release process
// package org.dspace.ctask.integrity;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Process;
import java.lang.ProcessBuilder;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Suspendable;


// Based on ClamScan.java
@Suspendable(invoked= Curator.Invoked.INTERACTIVE)
public class SophosScan extends AbstractCurationTask
{
    private static final String INFECTED_MESSAGE = "had virus detected.";
    private static final String CLEAN_MESSAGE = "had no viruses detected.";
    private static final String SCAN_FAIL_MESSAGE = "Error encountered using virus service - check setup";
    private static final String NEW_ITEM_HANDLE = "in workflow";

    private static Logger log = Logger.getLogger(SophosScan.class);
    
    private int status = Curator.CURATE_UNSET;
    private List<String> results = null;

/*    @Override
    public void init(Curator curator, String taskId) throws IOException
    {
        super.init(curator, taskId);
    } */

    @Override
    public int perform(DSpaceObject dso) throws IOException
    {
        status = Curator.CURATE_SKIP;
        logDebugMessage("The target dso is " + dso.getName());
        if (dso instanceof Item)
        {
            status = Curator.CURATE_SUCCESS;
            Item item = (Item)dso;
            
            try
            {
                Bundle bundle = item.getBundles("ORIGINAL")[0];
                results = new ArrayList<String>();
                for (Bitstream bitstream : bundle.getBitstreams())
                {
                    String filename = bitstream.getFilename();
                    logDebugMessage("Scanning " + bitstream.getName() + " . . . ");
                    int bstatus = scan(filename);
                    if (bstatus == Curator.CURATE_ERROR) {
                        // no point going further - set result and error out
                        setResult(SCAN_FAIL_MESSAGE);
                        status = bstatus;
                        break;
                    }

                    if (bstatus == Curator.CURATE_FAIL) {
                        status = Curator.CURATE_FAIL;
                        results.add(bitstream.getName());
                    }
                }             
            }
            catch (SQLException sqlE)
            {
                throw new IOException(sqlE.getMessage(), sqlE);
            }
            
            if (status != Curator.CURATE_ERROR)
            {
                formatResults(item);
            }
        }
        return status;
    }

    private int scan(String filename)
    {
        int retcode;

        try {
            Process sav = new ProcessBuilder(Arrays.asList("/usr/local/bin/savscan", filename)).start();
            retcode = sav.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error(e.toString());
            return Curator.CURATE_ERROR;
        }

        // virus-free
        if (retcode == 0)
            return Curator.CURATE_SUCCESS;

        // virus found
        if (retcode == 3)
            return Curator.CURATE_FAIL;

        // error
        return Curator.CURATE_ERROR;
    }

    private void formatResults(Item item) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Item: ").append(getItemHandle(item)).append(" ");
        if (status == Curator.CURATE_FAIL)
        {
            sb.append(INFECTED_MESSAGE);
            for (String scanresult : results)
            {
                sb.append("\n").append(scanresult);
            }
        }
        else
        {
            sb.append(CLEAN_MESSAGE);
        }
        setResult(sb.toString());
    }

    private static String getItemHandle(Item item)
    {
        String handle = item.getHandle();
        return (handle != null) ? handle: NEW_ITEM_HANDLE;
    }

    private void logDebugMessage(String message)
    {
        if (log.isDebugEnabled())
        {
            log.debug(message);
        }
    }
}
