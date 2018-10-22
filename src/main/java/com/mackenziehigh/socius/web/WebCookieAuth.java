package com.mackenziehigh.socius.web;

import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.web.http_m.Request;
import com.mackenziehigh.socius.web.http_m.Response;

/**
 *
 */
public class WebCookieAuth
{
    @FunctionalInterface
    public interface Authenticator
    {
        public boolean isAuthorized (String user,
                                     String password);
    }

    private final Processor<Request> procRequestIn;

    private final Processor<Response> procResponseOut;

    private WebCookieAuth ()
    {
        this.procRequestIn = null;
        this.procResponseOut = null;
    }
}
