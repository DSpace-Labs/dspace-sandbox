/*
 * AuthenticationMethod.java
 *
 * Version: $Revision: 2417 $
 *
 * Date: $Date: 2007-12-10 17:00:07 +0000 (Mon, 10 Dec 2007) $
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
package org.dspace.authenticate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;


/**
 * Implement this interface to participate in the stackable
 * authentication mechanism.  See the <code>AuthenticationManager</code>
 * class for details about configuring authentication handlers.
 * <p>
 * Each <em>authentication method</em> provides a way to map
 * "credentials" supplied by the client into a DSpace e-person.
 * "Authentication" is when the credentials are compared against some
 * sort of registry or other test of authenticity.
 * <p>
 * The DSpace instance may configure many authentication methods, in a
 * "stack".  The same credentials are passed to each method in turn
 * until one accepts them, so each method need only attempt to interpret
 * and validate the credentials and fail gracefully if they are not
 * appropriate for it.  The next method in the stack is then called.
 *
 * @see AuthenticationManager
 *
 * @author Larry Stone
 * @version $Revision: 2417 $
 */
public interface AuthenticationMethod {

    /**
     * Symbolic return values for authenticate() method:
     */

    /** Authenticated OK, EPerson has been set. */
    public static final int SUCCESS = 1;

    /** User exists, but credentials (<em>e.g.</em> passwd) don't match. */
    public static final int BAD_CREDENTIALS = 2;

    /** Not allowed to login this way without X.509 certificate. */
    public static final int CERT_REQUIRED = 3;

    /** User not found using this method. */
    public static final int NO_SUCH_USER = 4;

    /** User or password is not appropriate for this method. */
    public static final int BAD_ARGS = 5;


    /**
     * Predicate, whether to allow new EPerson to be created.
     * The answer determines whether a new user is created when
     * the credentials describe a valid entity but there is no
     * corresponding EPerson in DSpace yet.
     * The EPerson is only created if authentication succeeds.
     *
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case it's needed. May be null.
     * @param username
     *            Username, if available.  May be null.
     * @return true if new ePerson should be created.
     */
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String username);

    /**
     * Initialize a new EPerson record for a self-registered new user.
     * Set any data in the EPerson that is specific to this authentication
     * method.
     *
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case it's needed. May be null.
     * @param eperson
     *            newly created EPerson record - email + information from the
     *            registration form will have been filled out.
     */
    public void initEPerson(Context context,
                            HttpServletRequest request,
                            EPerson eperson);

    /**
     * Should (or can) we allow the user to change their password.
     * Note that this means the password stored in the EPerson record, so if
     * <em>any</em> method in the stack returns true, the user is
     * allowed to change it.
     *
     * @param context
     *            DSpace context
     * @param request
     *            HTTP request, in case it's needed. May be null.
     * @param username
     *            Username, if available.  May be null.
     * @return true if this method allows user to change ePerson password.
     */
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username);

    /**
     * Predicate, is this an implicit authentication method.
     * An implicit method gets credentials from the environment (such as
     * an HTTP request or even Java system properties) rather than the
     * explicit username and password.  For example, a method that reads
     * the X.509 certificates in an HTTPS request is implicit.
     *
     * @return true if this method uses implicit authentication.
     */
    public boolean isImplicit();

    /**
     * Get list of extra groups that user implicitly belongs to.
     * Returns IDs of any EPerson-groups that the user authenticated by
     * this request is <em>implicitly</em> a member of -- e.g.
     * a group that depends on the client network-address.
     * <p>
     * It might make sense to implement this method by itself in a separate
     * authentication method that just adds special groups, if the
     * code doesn't belong with any existing auth method.
     * The stackable authentication system was designed expressly to
     * separate functions into "stacked" methods to keep your
     * site-specific code  modular and tidy.
     *
     * @param context
     *  A valid DSpace context.
     *
     * @param request
     *  The request that started this operation, or null if not applicable.
     *
     * @return array of EPerson-group IDs, possibly 0-length, but
     * never <code>null</code>.
     */
    public int[] getSpecialGroups(Context context, HttpServletRequest request);

    /**
     * Authenticate the given or implicit credentials.
     * This is the heart of the authentication method: test the
     * credentials for authenticity, and if accepted, attempt to match
     * (or optionally, create) an <code>EPerson</code>.  If an <code>EPerson</code> is found it is
     * set in the <code>Context</code> that was passed.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @param username
     *  Username (or email address) when method is explicit. Use null for
     *  implicit method.
     *
     * @param password
     *  Password for explicit auth, or null for implicit method.
     *
     * @param realm
     *  Realm is an extra parameter used by some authentication methods, leave null if
     *  not applicable.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @return One of:
     *   SUCCESS, BAD_CREDENTIALS, CERT_REQUIRED, NO_SUCH_USER, BAD_ARGS
     * <p>Meaning:
     * <br>SUCCESS         - authenticated OK.
     * <br>BAD_CREDENTIALS - user exists, but credentials (e.g. passwd) don't match
     * <br>CERT_REQUIRED   - not allowed to login this way without X.509 cert.
     * <br>NO_SUCH_USER    - user not found using this method.
     * <br>BAD_ARGS        - user/pw not appropriate for this method
     */

    public int authenticate(Context context,
                            String username,
                            String password,
                            String realm,
                            HttpServletRequest request);

    /**
     * Get login page to which to redirect.
     * Returns URL (as string) to which to redirect to obtain
     * credentials (either password prompt or e.g. HTTPS port for client
     * cert.); null means no redirect.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @param response
     *  The HTTP response from the servlet method.
     *
     * @return fully-qualified URL or null
     */
    public String loginPageURL(Context context,
                            HttpServletRequest request,
                            HttpServletResponse response);

    /**
     * Get title of login page to which to redirect.
     * Returns a <i>message key</i> that gets translated into the title
     * or label for "login page" (or null, if not implemented) This
     * title may be used to identify the link to the login page in a
     * selection menu, when there are multiple ways to login.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @return title text.
     */
    public String loginPageTitle(Context context);
}
