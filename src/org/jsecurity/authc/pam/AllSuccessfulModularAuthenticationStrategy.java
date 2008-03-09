/*
 * Copyright (C) 2005-2007 Les Hazlewood
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
package org.jsecurity.authc.pam;

import org.jsecurity.authc.Account;
import org.jsecurity.authc.AuthenticationException;
import org.jsecurity.authc.AuthenticationToken;
import org.jsecurity.authc.UnknownAccountException;
import org.jsecurity.realm.Realm;

import java.util.Collection;

/**
 * <tt>ModularAuthenticationStrategy</tt> implementation that requires <em>all</em> configured realms to
 * <b>successfully</b> process the submitted <tt>AuthenticationToken</tt> during the log-in attempt.
 *
 * <p>If one or more realms do not support the submitted token, or one or more are unable to acquire
 * <tt>Account</tt> for the token, this implementation will immediately fail the log-in attempt for the
 * associated subject (user).
 *
 * @since 0.2
 * @author Les Hazlewood
 */
public class AllSuccessfulModularAuthenticationStrategy implements ModularAuthenticationStrategy {

    public void beforeAllAttempts( Collection<? extends Realm> realms, AuthenticationToken token ) throws AuthenticationException {
        //does nothing - here to satisfy the interface requirements
    }

    public void beforeAttempt( Realm realm, AuthenticationToken token ) throws AuthenticationException {
        if ( !realm.supports( token ) ) {
            String msg = "Realm [" + realm + "] of type [" + realm.getClass().getName() + "] does not support " +
                " the submitted AuthenticationToken [" + token + "].  The [" + getClass().getName() +
                "] implementation requires all configured realm(s) to support and be able to process the submitted " +
                "AuthenticationToken.";
            throw new UnsupportedTokenException( msg );
        }
    }

    public void afterAttempt( Realm realm, AuthenticationToken token, Account account, Throwable t )
        throws AuthenticationException {
        if( t != null ) {
            if ( t instanceof AuthenticationException ) {
                //propagate:
                throw ((AuthenticationException)t);
            } else {
                String msg = "Unable to acquire account data from realm [" + realm + "].  The [" +
                    getClass().getName() + " implementation requires all configured realm(s) to operate successfully " +
                    "for a successful authentication.";
                throw new AuthenticationException( msg, t );
            }
        }
        if ( account == null ) {
            String msg = "Realm [" + realm + "] could not find any associated account data for the submitted " +
                "AuthenticationToken [" + token + "].  The [" + getClass().getName() + "] implementation requires " +
                "all configured realm(s) to acquire valid account data for a submitted token during the " +
                "log-in process.";
            throw new UnknownAccountException( msg );
        }
    }

    public void afterAllAttempts( AuthenticationToken token, Account aggregated )
        throws AuthenticationException {
        //if the authentication process made it this far (because of the potential exceptions that could have been
        //thrown from the other two methods in this class), then the authentication attempt was successful across all
        //configured realms, so do nothing - allow to continue
    }
}