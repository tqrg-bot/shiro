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
package org.jsecurity.web.support;

import org.jsecurity.context.SecurityContext;
import org.jsecurity.session.Session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Uses the JSecurity <tt>Session</tt> object as the underlying storage mechanism, using the {@link #getName() name}
 * attribute as the session key.
 *
 * @author Les Hazlewood
 * @since 0.2
 */
public class SessionStore<T> extends AbstractWebStore<T> {

    public SessionStore() {
        super();
    }

    public SessionStore( String name ) {
        super( name );
    }

    public SessionStore( String name, boolean checkRequestParams ) {
        super( name, checkRequestParams );
    }

    protected Session getSession( HttpServletRequest request, HttpServletResponse response ) {

        Session session = null;

        SecurityContext securityContext = getSecurityContext( request, response );

        if ( securityContext != null ) {
            session = securityContext.getSession( false );
        }

        return session;
    }

    public T onRetrieveValue( HttpServletRequest request, HttpServletResponse response ) {
        T value = null;

        Session session = getSession( request, response );
        if ( session != null ) {
            value = (T)session.getAttribute( getName() );
        }

        if ( value != null ) {
            if ( log.isInfoEnabled() ) {
                log.info( "Found value [" + value + "] via JSecurity Session key [" + getName() + "]" );
            }
        } else {
            if ( log.isDebugEnabled() ) {
                log.debug( "No value fround in JSecurity Session via session key [" + getName() + "]" );
            }
        }

        return value;
    }

    public void onStoreValue( T value, HttpServletRequest request, HttpServletResponse response ) {
        SecurityContext securityContext = getSecurityContext( request, response );
        if ( securityContext != null ) {
            Session session = securityContext.getSession();
            if ( session != null ) {
                session.setAttribute( getName(), value );
                if ( log.isDebugEnabled() ) {
                    log.debug( "Set JSecurity Session attribute [" + getName() + "] with value [" + value + "]" );
                }
            }
        }
    }
}