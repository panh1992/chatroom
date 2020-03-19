package net.library.clink.core;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * 公共的数据封装
 * 提供了类型以及基本的长度的定义
 */
public abstract class Packet<Stream extends Closeable> implements Closeable {

    // BYTES 类型
    public static final byte TYPE_MEMORY_BYTES = 1;

    // String 类型
    public static final byte TYPE_MEMORY_STRING = 2;

    // 文件 类型
    public static final byte TYPE_STREAM_FILE = 3;

    // 长连接流 类型
    public static final byte TYPE_STREAM_DIRECT = 4;

    protected long length;

    private Stream stream;

    public long length() {
        return length;
    }

    /**
     * 类型，直接通过方法得到：
     * {@link #TYPE_MEMORY_BYTES}
     * {@link #TYPE_MEMORY_STRING}
     * {@link #TYPE_STREAM_FILE}
     * {@link #TYPE_STREAM_DIRECT}
     *
     * @return 类型
     */
    public abstract byte type();

    /**
     * 头部额外信息，用于携带额外的校验信息
     *
     * @return byte数组，最大255长度
     */
    public byte[] headerInfo() {
        return null;
    }

    /**
     * 对外获取当前实例的流操作
     */
    public final Stream open() {
        if (Objects.isNull(stream)) {
            stream = createStream();
        }
        return stream;
    }

    /**
     * 创建流操作，应当将当前需要传输的数据转化为流
     *
     * @return {@link java.io.InputStream} or {@link java.io.OutputStream}
     */
    protected abstract Stream createStream();

    /**
     * 关闭流， 当前方法会调用流的关闭操作
     *
     * @param stream 待关闭的流
     * @throws IOException 抛出异常
     */
    protected void closeStream(Stream stream) throws IOException {
        stream.close();
    }

    /**
     * 对外的关闭资源操作
     */
    @Override
    public final void close() throws IOException {
        if (Objects.nonNull(stream)) {
            closeStream(stream);
            stream = null;
        }
    }

}
