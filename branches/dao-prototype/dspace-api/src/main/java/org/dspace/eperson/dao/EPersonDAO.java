/*
 * EPersonDAO.java
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
package org.dspace.eperson.dao;

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPersonDeletionException;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.history.HistoryManager;

/**
 * @author James Rutherford
 */
public abstract class EPersonDAO
{
    protected Logger log = Logger.getLogger(EPersonDAO.class);

    protected Context context;

    protected enum EPersonMetadataField
    {
        FIRSTNAME ("firstname"),
        LASTNAME ("lastname"),
        PASSWORD ("password"),
        EMAIL ("email"),
        PHONE ("phone"),
        NETID ("netid"),
        LANGUAGE ("language");

        private String name;

        private EPersonMetadataField(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return name;
        }
    }

    public EPerson create() throws AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson");
        }

        return null;
    }

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the object that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
    public EPerson create(int id, UUID uuid) throws AuthorizeException
    {
        EPerson eperson = new EPerson(context, id);

        eperson.setIdentifier(new ObjectIdentifier(uuid));

        update(eperson);

        log.info(LogManager.getHeader(context, "create_eperson", "eperson_id="
                    + id));

        HistoryManager.saveHistory(context, eperson, HistoryManager.REMOVE,
                context.getCurrentUser(), context.getExtraLogInfo());

        return eperson;
    }

    public EPerson retrieve(int id)
    {
        return (EPerson) context.fromCache(EPerson.class, id);
    }

    public EPerson retrieve(UUID uuid)
    {
        return null;
    }

    public void update(EPerson eperson) throws AuthorizeException
    {
        // Check authorisation - if you're not the eperson
        // see if the authorization system says you can
        if (!context.ignoreAuthorization() && (
                    (context.getCurrentUser() == null) ||
                    (eperson.getID() != context.getCurrentUser().getID())))
        {
            AuthorizeManager.authorizeAction(context, eperson, Constants.WRITE);
        }

        log.info(LogManager.getHeader(context, "update_eperson",
                "eperson_id=" + eperson.getID()));

        HistoryManager.saveHistory(context, eperson, HistoryManager.MODIFY,
                context.getCurrentUser(), context.getExtraLogInfo());
    }

    public void delete(int id)
        throws AuthorizeException, EPersonDeletionException
    {
        EPerson eperson = retrieve(id);
        update(eperson); // Sync in-memory object before removal

        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to delete an EPerson");
        }

        HistoryManager.saveHistory(context, this, HistoryManager.REMOVE,
                context.getCurrentUser(), context.getExtraLogInfo());

        // Remove from cache
        context.removeCached(this, id);

        log.info(LogManager.getHeader(context, "delete_eperson",
                "eperson_id=" + id));
    }

    public List<EPerson> getEPeople()
    {
        return getEPeople(EPerson.LASTNAME);
    }

    /**
     * Find all the epeople that match a particular query
     * <ul>
     * <li><code>ID</code></li>
     * <li><code>LASTNAME</code></li>
     * <li><code>EMAIL</code></li>
     * <li><code>NETID</code></li>
     * </ul>
     * 
     * @return array of EPerson objects
     */
    public abstract List<EPerson> getEPeople(int sortField);
}
