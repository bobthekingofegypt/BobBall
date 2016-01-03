package org.bobstuff.bobball.Network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public abstract class NetworkDispatcher {

    private int uid;
    private List<Connection> cons;
    protected Executor threadpool;

    private NetworkMsgHandler handler;

    class Connection {
        public Connection(DataInputStream in, DataOutputStream out) {
            this.in = in;
            this.out = out;
        }

        DataInputStream in;
        DataOutputStream out;
    }

    public NetworkDispatcher(int uniqueID) {
        this.uid = uniqueID;
        this.cons = new ArrayList<>();
        this.threadpool = Executors.newCachedThreadPool();
    }

    public synchronized void sendMsg(int destID, byte[] payload) {
        NetworkMessage m = new NetworkMessage(destID, uid, payload);
        for (Connection c : cons) {
            DataOutputStream o = c.out;
            try {
                m.toOutputStream(o);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void setMsgHandler(NetworkMsgHandler mh, long destID) {
        if (this.handler != null)
                throw new IllegalArgumentException();
        this.handler=mh;
    }


    public synchronized void addConnection(InputStream in, OutputStream out) {

        DataInputStream din = null;
        DataOutputStream dout = null;

        if (in != null) {
            din = new DataInputStream(in);
        }
        if (out != null) {
            dout = new DataOutputStream(out);
        }
        Connection con = new Connection(din, dout);
        this.cons.add(con);

        if (in != null) { //start a listener for incoming messages
            this.threadpool.execute(new StreamMsgListener(din, con, cons));
        }

    }


    class StreamMsgListener implements Runnable {
        private DataInputStream in;
        private Connection mycon;
        private List<Connection> cons;

        public StreamMsgListener(DataInputStream in, Connection mycon, List<Connection> cons) {
            this.in = in;
            this.mycon = mycon;
            this.cons = cons;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    NetworkMessage m = new NetworkMessage(this.in);

                    System.out.println("Rcv msg " + m +"  on connection " + mycon);
                    // flood fill
                    for (Connection c: cons)
                    {
                        if (c != mycon) // forward message to all _other_ connections
                        {
                            DataOutputStream o = c.out;
                            try {
                                m.toOutputStream(o);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                    // call NetworkMsgHandler
                    handler.handleMsg(m);


                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

}




