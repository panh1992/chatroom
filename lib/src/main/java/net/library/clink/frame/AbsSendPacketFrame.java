package net.library.clink.frame;

import net.library.clink.core.Frame;
import net.library.clink.core.IOArgs;
import net.library.clink.core.SendPacket;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public abstract class AbsSendPacketFrame extends AbsSendFrame {

    protected volatile SendPacket<? extends InputStream> packet;

    public AbsSendPacketFrame(int length, byte type, byte flag, short identifier,
                              SendPacket<? extends InputStream> packet) {
        super(length, type, flag, identifier);
        this.packet = packet;
    }

    public synchronized SendPacket<? extends InputStream> getPacket() {
        return packet;
    }

    @Override
    public synchronized boolean handle(IOArgs args) throws IOException {
        if (Objects.isNull(packet) && !isSending()) {
            // 已取消，并且未发送任何数据，直接返回结束，发送下一帧
            return true;
        }
        return super.handle(args);
    }

    @Override
    public final synchronized Frame nextFrame() {
        return Objects.isNull(packet) ? null : buildNextFrame();
    }

    /**
     * 终止当前帧
     * 需要在当前方法中做一些操作，以及状态的维护
     * 后续可以扩展{@link #fillDirtyDataOnAbort()}方法对数据进行填充操作
     *
     * @return True：完美终止，可以顺利的移除当前帧；False：已发送部分数据
     */
    public final synchronized boolean abort() {
        boolean isSending = isSending();
        if (isSending) {
            fillDirtyDataOnAbort();
        }
        packet = null;
        return !isSending;
    }

    protected abstract Frame buildNextFrame();

    protected void fillDirtyDataOnAbort() {
    }

}
