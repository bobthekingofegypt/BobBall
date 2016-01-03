
import org.bobstuff.bobball.Network.NetworkIP;

import java.lang.Integer;
import java.lang.System;
import java.util.Timer;

public class Server {
    public static void main(String[] args) {
        int port = 0;

        if (args.length >= 1) {
            port = Integer.valueOf(args[0]);
        }

        System.out.println("Started server on port " + port);
        final NetworkIP nw = new NetworkIP((int) System.currentTimeMillis());
        if (port > 0 )
            nw.startServer(port);
        nw.clientConnect("127.0.0.1", 1235);
        while (true)
        {
            nw.sendMsg(123, "asdas".getBytes());

    }}
}
