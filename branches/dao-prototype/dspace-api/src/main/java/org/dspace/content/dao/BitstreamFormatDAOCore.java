/*
 * BitstreamFormatDAOCore.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
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
package org.dspace.content.dao;

import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.content.BitstreamFormat;

/**
 * @author James Rutherford
 */
public class BitstreamFormatDAOCore extends BitstreamFormatDAO
{
    private BitstreamFormatDAO childDAO;

    public BitstreamFormatDAOCore(Context context)
    {
        super(context);
    }

    public BitstreamFormatDAO getChild()
    {
        return childDAO;
    }

    public void setChild(BitstreamFormatDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public BitstreamFormat create() throws AuthorizeException
    {
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators can create bitstream formats");
        }

        BitstreamFormat bitstreamFormat = childDAO.create();

        log.info(LogManager.getHeader(context, "create_bitstream_format",
                "bitstream_format_id=" + bitstreamFormat.getID()));

        return bitstreamFormat;
    }

    public BitstreamFormat retrieve(int id)
    {
        BitstreamFormat bf =
                (BitstreamFormat) context.fromCache(BitstreamFormat.class, id);

        if (bf == null)
        {
            bf = childDAO.retrieve(id);
        }

        return bf;
    }

    public BitstreamFormat retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public BitstreamFormat retrieveByMimeType(String mimeType)
    {
        return childDAO.retrieveByMimeType(mimeType);
    }

    public BitstreamFormat retrieveByShortDescription(String desc)
    {
        return childDAO.retrieveByShortDescription(desc);
    }

    public void update(BitstreamFormat bitstreamFormat)
        throws AuthorizeException
    {
        // Check authorisation - only administrators can change formats
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators can modify bitstream formats");
        }

        log.info(LogManager.getHeader(context, "update_bitstream_format",
                "bitstream_format_id=" + bitstreamFormat.getID()));

        childDAO.update(bitstreamFormat);
    }

    public void delete(int id) throws AuthorizeException
    {
        // Check authorisation - only administrators can delete formats
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "Only administrators can delete bitstream formats");
        }

        // Find "unknown" type
        BitstreamFormat unknown = BitstreamFormat.findUnknown(context);

        if (unknown.getID() == id)
        {
            throw new IllegalArgumentException(
                    "The Unknown bitstream format may not be deleted.");
        }

        BitstreamFormat bitstreamFormat = retrieve(id);
        update(bitstreamFormat); // Sync in-memory object before removal

        context.removeCached(bitstreamFormat, id);

        childDAO.delete(id);
    }

    public List<BitstreamFormat> getBitstreamFormats()
    {
        return childDAO.getBitstreamFormats();
    }

    public List<BitstreamFormat> getBitstreamFormats(String extension)
    {
        return childDAO.getBitstreamFormats(extension);
    }

    public List<BitstreamFormat> getBitstreamFormats(boolean internal)
    {
        return childDAO.getBitstreamFormats(internal);
    }
}