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

import org.jsecurity.session.Session;
import org.jsecurity.session.SessionFactory;
import org.jsecurity.util.Initializable;
import org.jsecurity.util.ThreadContext;
import org.jsecurity.web.WebInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TODO class JavaDoc
 *
 * @author Les Hazlewood
 * @since 0.2
 */
public class SessionWebInterceptor extends DefaultWebSessionFactory implements WebInterceptor, Initializable {

    public SessionWebInterceptor() {
        setRequireSessionOnRequest( true );
    }

    public SessionWebInterceptor( SessionFactory sessionFactory ) {
        super( sessionFactory );
        setRequireSessionOnRequest( true );
    }

    public boolean preHandle( HttpServletRequest request, HttpServletResponse response )
        throws Exception {

        Session session = getSession( request, response );
        if ( session != null ) {
            ThreadContext.bind( session );
        }
        //useful for a number of JSecurity components - do it in case this interceptor is the only one configured:
        ThreadContext.bind( SecurityWebSupport.getInetAddress( request ) );
        return true;
    }

    public void postHandle( HttpServletRequest request, HttpServletResponse response )
        throws Exception {
        //check to see if the Session object was created after the request started (this can happen at any point when
        //securityContext.getSession() is called:
        Session session = ThreadContext.getSession();
        if ( session != null ) {
            storeSessionId( session, request, response );
        }
    }

    public void afterCompletion( HttpServletRequest request, HttpServletResponse response, Exception exception )
        throws Exception {
        ThreadContext.unbindSession();
        ThreadContext.unbindInetAddress();
    }
}