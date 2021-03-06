/*
 * Copyright (c) 2004-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.checker;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ApplicationService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.uri.ResolvableIdentifier;
import org.dspace.uri.IdentifierFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A BitstreamDispatcher that checks all the bitstreams contained within an
 * item, collection or community referred to by persistent identifier.
 * 
 * FIXME: This needs to be changed to store an ObjectIdentifier rather than a
 * String.
 * 
 * @author Jim Downing
 * @author Grace Carpenter
 * @author Nathan Sarr
 * @author James Rutherford
 * 
 */
public class URIDispatcher implements BitstreamDispatcher
{

    /** Log 4j logger. */
    private static final Logger LOG = Logger.getLogger(URIDispatcher.class);

    /** URI to retrieve bitstreams from. */
    String uri = null;

    /** Has the type of object the URI refers to been determined. */
    Boolean init = Boolean.FALSE;

    /** the delegate to dispatch to. */
    ListDispatcher delegate = null;

    /**
     * Database access for retrieving bitstreams
     */
    BitstreamInfoDAO bitstreamInfoDAO;

    /**
     * Blanked off, no-op constructor.
     */
    private URIDispatcher()
    {
        ;
    }

    /**
     * Main constructor.
     * 
     * @param uri
     *            the uri to get bitstreams from (canonical form).
     */
    public URIDispatcher(BitstreamInfoDAO bitInfoDAO, String uri)
    {
        bitstreamInfoDAO = bitInfoDAO;
        this.uri = uri;
    }

    /**
     * Private initialization routine.
     * 
     * @throws SQLException
     *             if database access fails.
     */
    private void init()
    {
        Context context = null;
        int dsoType = -1;

        int id = -1;
        try
        {
            context = new Context();

            ResolvableIdentifier di = IdentifierFactory.resolve(context, uri);
            DSpaceObject dso = di.getObject(context);
            /*
            ExternalIdentifierDAO identifierDAO =
                ExternalIdentifierDAOFactory.getInstance(context);

            ExternalIdentifier identifier = identifierDAO.retrieve(uri);
            ObjectIdentifier oi = identifier.getObjectIdentifier();
            DSpaceObject dso = oi.getObject(context);
            */
            
            id = dso.getID();
            dsoType = dso.getType();
            context.abort();

        }
        catch (SQLException e)
        {
            LOG.error("init error " + e.getMessage(), e);
            throw new RuntimeException("init error" + e.getMessage(), e);

        }
        finally
        {
            // Abort the context if it's still valid
            if ((context != null) && context.isValid())
            {
                context.abort();
            }
        }

        List<Integer> ids = new ArrayList<Integer>();

        switch (dsoType)
        {
        case Constants.BITSTREAM:
            ids.add(new Integer(id));
            break;

        case Constants.ITEM:
            //ids = bitstreamInfoDAO.getItemBitstreams(id);
            ids = ApplicationService.findAllItemBitstreamsId(id, context);
            break;

        case Constants.COLLECTION:
            //ids = bitstreamInfoDAO.getCollectionBitstreams(id);
            ids = ApplicationService.findAllCollectionBitstreamsId(id, context);
            break;

        case Constants.COMMUNITY:
            //ids = bitstreamInfoDAO.getCommunityBitstreams(id);
            ids = ApplicationService.findAllCommunityBitstreamsId(id, context);
            break;
        }

        delegate = new ListDispatcher(ids);
        init = Boolean.TRUE;
    }

    /**
     * Initializes this dispatcher on first execution.
     * 
     * @see org.dspace.checker.BitstreamDispatcher#next()
     */
    public int next(Context context)
    {
        synchronized (init)
        {
            if (init == Boolean.FALSE)
            {
                init();
            }
        }

        return delegate.next(context);
    }
}
