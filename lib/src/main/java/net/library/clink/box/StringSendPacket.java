package net.library.clink.box;

import net.library.clink.core.SendPacket;

public class StringSendPacket extends SendPacket {

    private final byte[] bytes;

    public StringSendPacket(String msg) {
        this.bytes = msg.getBytes();
        this.length = this.bytes.length;
    }

    @Override
    public byte[] bytes() {
        return this.bytes;
    }

    @Override
    public void close() {
    }

}
