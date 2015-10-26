package org.bobstuff.bobball;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.util.Timer;

public class Network {
    private ServerSocket srvSock;
    private Socket cliSock;

    public static  final int  BOBBALL_SRV_PORT =8477;
    public static  final int  BOBBALL_CLI_PORT =8477;
    public static  final String  BOBBALL_CLI_HOST ="127.0.0.1";

    public Socket serverListenBlocking() {

        try {
            if (srvSock == null) {
                srvSock = new ServerSocket(BOBBALL_SRV_PORT);
            }

            Socket s = srvSock.accept();
            return  s;

        } catch (IOException e) {
            Log.d("xx", "IOException ---");
            e.printStackTrace();
        }
        return null;
    }

    public Socket clientListenBlocking() {
        while (true){
        try {
            cliSock = new Socket(BOBBALL_CLI_HOST,BOBBALL_CLI_PORT);
            return  cliSock;

        } catch (IOException e) {
            e.printStackTrace();
        }
        }
    }




}
