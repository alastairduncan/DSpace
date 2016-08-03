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
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.curate.Curator;
import org.dspace.handle.HandleManager;

/**
 * Upload step with the advanced embargo system for DSpace. Processes the actual 
 * upload of files for an item being submitted into DSpace.
 * <P>
 * This class performs all the behind-the-scenes processing that
 * this particular step requires.  This class's methods are utilized
 * by both the JSP-UI and the Manakin XML-UI
 *
 * @see org.dspace.app.util.SubmissionConfig
 * @see org.dspace.app.util.SubmissionStepConfig
 * @see org.dspace.submit.step.UploadStep
 * @see org.dspace.submit.AbstractProcessingStep
 *
 * @author Tim Donohue
 * @author Keiji Suzuki
 * @version $Revision$
 */
public class ResumableUploadStep extends UploadWithEmbargoStep
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ResumableUploadStep.class);

    /**
     * Process the upload of a new file!
     *
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     * @param subInfo
     *            submission info object
     *
     * @return Status or error flag which will be processed by
     *         UI-related code! (if STATUS_COMPLETE or 0 is returned,
     *         no errors occurred!)
     */
    @Override
    public int processUploadFile(Context context, HttpServletRequest request,
                                    HttpServletResponse response, SubmissionInfo subInfo)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        boolean formatKnown = true;
        boolean fileOK = false;
        BitstreamFormat bf = null;
        Bitstream b = null;

        //NOTE: File should already be uploaded.
        //Manakin does this automatically via Cocoon.
        //For JSP-UI, the SubmissionController.uploadFiles() does the actual upload

        if (subInfo == null)
        {
            // In any event, if we don't have the submission info, the request
            // was malformed
            return STATUS_INTEGRITY_ERROR;
        }

        // Create the bitstream
        Item item = subInfo.getSubmissionItem().getItem();

        String[] parameterValues = request.getParameterValues("bitstream-id");
        if (parameterValues == null) {
            log.debug("processUploadFile: parameterValues.length is null");
            // could be the case where the files have already been added and the
            // next button has been clicked
            if (item.getBundles().length <= 0) {
                // if there are no uploaded files its an error
                return STATUS_NO_FILES_ERROR;
            } else {
                // if there are uploaded files everything is ok
                return STATUS_COMPLETE;
            }
        }

        //loop through each bitstream-id
        for (int i = 0; i < parameterValues.length; i++)
        {
            {
                String fileDescription = request.getParameter("description");
                String bitstreamId = parameterValues[i];
                String filename = request.getParameter("file-name-" + bitstreamId);

                log.debug("processUploadFile: id " + item.getID() + " bitstreamId " + bitstreamId + " filename " + filename);


                // if information wasn't passed by User Interface, we had a problem
                // with the upload
                if (filename == null || bitstreamId == null)
                {
                    return STATUS_UPLOAD_ERROR;
                }

                // do we already have a bundle?
                Bundle[] bundles = item.getBundles("ORIGINAL");

                if (bundles.length < 1)
                {
                    // set bundle's name to ORIGINAL
                    b = item.createBigSingleBitstream(bitstreamId, "ORIGINAL", item.getID());
                }
                else
                {
                    // we have a bundle already, just add bitstream
                    b = bundles[0].createBigBitstream(bitstreamId, item.getID());
                }

                b.setName(filename);
                b.setSource(bitstreamId);
                b.setDescription(fileDescription);

                // Identify the format
                bf = FormatIdentifier.guessFormat(context, b);
                b.setFormat(bf);

                // Update to DB
                b.update();
                item.update();


                processAccessFields(context, request, subInfo, b);


                // commit all changes to database
                context.commit();


                if ((bf != null) && (bf.isInternal()))
                {
                    log.warn("Attempt to upload file format marked as internal system use only");
                    backoutBitstream(subInfo, b, item);
                    return STATUS_UPLOAD_ERROR;
                }

                // Check for virus
                if (ConfigurationManager.getBooleanProperty("submission-curation", "virus-scan"))
                {
                    Curator curator = new Curator();
                    curator.addTask("vscan").curate(item);
                    int status = curator.getStatus("vscan");
                    if (status == Curator.CURATE_ERROR)
                    {
                        backoutBitstream(subInfo, b, item);
                        return STATUS_VIRUS_CHECKER_UNAVAILABLE;
                    }
                    else if (status == Curator.CURATE_FAIL)
                    {
                        backoutBitstream(subInfo, b, item);
                        return STATUS_CONTAINS_VIRUS;
                    }
                }

                // If we got this far then everything is more or less ok.

                // Comment - not sure if this is the right place for a commit here
                // but I'm not brave enough to remove it - Robin.
                context.commit();

                // save this bitstream to the submission info, as the
                // bitstream we're currently working with
                subInfo.setBitstream(b);

                //if format was not identified
                if (bf == null)
                {
                    return STATUS_UNKNOWN_FORMAT;
                }

            }//end if attribute ends with "-path"
        }//end while


        return STATUS_COMPLETE;


    }

    private void processAccessFields(Context context, HttpServletRequest request, SubmissionInfo subInfo, Bitstream b) throws SQLException, AuthorizeException {
        // ResourcePolicy Management
        boolean isAdvancedFormEnabled= ConfigurationManager.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);
        // if it is a simple form we should create the policy for Anonymous
        // if Anonymous does not have right on this collection, create policies for any other groups with
        // DEFAULT_ITEM_READ specified.
        if(!isAdvancedFormEnabled){
            Date startDate = null;
            try {
                startDate = DateUtils.parseDate(request.getParameter("embargo_until_date"), new String[]{"yyyy-MM-dd", "yyyy-MM", "yyyy"});
            } catch (Exception e) {
                //Ignore start date already null
            }
            String reason = request.getParameter("reason");
            AuthorizeManager.generateAutomaticPolicies(context, startDate, reason, b, (Collection) HandleManager.resolveToObject(context, subInfo.getCollectionHandle()));
        }
    }
}
