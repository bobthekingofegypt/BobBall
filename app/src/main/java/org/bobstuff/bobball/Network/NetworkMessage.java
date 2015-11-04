package org.bobstuff.bobball.Network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

  /* defines the protocol on the wire */

public class NetworkMessage {
    final int MAX_PACKET_SIZE = 2048;
    final int HEADERLEN = 16;

    private ByteBuffer data;
    private int len;

    /*construct a new NetworkMessage*/
    public NetworkMessage(int destID, int senderID, byte[] payload) {
        len = payload.length + HEADERLEN;
        byte[] tmp = new byte[len];
        data = ByteBuffer.wrap(tmp);
        data.putInt(len);
        data.putInt(destID);
        data.putInt(senderID);
        data.putInt(0);
        assert data.position() == HEADERLEN;
        data.put(payload);
    }

    /*construct a new NetworkMessage from a byte buffer that contains the data of exactly one NetworkMessage*/
    public NetworkMessage(byte[] data) {
        this.data = ByteBuffer.wrap(data);
    }

    /*construct a new NetworkMessage from a Stream reading exactly the required amount of bytes*/
    public NetworkMessage(DataInputStream in) throws IOException {
        len = in.readInt();
        if ((len <= HEADERLEN) || (len > MAX_PACKET_SIZE))
            throw new IOException();
        data = ByteBuffer.allocate(len);
        data.putInt(len);
        in.readFully(data.array(), Integer.SIZE, len - Integer.SIZE);
    }

    public void toOutputStream(DataOutputStream out) throws IOException {
        out.write(data.array());
    }

    public int getLen() {
        return len;
    }

    public int getPayloadLen() {
        return len - HEADERLEN;
    }

    public byte[] getPayload() {
        byte[] payload = new byte[getPayloadLen()];
        data.get(payload);
        return payload;
    }


}
