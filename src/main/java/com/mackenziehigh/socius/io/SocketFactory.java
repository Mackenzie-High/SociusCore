package com.mackenziehigh.socius.io;

import java.io.IOException;
import java.net.Socket;

/**
 *
 */
@FunctionalInterface
public interface SocketFactory
{
    public Socket newSocket ()
            throws IOException;
}
