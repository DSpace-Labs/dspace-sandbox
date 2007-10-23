/*
 * RegistryImportException.java
 * 
 * Copyright (c) 2006, Imperial College London.  All rights reserved.
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
 * - Neither the name of Imperial College nor the names of their
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
package org.dspace.administer;

/**
 * @author Richard Jones
 *
 * An exception to report any problems with registry imports
 */
public class RegistryImportException extends Exception
{
    private Exception e;
    
    /**
     * Create an empty authorize exception
     */
    public RegistryImportException()
    {
        super();
    }

    /**
     * create an exception with only a message
     * 
     * @param message
     */
    public RegistryImportException(String message)
    {
        super(message);
    }
    
    /**
     * create an exception with an inner exception and a message
     * 
     * @param	message
     * @param	e
     */
    public RegistryImportException(String message, Throwable e)
    {
    	super(message, e);
    }
    
    /**
     * create an exception with an inner exception
     * 
     * @param	e
     */
    public RegistryImportException(Throwable e)
    {
    	super(e);
    }
    
}
