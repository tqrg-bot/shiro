/*
 * Copyright (C) 2005 Jeremy Haile
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the
 *
 * Free Software Foundation, Inc.
 * 59 Temple Place, Suite 330
 * Boston, MA 02111-1307
 * USA
 *
 * Or, you may view it online at
 * http://www.opensource.org/licenses/lgpl-license.php
 */
package org.jsecurity.realm.support.ldap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsecurity.authc.*;
import org.jsecurity.authc.support.SimpleAuthenticationInfo;
import org.jsecurity.realm.Realm;
import org.jsecurity.realm.support.AbstractCachingRealm;
import org.jsecurity.realm.support.AuthorizationInfo;
import org.jsecurity.util.UsernamePrincipal;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.security.Principal;
import java.util.*;

/**
 * <p>An {@link Realm} that authenticates with an LDAP
 * server to build the SecurityContext for a user.  This implementation only returns roles for a
 * particular user, and not permissions - but it can be subclassed to build a permission
 * list as well.</p>
 *
 * <p>Implementations would need to implement the
 * {@link #queryForLdapDirectoryInfo(String, javax.naming.ldap.LdapContext)} abstract method,
 * and may wish to override
 * {@link #buildAuthenticationInfo(String, char[],LdapSecurityInfo)}.</p>
 *
 * @see LdapSecurityInfo
 * @see #queryForLdapDirectoryInfo(String, javax.naming.ldap.LdapContext)
 * @see #buildAuthenticationInfo(String, char[],LdapSecurityInfo)
 *
 * @since 0.1
 * @author Jeremy Haile
 */
public abstract class LdapRealm extends AbstractCachingRealm {

    /*--------------------------------------------
    |             C O N S T A N T S             |
    ============================================*/

    /*--------------------------------------------
    |    I N S T A N C E   V A R I A B L E S    |
    ============================================*/
    /**
     * Commons-logger.
     */
    protected transient final Log log = LogFactory.getLog( getClass() );

    /**
     * The type of LDAP authentication to perform.
     */
    protected String authentication = "simple";

    /**
     * A suffix appended to the username. This is typically for
     * domain names.  (e.g. "@MyDomain.local")
     */
    protected String principalSuffix = null;

    /**
     * The search base for the search to perform in the LDAP server.
     * (e.g. OU=OrganizationName,DC=MyDomain,DC=local )
     */
    protected String searchBase = null;

    /**
     * The context factory to use. This defaults to the SUN LDAP JNDI implementation
     * but can be overridden to use custom LDAP factories.
     */
    protected String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";

    /**
     * The LDAP url to connect to. (e.g. ldap://<ldapDirectoryHostname>:<port>)
     */
    protected String url = null;

    /**
     * The LDAP referral property.  Defaults to "follow"
     */
    protected String refferal = "follow";


    /*--------------------------------------------
    |         C O N S T R U C T O R S           |
    ============================================*/

    /*--------------------------------------------
    |  A C C E S S O R S / M O D I F I E R S    |
    ============================================*/

    /*--------------------------------------------
    |               M E T H O D S               |
    ============================================*/
    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public void setPrincipalSuffix(String principalSuffix) {
        this.principalSuffix = principalSuffix;
    }

    public void setSearchBase(String searchBase) {
        this.searchBase = searchBase;
    }

    public void setContextFactory(String contextFactory) {
        this.contextFactory = contextFactory;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setRefferal(String refferal) {
        this.refferal = refferal;
    }

    /*--------------------------------------------
    |  A C C E S S O R S / M O D I F I E R S    |
    ============================================*/

    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;

        LdapSecurityInfo ldapSecurityInfo = null;
        try {

            ldapSecurityInfo = performAuthentication(upToken.getUsername(), upToken.getPassword());

        } catch (NamingException e) {
            final String message = "LDAP naming error while attempting to authenticate user.";
            if( log.isErrorEnabled() ) {
                log.error( message, e );
            }
        }

        if( ldapSecurityInfo != null ) {
            return buildAuthenticationInfo( upToken.getUsername(), upToken.getPassword(), ldapSecurityInfo);
        } else {
            return null;
        }
    }

