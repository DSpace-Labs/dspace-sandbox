/*
 * BasicDispatcher.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.event;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.Utils;

/**
 * BasicDispatcher implements the primary task of a Dispatcher: it delivers a
 * filtered list of events, synchronously, to a configured list of consumers. It
 * may be extended for more elaborate behavior.
 * 
 * @version $Revision$
 */
public class BasicDispatcher extends Dispatcher
{
    /** log4j category */
    private static Logger log = Logger.getLogger(BasicDispatcher.class);
    
    public BasicDispatcher(String name)
    {
        super(name);
    }

    public void addConsumerProfile(ConsumerProfile profile)
            throws IllegalArgumentException
    {
        if (consumers.containsKey(profile.getName()))
        {
            throw new IllegalArgumentException(
                    "This dispatcher already has a consumer named \""
                            + profile.getName() + "\"");
        }

        consumers.put(profile.getName(), profile);

        if (log.isDebugEnabled())
        {
            int n = 0;
            for (Iterator i = profile.getFilters().iterator(); i.hasNext(); ++n)
            {
                int f[] = (int[]) i.next();
                log.debug("Adding Consumer=\"" + profile.getName() + "\", instance="
                        + profile.getConsumer().toString() + ", filter["
                        + String.valueOf(n) + "]=(ObjMask="
                        + String.valueOf(f[Event.SUBJECT_MASK])
                        + ", EventMask=" + String.valueOf(f[Event.EVENT_MASK])
                        + ")");
            }
        }
    }

    /**
     * Dispatch all events added to this Context according to configured
     * consumers.
     * 
     * @param ctx
     *            the execution context
     */
    public void dispatch(Context ctx)
    {
        if (!consumers.isEmpty())
        {
            List<Event> events = ctx.getEvents();

            if (events == null)
            {
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug("Processing queue of "
                        + String.valueOf(events.size()) + " events.");
            }

            // transaction identifier applies to all events created in
            // this context for the current transaction. Prefix it with
            // some letters so RDF readers don't mistake it for an integer.
            String tid = "TX" + Utils.generateKey();

            for (Event event : events)
            {
                event.setTransactionID(tid);

                if (log.isDebugEnabled())
                {
                    log.debug("Iterating over "
                            + String.valueOf(consumers.values().size())
                            + " consumers...");
                }

                for (ConsumerProfile profile : consumers.values())
                {
                    if (event.pass(profile.getFilters()))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Sending event to \"" + profile.getName()
                                    + "\": " + event.toString());
                        }
                        try
                        {
                            profile.getConsumer().consume(ctx, event);
                            // Record that event was seen by this consumer
                            event.setBitSet(profile.getName());
                        }
                        catch (Exception e)
                        {
                            log.error("Consumer(\"" + profile.getName()
                                    + "\").consume threw: " + e.toString(), e);
                        }
                    }

                }
            }

            // Call end on the consumers that got synchronous events.
            for (ConsumerProfile profile : consumers.values())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Calling end for consumer \"" + profile.getName()
                            + "\"");
                }
                try
                {
                    profile.getConsumer().end(ctx);
                }
                catch (Exception e)
                {
                    log.error("Error in Consumer(\"" + profile.getName()
                            + "\").end: " + e.toString(), e);
                }
            }
        }
    }

}
