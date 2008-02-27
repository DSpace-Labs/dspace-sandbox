/*
 * TaskListItem.java
 *
 * Version: $Revision: 246 $
 *
 * Date: $Date: 2007-07-19 15:56:33 +0100 (Thu, 19 Jul 2007) $
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
package org.dspace.workflow;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.dspace.eperson.EPerson;

@Entity
public class TaskListItem
{
    private int iD;
//    private int epersonID;
//    private int workflowItemID;
    private EPerson eperson;
    private WorkflowItem workflowItem;

    protected TaskListItem() {}
    
    public TaskListItem(int id)
    {
        this.iD = id;
    }

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    @Column(name="tasklist_id")
    public int getID()
    {
        return iD;
    }

    public void setID(int id)
    {
        iD = id;
    }
    
    @ManyToOne
    @JoinColumn(name="eperson_id")
    public EPerson getEperson()
    {
        return eperson;
    }

    public void setEperson(EPerson eperson)
    {
        this.eperson = eperson;
    }

    @ManyToOne
    @JoinColumn(name="workflow_id")
    public WorkflowItem getWorkflowItem()
    {
        return workflowItem;
    }

    public void setWorkflowItem(WorkflowItem workflowItem)
    {
        this.workflowItem = workflowItem;
    }

    
    @Transient
    public int getEPersonID()
    {
        return eperson.getId();
    }

//    public void setEPersonID(int epersonID)
//    {
//        this.epersonID = epersonID;
//    }

    @Transient
    public int getWorkflowItemID()
    {
        return workflowItem.getId();
    }

//    public void setWorkflowItemID(int workflowItemID)
//    {
//        this.workflowItemID = workflowItemID;
//    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

}