    /**
     * Builds an {@link org.jsecurity.authc.AuthenticationInfo} object to return based on an {@link LdapSecurityInfo} object
     * returned from {@link #performAuthentication(String, char[])}
     *
     * @param username the username of the user being authenticated.
     * @param password the password of the user being authenticated.
     * @param ldapSecurityInfo the LDAP directory information queried from the LDAP server.
     * @return an instance of {@link org.jsecurity.authc.AuthenticationInfo} that represents the principal, credentials, and
     * roles that this user has.
     */
    protected AuthenticationInfo buildAuthenticationInfo(String username, char[] password, LdapSecurityInfo ldapSecurityInfo) {
        List<Principal> principals = new ArrayList<Principal>( ldapSecurityInfo.getPrincipals().size() + 1 );

        UsernamePrincipal principal = new UsernamePrincipal( username );

        principals.add( principal );
        principals.addAll( ldapSecurityInfo.getPrincipals() );

        return new SimpleAuthenticationInfo( principals, password );
    }

    /**
     * Performs the actual authentication of the user by connecting to the LDAP server, querying it
     * for user information, and returning an {@link LdapSecurityInfo} instance containing the
     * results.
     *
     * <p>Typically, users that need special behavior will not override this method, but will instead
     * override {@link #queryForLdapDirectoryInfo(String, javax.naming.ldap.LdapContext)}</p>
     *
     * @param username the username of the user being authenticated.
     * @param password the password of the user being authenticated.
     *
     * @return the results of the LDAP directory search.
     * @throws NamingException if there is an error accessing the LDAP server.
     */
    protected LdapSecurityInfo performAuthentication(String username, char[] password) throws NamingException {

        if( searchBase == null ) {
            throw new IllegalStateException( "A search base must be specified." );
        }
        if( url == null ) {
            throw new IllegalStateException( "An LDAP URL must be specified of the form ldap://<hostname>:<port>" );
        }


        if( principalSuffix != null ) {
            username = username + principalSuffix;
        }

        Hashtable<String, String> env = initializeLdapContext(username, password);

        if (log.isDebugEnabled()) {
            log.debug( "Initializing LDAP context using URL [" + url + "] for user [" + username + "]." );
        }

        LdapContext ctx = null;
        try {
            ctx = new InitialLdapContext(env, null);

            return queryForLdapDirectoryInfo(username, ctx);


        } catch (javax.naming.AuthenticationException e) {
            throw new IncorrectCredentialException( "User could not be authenticated with LDAP server.", e );

        } finally {
            // Always close the LDAP context
            LdapUtils.closeContext(ctx);
        }
    }

    /**
     * Initializes the LDAP context for authentication with the given username and password.
     * @param username the username to use for authentication.
     * @param password the password to use for authentication.
     * @return a <tt>Hashtable</tt> with the environment properties
     *         that can be used to initialize an LDAP context.
     */
    protected Hashtable<String, String> initializeLdapContext(String username, char[] password) {
        Hashtable<String, String> env = new Hashtable<String, String>(6);

        env.put(Context.SECURITY_AUTHENTICATION, authentication);
        env.put(Context.SECURITY_PRINCIPAL, username);
        env.put(Context.SECURITY_CREDENTIALS, new String( password ));
        env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.REFERRAL, refferal);

        return env;
    }

    /**
     * Helper method used to retrieve all attribute values from a particular context attribute.
     */
    protected Collection<String> getAllAttributeValues(Attribute attr) throws NamingException {
        Set<String> values = new HashSet<String>();
        for (NamingEnumeration e = attr.getAll(); e.hasMore();) {
            String value = (String) e.next();
            values.add( value );
        }
        return values;
    }

    /**
     * <p>Abstract method that should be implemented by subclasses to builds an
     * {@link LdapSecurityInfo} object by querying the LDAP context for the
     * specified username.</p>
     *
     * @param username the username whose information should be queried from the LDAP server.
     * @param ctx the LDAP context that is connected to the LDAP server.
     *
     * @return an {@link LdapSecurityInfo} instance containing information retrieved from LDAP
     * that can be used to build an {@link AuthenticationInfo} instance to return.
     *
     * @throws NamingException if any LDAP errors occur during the search.
     *
     * @see #buildAuthenticationInfo(String, char[],LdapSecurityInfo)
     */
    protected abstract LdapSecurityInfo queryForLdapDirectoryInfo(String username, LdapContext ctx) throws NamingException;

    protected AuthorizationInfo doGetAuthorizationInfo(Principal principal) {
        //todo Implement this for LDAP - Need to configure a system account? -JCH 5/29/06
        throw new UnsupportedOperationException( "This method has not yet been implemented for LDAP." );
    }
}