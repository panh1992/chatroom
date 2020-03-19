package net.library.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public class IOArgs {

    private int limit = 256;

    private ByteBuffer buffer = ByteBuffer.allocate(limit);

    /**
     * 从byte数组中读取数据
     */
    public int readFrom(byte[] bytes, int offset, int count) {
        int size = Math.min(count, buffer.remaining());
        if (size <= 0) {
            return 0;
        }
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 写入数据到byte数组中
     */
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

    /**
     * 从channel中读取数据
     */
    public int readFrom(ReadableByteChannel readableByteChannel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int length = readableByteChannel.read(buffer);
            if (length < 0) {
                throw new EOFException();
            }
            bytesProduced += length;
        }
        return bytesProduced;
    }

    /**
     * 写入数据到channel中
     */
    public int writeTo(WritableByteChannel writableByteChannel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int length = writableByteChannel.write(buffer);
            if (length < 0) {
                throw new EOFException();
            }
            bytesProduced += length;
        }
        return bytesProduced;
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
        this.limit = Math.min(limit, buffer.capacity());
    }

    public int readLength() {
        return buffer.getInt();
    }

    public int capacity() {
        return buffer.capacity();
    }

    public boolean remained() {
        return buffer.remaining() > 0;
    }

    /**
     * 填充空数据
     */
    public int fillEmpty(int size) {
        int fillSize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + fillSize);
        return fillSize;
    }

    /**
     * 清空数据
     */
    public int setEmpty(int size) {
        int emptySize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + emptySize);
        return emptySize;
    }

    /**
     * IOArgs 提供者、处理者；数据的生产或消费者
     */
    public interface IOArgsEventProcessor {

        /**
         * 提供一份可消费的IOArgs
         */
        IOArgs provideIOArgs();

        /**
         * 消费成功时回调
         */
        void onConsumeCompleted(IOArgs args);

        /**
         * 消费异常时回调
         */
        void onConsumeFailed(IOArgs args, Exception ex);

    }

}
