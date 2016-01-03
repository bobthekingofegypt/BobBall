package org.bobstuff.bobball.Network;

//import android.util.Log;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;

public class NetworkIP extends NetworkDispatcher {

    public static final int BOBBALL_SRV_PORT = 8477;

    private ServerSocket srvSock;

    public NetworkIP(int uniqueID) {
        super(uniqueID);
    }

    public void startServer() {
        startServer(BOBBALL_SRV_PORT);
    }

    public void startServer(final int port) {
        if (this.srvSock != null)
            throw new IllegalArgumentException();

        this.threadpool.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    srvSock = new ServerSocket(port);

                } catch (IOException e) {
                    //Log.d("xx", "IOException ---");
                    e.printStackTrace();
                    return;
                }

                while (true) {
                    try {
                        Socket s = srvSock.accept();
                        addConnection(s.getInputStream(), s.getOutputStream());
                    } catch (IOException e) {
                        //Log.d("xx", "IOException --zz-");
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    public void clientConnect(final String dstName, final int dstPort) {
        this.threadpool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket cliSock = new Socket(dstName, dstPort);
                    addConnection(cliSock.getInputStream(), cliSock.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                    //Log.d("xx", "IOException --cli-");
                }
            }
        });
    }

}
