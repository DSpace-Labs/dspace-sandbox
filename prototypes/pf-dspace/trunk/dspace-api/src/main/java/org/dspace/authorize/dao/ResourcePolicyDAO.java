/*
 * ResourcePolicyDAO.java
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
package org.dspace.authorize.dao;

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Context;
import org.dspace.content.DSpaceObject;
import org.dspace.eperson.Group;

/**
 * @author James Rutherford
 */
public abstract class ResourcePolicyDAO
{
    protected Logger log = Logger.getLogger(ResourcePolicyDAO.class);

    protected Context context;

    public abstract ResourcePolicy create();

    public ResourcePolicy retrieve(int id)
    {
        return (ResourcePolicy) context.fromCache(ResourcePolicy.class, id);
    }

    public ResourcePolicy retrieve(UUID uuid)
    {
        return null;
    }

    public abstract void update(ResourcePolicy rp);

    public void delete(int id)
    {
        ResourcePolicy rp = retrieve(id);
        update(rp); // Sync in-memory object before removal

        context.removeCached(rp, id);
    }

    public abstract List<ResourcePolicy> getPolicies(DSpaceObject dso);
    public abstract List<ResourcePolicy> getPolicies(Group group);
    public abstract List<ResourcePolicy> getPolicies(DSpaceObject dso,
            Group group);
    public abstract List<ResourcePolicy> getPolicies(DSpaceObject dso,
            int actionID);
}
