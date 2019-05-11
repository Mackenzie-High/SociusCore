package com.mackenziehigh.socius.util;

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
