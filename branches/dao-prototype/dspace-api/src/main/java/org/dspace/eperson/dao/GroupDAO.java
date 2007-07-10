/*
 * GroupDAO.java
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

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.eperson.proxy.GroupProxy;
import org.dspace.content.uri.ObjectIdentifier;

/**
 * @author James Rutherford
 */
public abstract class GroupDAO
{
    protected Logger log = Logger.getLogger(GroupDAO.class);

    protected Context context;

    public Group create() throws AuthorizeException
    {
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson Group");
        }
    }

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the object that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
    public Group create(int id, UUID uuid) throws AuthorizeException
    {
        Group group = new GroupProxy(context, id);

        group.setIdentifier(new ObjectIdentifier(uuid));

        update(group);

        log.info(LogManager.getHeader(context, "create_group", "group_id="
                + id));

        return group;
    }

    public Group retrieve(int id)
    {
        return (Group) context.fromCache(Group.class, id);
    }

    public Group retrieve(UUID uuid)
    {
        return null;
    }

    public Group retrieve(String name)
    {
        return null;
    }

    public void update(Group group) throws AuthorizeException
    {
        // Check authorisation - if you're not the eperson
        // see if the authorization system says you can
        if (!context.ignoreAuthorization())
        {
            AuthorizeManager.authorizeAction(context, group, Constants.WRITE);
        }

        log.info(LogManager.getHeader(myContext, "update_group", "group_id="
                + group.getID()));
    }

    public void delete(int id) throws AuthorizeException
    {
        Group group = retrieve(id);
        update(group); // Sync in-memory object before removal

        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to delete a Group");
        }

        // Remove any ResourcePolicies that reference this group
        AuthorizeManager.removeGroupPolicies(context, id);

        HistoryManager.saveHistory(context, group, HistoryManager.REMOVE,
                context.getCurrentUser(), context.getExtraLogInfo());

        log.info(LogManager.getHeader(context, "delete_group", "group_id=" +
                    id));

        // Remove from cache
        context.removeCached(group, id);
    }

    public abstract List<Group> getGroups(int sortField);

    /**
     * Returns a list of all the Groups the given EPerson is a member of.
     */
    public abstract List<Group> getGroups(EPerson eperson);

    /**
     * Returns a list of all the subgroups of the given Group (recursively).
     */
    public abstract List<Group> getAllSubGroups(Group group);

    /**
     * Returns a list of all the immediate subgroups of the given Group.
     */
    public abstract List<Group> getImmediateSubGroups(Group group);

    /**
     * Find the groups that match the search query across eperson_group_id or
     * name.
     *
     * @param query The search string
     *
     * @return List of Group objects
     */
    public abstract List<Group> search(String query);

    /**
     * Find the groups that match the search query across eperson_group_id or
     * name.
     *
     * @param query The search string
     * @param offset Inclusive offset
     * @param limit Maximum number of matches returned
     *
     * @return List of Group objects
     */
    public abstract List<Group> search(Context context, String query,
            int offset, int limit);

    /**
     * Find out whether or not the logged in EPerson is a member of the given
     * Group. The reason we take an ID rather than a full object is because we
     * may be able to give a really quick answer without having to actually
     * inspect the Group beyond knowing its ID.
     */
    public boolean currentUserInGroup(int groupID)
    {
        // special, everyone is member of group 0 (anonymous)
        if (group.getID() == 0)
        {
            return true;
        }

        // first, check for membership if it's a special group
        // (special groups can be set even if person isn't authenticated)
        if (context.inSpecialGroup(group.getID()))
        {
            return true;
        }

        EPerson currentuser = context.getCurrentUser();

        // only test for membership if context contains a user
        if (currentuser != null)
        {
            List<Group> groups = getGroups(currentuser);

            return groups.contains(retrieve(groupid));
        }
    }
}
