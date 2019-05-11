package com.mackenziehigh.socius.util;

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
