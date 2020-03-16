package net.library.clink.box;

import java.io.ByteArrayOutputStream;

/**
 * 字符串接收包
 */
public class StringReceivePacket extends AbsByteArrayReceivePacket<String> {

    public StringReceivePacket(long length) {
        super(length);
    }

    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return new String(stream.toByteArray());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }

}
