/*
 * InstallItem.java
 *
 * Version: $Revision: 2458 $
 *
 * Date: $Date: 2007-12-21 13:25:59 +0100 (ven, 21 dic 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content;

import java.io.IOException;
import java.util.Date;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ApplicationService;
import org.dspace.core.ArchiveManager;
import org.dspace.core.Context;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.workflow.WorkflowManager;

/**
 * Support to install item in the archive
 * 
 * @author dstuve
 * @version $Revision: 2458 $
 */
public class InstallItem
{
   

    /**
     * Take an InProgressSubmission and turn it into a fully-archived Item,
     * creating a new Handle
     * 
     * @param c
     *            DSpace Context
     * @param is
     *            submission to install
     * 
     * @return the fully archived Item
     */
    public static Item installItem(Context c, InProgressSubmission is)
            throws IOException, AuthorizeException
    {
        return installItem(c, is, null);
    }

    /**
     * Take an InProgressSubmission and turn it into a fully-archived Item.
     *
     * FIXME: This needs to be more flexible about what kind of existing
     * identifiers may be passed in.
     *
     * @param c  current context
     * @param is
     *            submission to install
     * @param value
     *            the existing identifier to give the installed item in
     *            canonical form
     * 
     * @return the fully archived Item
     */
    public static Item installItem(Context c, InProgressSubmission is,
            String value) throws IOException, AuthorizeException
    {
//        ItemDAO itemDAO = ItemDAOFactory.getInstance(c);
//        ExternalIdentifierDAO identifierDAO =
//            ExternalIdentifierDAOFactory.getInstance(c);

        Item item = is.getItem();
        ExternalIdentifier identifier;

        // create accession date
        DCDate now = DCDate.getCurrent();
        //item.addDC("date", "accessioned", null, now.toString());
        MetadataField field = ApplicationService.findMetadataField("date", "available", MetadataSchema.DC_SCHEMA, c);
        item.addMetadata(field, null, now.toString());
        //item.addDC("date", "available", null, now.toString());

        // create issue date if not present
        MetadataValue[] currentDateIssued = item.getMetadata(MetadataSchema.DC_SCHEMA, "date", "issued", Item.ANY);

        if (currentDateIssued.length == 0)
        {
            field = ApplicationService.findMetadataField("date", "issued", MetadataSchema.DC_SCHEMA, c);
            item.addMetadata(field, null, now.toString());            
            //item.addDC("date", "issued", null, now.toString());
        }

        /*  FIXME: items should get persistent identifiers anyway, so this should no longer be necessary

        BUT: what do we do about adding identifiers to metadata!
        // if no previous identifier supplied, create one
        if (value == null)
        {
            // Create persistent identifier. Note that this will create an
            // identifier of the default type (as specified in the
            // configuration).
            identifier = identifierDAO.create(item);
        }
        else
        {
            identifier = identifierDAO.create(item, value);
        }

        String uri = identifier.getURI().toString();

        // Add uri as identifier.uri DC value
        item.addDC("identifier", "uri", null, uri);
*/
        String provDescription = "Made available in DSpace on " + now
                + " (GMT). " + getBitstreamProvenanceMessage(item);

        if (currentDateIssued.length != 0)
        {
            DCDate d = new DCDate(currentDateIssued[0].getValue());
            provDescription = provDescription + "  Previous issue date: "
                    + d.toString();
        }

        // Add provenance description
        field = ApplicationService.findMetadataField("description", "provenance", MetadataSchema.DC_SCHEMA, c);
        item.addMetadata(field, "en", provDescription);
        //item.addDC("description", "provenance", "en", provDescription);

        // create collection2item mapping
        ArchiveManager.addItem(is.getCollection(), item);        

        // set owning collection
        item.setOwningCollection(is.getCollection());

        // set in_archive=true
        item.setArchived(true);

        // save changes ;-)
        // entitymanager takes care of it
        item.setLastModified(new Date());
        //itemDAO.update(item);

        // remove in-progress submission
        //is.deleteWrapper();
        WorkflowManager.deleteWorkflowItem(is.getId(), c);

        // remove the item's policies and replace them with
        // the defaults from the collection
        item.inheritCollectionDefaultPolicies(is.getCollection());

        return item;
    }


    /**
     * Generate provenance-worthy description of the bitstreams contained in an
     * item.
     * 
     * @param myitem  the item generate description for
     * 
     * @return provenance description
     */
    public static String getBitstreamProvenanceMessage(Item myitem)
    {
        // Get non-internal format bitstreams
        Bitstream[] bitstreams = myitem.getNonInternalBitstreams();

        // Create provenance description
        String mymessage = "No. of bitstreams: " + bitstreams.length + "\n";

        // Add sizes and checksums of bitstreams
        for (int j = 0; j < bitstreams.length; j++)
        {
            mymessage = mymessage + bitstreams[j].getName() + ": "
                    + bitstreams[j].getSize() + " bytes, checksum: "
                    + bitstreams[j].getChecksum() + " ("
                    + bitstreams[j].getChecksumAlgorithm() + ")\n";
        }

        return mymessage;
    }
}
