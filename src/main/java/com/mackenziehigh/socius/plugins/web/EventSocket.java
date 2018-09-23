package com.mackenziehigh.socius.plugins.web;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ClientEndpoint
@ServerEndpoint (value = "/echo")
public class EventSocket
{
    @OnOpen
    public void onWebSocketConnect (Session sess)
    {
        System.out.println("Socket Connected: " + sess);
    }

    @OnMessage
    public void onWebSocketText (String message,
                                 Session session)
    {
        System.out.println("Received TEXT message: " + message);
        session.getAsyncRemote().sendText("Hello");
    }

    @OnClose
    public void onWebSocketClose (CloseReason reason)
    {
        System.out.println("Socket Closed: " + reason);
    }

    @OnError
    public void onWebSocketError (Throwable cause)
    {
        cause.printStackTrace(System.err);
    }
}
