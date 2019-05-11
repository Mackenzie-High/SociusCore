package com.mackenziehigh.socius.io.tcp;

import com.mackenziehigh.cascade.Cascade;
import com.mackenziehigh.socius.flow.Processor;
import com.mackenziehigh.socius.io.stdio.Printer;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class Main01
{

    public static void main (String[] args)
            throws IOException
    {
        final Cascade.Stage stage = Cascade.newStage();

        final ServerSocket sock = new ServerSocket();
        sock.bind(new InetSocketAddress("127.0.0.1", 8080));
        final TcpServerSocket server = TcpServerSocket.newTcpServer(stage, sock);

        final Processor<Socket> procConn = Processor.fromConsumerScript(stage, msg -> onConnection(stage, msg));

        server.socketsOut().connect(procConn.dataIn());

        server.start();

        System.in.read();
    }

    private static void onConnection (final Cascade.Stage stage,
                                      final Socket sock)
    {
        final TcpSocket<String> conn = TcpSocket.newTcpSocket(stage, sock);

        final Processor<String> echo = Processor.fromFunctionScript(stage, msg -> "S = " + msg + "\n");
        final Printer<String> printer = Printer.newPrintln(stage);
        conn.dataOut().connect(printer.dataIn());
        conn.dataOut().connect(echo.dataIn());
        conn.dataIn().connect(echo.dataOut());

        conn.start(new StringReader(), new StringWriter());
    }

    private static final class StringReader
            implements TcpSocket.SocketReader<String>
    {
        private volatile DataInputStream in;

        @Override
        public void setSocket (Socket socket)
                throws IOException
        {
            in = new DataInputStream(socket.getInputStream());
        }

        @Override
        public String onRead ()
                throws IOException
        {
            final String line = in.readLine();
            return line;
        }

        @Override
        public void close ()
                throws IOException
        {
            // Pass.
        }

        @Override
        public boolean isComplete ()
        {
            return false;
        }
    }

    private static final class StringWriter
            implements TcpSocket.SocketWriter<String>
    {
        private volatile DataOutputStream out;

        @Override
        public void setSocket (Socket socket)
                throws IOException
        {
            out = new DataOutputStream(socket.getOutputStream());
        }

        @Override
        public void onWrite (String message)
                throws IOException
        {
            out.writeChars(message);
        }

        @Override
        public boolean isComplete ()
        {
            return false;
        }

        @Override
        public void close ()
                throws IOException
        {
            // Pass.
        }
    }
}
