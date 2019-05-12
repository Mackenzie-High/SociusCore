package com.mackenziehigh.socius;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 *
 */
public final class Messenger
{
    public interface MessageInput
    {
        public boolean isBegin ();

        public boolean isHeartbeat ();

        public boolean isData ();

        public boolean isEnd ();

        public int size ();

        public int read (byte[] buffer);

        public int read (byte[] buffer,
                         int offset,
                         int length);

        public DataInputStream openDataInputStream ();

        public void closeConnection ();
    }

    public interface MessageOutput
    {
        public int capacity ();

        public int write (byte[] buffer);

        public int write (byte[] buffer,
                          int offset,
                          int length);

        public DataOutputStream openDataOutputStream ();

        public void closeConnection ();
    }
}
