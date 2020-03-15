package net.library.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class IOArgs {

    private int limit = 256;

    private byte[] byteBuffer = new byte[256];

    private ByteBuffer buffer = ByteBuffer.wrap(byteBuffer);

    /**
     * 从bytes中读取数据
     */
    public int readFrom(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 写入数据到bytes中
     */
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

    /**
     * 从socketChannel读取数据
     */
    public int readFrom(SocketChannel socketChannel) throws IOException {
        startWriting();
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int length = socketChannel.read(buffer);
            if (length < 0) {
                throw new EOFException();
            }
            bytesProduced += length;
        }
        finishWriting();
        return bytesProduced;
    }

    /**
     * 写数据到socketChannel
     */
    public int writeTo(SocketChannel socketChannel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int length = socketChannel.write(buffer);
            if (length < 0) {
                throw new EOFException();
            }
            bytesProduced += length;
        }
        return bytesProduced;
    }

    /**
     * 开始写入数据到IOArgs
     */
    public void startWriting() {
        buffer.clear();
        // 定义容纳区间
        buffer.limit(limit);
    }

    /**
     * 写完数据后调用
     */
    public void finishWriting() {
        buffer.flip();
    }

    /**
     * 设置单次写操作的容纳区间
     *
     * @param limit 区间大小
     */
    public void limit(int limit) {
        this.limit = limit;
    }

    public void writeLength(int total) {
        buffer.putInt(total);
    }

    public int readLength() {
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public interface IOArgsEventListener {

        void onStarted(IOArgs args);

        void onCompleted(IOArgs args);

    }

}
