package com.mackenziehigh.socius.io;

import java.io.IOException;
import java.net.ServerSocket;

/**
 *
 */
@FunctionalInterface
public interface ServerSocketFactory
{
    public ServerSocket newServerSocket ()
            throws IOException;
}
