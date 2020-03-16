package net.library.clink.box;

/**
 * 字符串发送包
 */
public class StringSendPacket extends BytesSendPacket {

    /**
     * 字符串发送时就是Byte数据，得到Byte数组，并按照Byte的发送方式发送
     *
     * @param msg 字符串
     */
    public StringSendPacket(String msg) {
        super(msg.getBytes());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }

}
