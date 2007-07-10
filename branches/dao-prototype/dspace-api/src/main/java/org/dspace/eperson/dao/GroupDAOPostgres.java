/*
 * GroupDAOPostgres.java
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.proxy.GroupProxy;
import org.dspace.content.uri.ObjectIdentifier;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * @author James Rutherford
 */
public class GroupDAOPostgres extends GroupDAO
{
    public GroupDAOPostgres(Context context)
    {
        this.context = context;
    }

    public Group create() throws AuthorizeException
    {
        Group group = super.create();

        try
        {
            UUID uuid = UUID.randomUUID();

            TableRow row = DatabaseManager.create(context, "epersongroup");
            row.setColumn("uuid", uuid.toString());
            DatabaseManager.update(context, row);

            int id = row.getIntColumn("eperson_group_id");

            return super.create(id, uuid);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public Group retrieve(int id)
    {
        Group group = super.retrieve(id);

        if (group != null)
        {
            return group;
        }

        try
        {
            TableRow row = DatabaseManager.find(context, "epersongroup", id);

            if (row == null)
            {
                log.warn("group " + id + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public Group retrieve(UUID uuid)
    {
        Group group = super.retrieve(uuid);

        if (group != null)
        {
            return group;
        }

        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "epersongroup", "uuid", uuid.toString());

            if (row == null)
            {
                log.warn("group " + uuid + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public Group retrieve(String name)
    {
        Group group = super.retrieve(name);

        if (group != null)
        {
            return group;
        }

        try
        {
            TableRow row = DatabaseManager.findByUnique(context,
                    "epersongroup", "name", name);

            if (row == null)
            {
                log.warn("group " + name + " not found");
                return null;
            }
            else
            {
                return retrieve(row);
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    private Group retrieve(TableRow row)
    {
        int id = row.getIntColumn("eperson_group_id");

        Group group = super.retrieve(id);

        if (group != null)
        {
            return group;
        }

        group = new GroupProxy(context, id);
        populateGroupFromTableRow(group, row);

        context.cache(group, id);

        return group;
    }

    public void update(Group group) throws AuthorizeException
    {
        super.update(group);

        try
        {
            TableRow row =
                DatabaseManager.find(context, "epersongroup", group.getID());

            if (row != null)
            {
                update(group, row);
            }
            else
            {
                throw new RuntimeException("Didn't find group " +
                        group.getID());
            }
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * FIXME: Look back into ItemDAOPostgres to see how we were cunning there
     * about updating Bundles + Bitstreams and use that below for EPeople and
     * Groups.
     */
    private void update(Group group, TableRow row) throws AuthorizeException
    {
        try
        {
            // Redo eperson mappings if they've changed
//            if (epeopleChanged)
//            {
                // Remove any existing mappings
                DatabaseManager.updateQuery(context,
                        "delete from epersongroup2eperson where eperson_group_id= ? ",
                        group.getID());

                for (EPerson eperson : group.getMembers())
                {
                    TableRow mappingRow = DatabaseManager.create(context,
                            "epersongroup2eperson");
                    mappingRow.setColumn("eperson_id", eperson.getID());
                    mappingRow.setColumn("eperson_group_id", group.getID());
                    DatabaseManager.update(context, mappingRow);
                }

//                epeopleChanged = false;
//            }

            // Redo Group mappings if they've changed
//            if (groupsChanged)
//            {
                // Remove any existing mappings
                DatabaseManager.updateQuery(context,
                        "delete from group2group where parent_id= ? ",
                        group.getID());

                // Add new mappings
                for (Group child : group.getSubGroups())
                {
                    TableRow mappingRow = DatabaseManager.create(context,
                            "group2group");
                    mappingRow.setColumn("parent_id", group.getID());
                    mappingRow.setColumn("child_id", child.getID());
                    DatabaseManager.update(context, mappingRow);
                }

                // groups changed, now change group cache
                rethinkGroupCache();

//                groupsChanged = false;
//            }

            populateTableRowFromGroup(group, row);
            DatabaseManager.update(context, row);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * FIXME We need link() and unlink() for EPerson <--> Group and
     * Group <--> Group mapping
     */
    public void delete(int id) throws AuthorizeException
    {
        try
        {
            // Remove any group memberships first
            DatabaseManager.updateQuery(context,
                    "DELETE FROM epersongroup2eperson " +
                    "WHERE eperson_group_id = ? ",
                    id);

            // remove any group2groupcache entries
            DatabaseManager.updateQuery(context,
                    "DELETE FROM group2groupcache " +
                    "WHERE parent_id = ? OR child_id = ? ",
                    id, id);

            // Now remove any group2group assignments
            DatabaseManager.updateQuery(context,
                    "DELETE FROM group2group " +
                    "WHERE parent_id = ? OR child_id = ? ",
                    id, id);

            // don't forget the new table
            DatabaseManager.updateQuery(context,
                    "DELETE FROM epersongroup2workspaceitem " +
                    "WHERE eperson_group_id = ? ",
                    id);

            // Remove ourself
            DatabaseManager.delete(context, "epersongroup", id);
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public List<Group> getGroups(int sortField)
    {
        String s;

        switch (sortField)
        {
            case Group.ID:
                s = "eperson_group_id";
                break;
            case Group.NAME:
            default:
                s = "name";
        }

        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "epersongroup",
                    "SELECT eperson_group_id FROM epersongroup ORDER BY " + s);

            List<Group> groups = new ArrayList<Group>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("eperson_group_id");
                groups.add(retrieve(id));
            }

            return groups;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public List<Group> getAllGroups(EPerson eperson)
    {
        try
        {
            // two queries - first to get groups eperson is a member of
            // second query gets parent groups for groups eperson is a member of
            TableRowIterator tri = DatabaseManager.queryTable(context,
                    "epersongroup2eperson",
                    "SELECT eperson_group_id " +
                    "FROM epersongroup2eperson WHERE eperson_id = ?",
                     eperson.getID());

            Set<Integer> groupIDs = new HashSet<Integer>();

            for (TableRow row : tri.toList())
            {
                int childID = row.getIntColumn("eperson_group_id");
                groupIDs.add(childID);
            }

            // Also need to get all "Special Groups" user is a member of!
            // Otherwise, you're ignoring the user's membership to these groups!
            for (Group group : context.getSpecialGroups())
            {
                groupIDs.add(group.getID());
            }

            // now we have all owning groups, also grab all parents of owning groups
            // yes, I know this could have been done as one big query and a union,
            // but doing the Oracle port taught me to keep to simple SQL!

            String groupQuery = "";

            // Build a list of query parameters
            Object[] parameters = new Object[groupIDs.size()];
            int idx = 0;
            for (Integer groupID : groupIDs)
            {
                parameters[idx++] = groupID;

                groupQuery += "child_id= ? ";

                if (idx < groupIDs.size())
                {
                    groupQuery += " OR ";
                }
            }

            List<Group> groups = new ArrayList<Group>();

            if (groupIDs.size() == 0)
            {
                // don't do query, isn't member of any groups
                return groups;
            }

            // was member of at least one group
            // NOTE: even through the query is built dynamicaly all data is
            // seperated into the the parameters array.
            tri = DatabaseManager.queryTable(context, "group2groupcache",
                    "SELECT * FROM group2groupcache WHERE " + groupQuery,
                    parameters);

            for (TableRow row : tri.toList())
            {
                int parentID = row.getIntColumn("parent_id");
                groupIDs.add(parentID);
            }

            for (Integer id : groupIDs)
            {
                groups.add(retrieve(id));
            }

            return groups;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
    }

    public List<Group> search(String query, int offset, int limit)
	{
		String params = "%" + query.toLowerCase() + "%";
		String dbquery =
            "SELECT eperson_group_id FROM epersongroup " +
            "WHERE name ILIKE ? " +
            "OR eperson_group_id = ? " +
            "ORDER BY name ASC";

		if (offset >= 0 && limit > 0)
        {
			dbquery += " LIMIT " + limit + " OFFSET " + offset;
		}

        // When checking against the eperson-id, make sure the query can be
        // made into a number
		Integer int_param;
		try
        {
			int_param = Integer.valueOf(query);
		}
		catch (NumberFormatException e)
        {
			int_param = new Integer(-1);
		}

        try
        {
            TableRowIterator tri = DatabaseManager.query(context, dbquery,
                    new Object[] {params, int_param});

            List<Group> groups = new ArrayList<Group>();

            for (TableRow row : tri.toList())
            {
                int id = row.getIntColumn("eperson_group_id");
                groups.add(retrieve(id));
            }

            return groups;
        }
        catch (SQLException sqle)
        {
            throw new RuntimeException(sqle);
        }
	}

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    private void populateTableRowFromGroup(Group group, TableRow row)
    {
        row.setColumn("name", group.getName());
    }

    /**
     * Regenerate the group cache AKA the group2groupcache table in the
     * database - meant to be called when a group is added or removed from
     * another group
     */
    private void rethinkGroupCache() throws SQLException
    {
        // read in the group2group table
        TableRowIterator tri = DatabaseManager.queryTable(context,
                "group2group", "SELECT * FROM group2group");

        Map<Integer, Set<Integer>> parents =
            new HashMap<Integer, Set<Integer>>();

        for (TableRow row : tri.toList())
        {
            Integer parentID = row.getIntColumn("parent_id");
            Integer childID = row.getIntColumn("child_id");

            // if parent doesn't have an entry, create one
            if (!parents.containsKey(parentID))
            {
                Set<Integer> children = new HashSet<Integer>();

                // add child id to the list
                children.add(childID);
                parents.put(parentID, children);
            }
            else
            {
                // parent has an entry, now add the child to the parent's record
                // of children
                Set<Integer> children = (Set<Integer>) parents.get(parentID);
                children.add(childID);
            }
        }
        
        // now parents is a hash of all of the IDs of groups that are parents
        // and each hash entry is a hash of all of the IDs of children of those
        // parent groups
        // so now to establish all parent,child relationships we can iterate
        // through the parents hash

        for (Integer parentID : parents.keySet())
        {
            Set<Integer> myChildren = getChildren(parents, parentID);

            for (Integer childID : myChildren)
            {
                ((Set<Integer>) parents.get(parentID)).add(childID);
            }
        }

        // empty out group2groupcache table
        DatabaseManager.updateQuery(context,
                "DELETE FROM group2groupcache WHERE id >= 0");

        for (Integer parent : parents.keySet())
        {
            Set<Integer> children = parents.get(parent);

            for (Integer child : children)
            {
                TableRow row = DatabaseManager.create(context,
                        "group2groupcache");

                int parentID = parent.intValue();
                int childID = child.intValue();

                row.setColumn("parent_id", parentID);
                row.setColumn("child_id", childID);

                DatabaseManager.update(context, row);
            }
        }
    }

    /**
     * Used recursively to generate a map of ALL of the children of the given
     * parent
     * 
     * @param parents
     *            Map of parent,child relationships
     * @param parent
     *            the parent you're interested in
     * @return Map whose keys are all of the children of a parent
     */
    private Set getChildren(Map<Integer, Set<Integer>> parents, Integer parent)
    {
        Set<Integer> myChildren = new HashSet<Integer>();

        // degenerate case, this parent has no children
        if (!parents.containsKey(parent))
        {
            return myChildren;
        }

        // got this far, so we must have children
        for (Integer childID : parents.get(parent))
        {
            // add this child's ID to our return set
            myChildren.add(childID);

            // and now its children
            myChildren.addAll(getChildren(parents, childID));
        }

        return myChildren;
    }
}
