/*
 * Copyright (C) 2005-2008 Les Hazlewood
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
package org.jsecurity.crypto;

/**
 * A JdkKey merely wraps an underlying {@link java.security.Key}, allowing the use of a Jdk Key in the JSecurity
 * crypto API.
 *
 * @author Les Hazlewood
 * @since 1.0
 */
public class JdkKey implements Key, java.security.Key {

    protected java.security.Key key = null;

    public JdkKey( java.security.Key key ) {
        this.key = key;
    }

    public String getAlgorithm() {
        return this.key.getAlgorithm();
    }

    public String getFormat() {
        return this.key.getFormat();
    }

    public byte[] getEncoded() {
        return this.key.getEncoded();
    }
}