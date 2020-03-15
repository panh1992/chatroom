package net.library.clink.box;

import net.library.clink.core.ReceivePacket;

public class StringReceivePacket extends ReceivePacket {

    private byte[] buffer;

    private int position;

    public StringReceivePacket(int length) {
        this.buffer = new byte[length];
        this.length = length;
    }

    @Override
    public void save(byte[] bytes, int count) {
        System.arraycopy(bytes, 0, buffer, position, count);
        position += count;
    }

    public String string() {
        return new String(buffer);
    }

    @Override
    public void close() {

    }
}
