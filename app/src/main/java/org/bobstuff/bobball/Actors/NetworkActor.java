package org.bobstuff.bobball.Actors;


import android.os.Parcel;
import android.util.Log;

import org.bobstuff.bobball.GameLogic.GameEvent;
import org.bobstuff.bobball.GameLogic.GameManager;
import org.bobstuff.bobball.Network.NetworkMsgHandler;
import org.bobstuff.bobball.Network.NetworkMessage;
import org.bobstuff.bobball.Network.NetworkDispatcher;


public class NetworkActor extends Actor implements NetworkMsgHandler {
    protected static final String TAG = "NetworkActor";
    private NetworkDispatcher nw;

    public NetworkActor(GameManager gameManager, int[] playerIds, NetworkDispatcher nw) {
        super(gameManager, playerIds);
        this.nw = nw;
        nw.setMsgHandler(this, 0);
    }

    @Override
    public void newEventCallback(GameEvent ev) {
        super.newEventCallback(ev);

        Parcel p = Parcel.obtain();
        p.writeParcelable(ev, 0);
        nw.sendMsg(0, p.marshall());
    }

    @Override
    public void handleMsg(NetworkMessage m) {
        if (playerIds.length == 0)
            return;

        ClassLoader classLoader = getClass().getClassLoader();
        Parcel p = Parcel.obtain();
        byte[] payload = m.getPayload();
        p.unmarshall(payload, 0, payload.length);
        p.setDataPosition(0);
        GameEvent ev = p.readParcelable(classLoader);
        Log.d(TAG, "Received ev: " + ev);
        p.recycle();

        gameManager.addEvent(ev);

    }

    @Override
    public void run() {


    }

    @Override
    public void reset() {

    }
}
