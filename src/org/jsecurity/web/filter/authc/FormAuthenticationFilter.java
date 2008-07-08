/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jsecurity.web.filter.authc;

import org.jsecurity.authc.AuthenticationException;
import org.jsecurity.authc.UsernamePasswordToken;
import org.jsecurity.web.WebUtils;
import static org.jsecurity.web.WebUtils.getSubject;
import static org.jsecurity.web.WebUtils.toHttp;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.net.InetAddress;

/**
 * Requires the requesting user to be authenticated for the request to continue, and if they are not, forces the user
 * to login via by redirecting them to the {@link #setLoginUrl(String) login page} you configure.
 *
 * <p>If the login attempt fails the AuthenticationException fully qualified class name will be placed as a request
 * attribute under the {@link #setFailureKeyAttribute(String) failureKeyAttribute} key.  This FQCN can then be used as
 * an i18n key or lookup mechanism that can then  be used to show the user why their login attempt failed
 * (e.g. no account, incorrect password, etc).
 *
 * @author Les Hazlewood
 * @author Jeremy Haile
 * @since 0.9
 */
public class FormAuthenticationFilter extends AuthenticationFilter {

    public static final String DEFAULT_ERROR_KEY_ATTRIBUTE_NAME = "jsecLoginFailure";

    public static final String DEFAULT_USERNAME_PARAM = "username";
    public static final String DEFAULT_PASSWORD_PARAM = "password";
    public static final String DEFAULT_REMEMBER_ME_PARAM = "rememberMe";

    private String usernameParam = DEFAULT_USERNAME_PARAM;
    private String passwordParam = DEFAULT_PASSWORD_PARAM;
    private String rememberMeParam = DEFAULT_REMEMBER_ME_PARAM;

    private String failureKeyAttribute = DEFAULT_ERROR_KEY_ATTRIBUTE_NAME;

    public FormAuthenticationFilter() {
        setLoginUrl(DEFAULT_LOGIN_URL);
    }

    public String getUsernameParam() {
        return usernameParam;
    }

    public void setUsernameParam(String usernameParam) {
        this.usernameParam = usernameParam;
    }

    public String getPasswordParam() {
        return passwordParam;
    }

    public void setPasswordParam(String passwordParam) {
        this.passwordParam = passwordParam;
    }

    public String getRememberMeParam() {
        return rememberMeParam;
    }

    public void setRememberMeParam(String rememberMeParam) {
        this.rememberMeParam = rememberMeParam;
    }

    public String getFailureKeyAttribute() {
        return failureKeyAttribute;
    }

    public void setFailureKeyAttribute(String failureKeyAttribute) {
        this.failureKeyAttribute = failureKeyAttribute;
    }

    @Override
    protected void onFilterConfigSet() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("Adding default login url to applied paths.");
        }
        this.appliedPaths.put(getLoginUrl(), null);
    }

    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        if (isLoginRequest(request, response)) {
            if (isLoginSubmission(request, response)) {
                if (log.isTraceEnabled()) {
                    log.trace("Login submission detected.  Attempting to execute login.");
                }
                return executeLogin(request, response);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("Login page view.");
                }
                //allow them to see the login page ;)
                return true;
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Attempting to access a path which requires authentication.  Forwarding to the " +
                        "Authentication url [" + getLoginUrl() + "]");
            }

            saveRequestAndRedirectToLogin(request,response);
            return false;
        }
    }

    protected boolean isLoginSubmission(ServletRequest servletRequest, ServletResponse response) {
        return toHttp(servletRequest).getMethod().equalsIgnoreCase("POST");
    }

    protected boolean executeLogin(ServletRequest request, ServletResponse response) throws Exception {
        String username = getUsername(request, response);
        String password = getPassword(request, response);
        boolean rememberMe = isRememberMe(request, response);
        InetAddress inet = getInetAddress(request, response);

        char[] passwordChars = null;
        if (password != null) {
            passwordChars = password.toCharArray();
        }

        UsernamePasswordToken token = new UsernamePasswordToken(username, passwordChars, rememberMe, inet);

        try {
            getSubject(request, response).login(token);
            issueSuccessRedirect(request, response);
            return false;
        } catch (AuthenticationException e) {
            String className = e.getClass().getName();
            request.setAttribute(getFailureKeyAttribute(), className);
            //login failed, let request continue back to the login page:
            return true;
        }
    }

    protected String getUsername(ServletRequest request, ServletResponse response) {
        return WebUtils.getCleanParam(request, getUsernameParam() );
    }

    protected String getPassword(ServletRequest request, ServletResponse response) {
        return WebUtils.getCleanParam(request, getPasswordParam());
    }

    protected boolean isRememberMe(ServletRequest request, ServletResponse response) {
        return WebUtils.isTrue( request, getRememberMeParam() );
    }

    protected InetAddress getInetAddress(ServletRequest request, ServletResponse response) {
        return WebUtils.getInetAddress(request);
    }
}