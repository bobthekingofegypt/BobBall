package org.bobstuff.bobball;


import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class NetworkActor extends Actor {
    protected static final String TAG = "NetworkActor";
    private InputStream in;
    private OutputStream out;

    public NetworkActor(GameManager gameManager, int[] playerIds, Socket s) {
        super(gameManager, playerIds);

        try {
            this.in = s.getInputStream();
            this.out = s.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void newEventCallback(GameEvent ev) {
        super.newEventCallback(ev);

        Parcel p = Parcel.obtain();
        p.writeParcelable(ev, 0);
        try {
            out.write(p.marshall());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        if (playerIds.length == 0)
            return;

        ClassLoader classLoader = getClass().getClassLoader();

        while (true) {
            try {
                byte[] buf = new byte[100];
                Parcel p = Parcel.obtain();
                int len = in.read(buf);
                p.unmarshall(buf, 0, len);
                p.setDataPosition(0);
                GameEvent ev = p.readParcelable(classLoader);
                Log.d(TAG, "Received ev: " + ev);
                p.recycle();

            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }

    @Override
    public void reset() {

    }
}
