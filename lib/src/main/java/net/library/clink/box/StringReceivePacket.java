package net.library.clink.box;

import net.library.clink.core.ReceivePacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class StringReceivePacket extends ReceivePacket<ByteArrayOutputStream> {

    private String string;

    public StringReceivePacket(int length) {
        this.length = length;
    }

    public String string() {
        return string;
    }

    @Override
    protected void closeStream(ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        super.closeStream(byteArrayOutputStream);
        string = new String(byteArrayOutputStream.toByteArray());
    }

    @Override
    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) this.length);
    }

}
