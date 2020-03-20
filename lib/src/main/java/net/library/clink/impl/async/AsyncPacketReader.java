package net.library.clink.impl.async;

import lombok.extern.log4j.Log4j2;
import net.library.clink.core.Frame;
import net.library.clink.core.IOArgs;
import net.library.clink.core.SendPacket;
import net.library.clink.core.ds.BytePriorityNode;
import net.library.clink.frame.AbsSendPacketFrame;
import net.library.clink.frame.CancelSendFrame;
import net.library.clink.frame.SendEntityFrame;
import net.library.clink.frame.SendHeaderFrame;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@Log4j2
public class AsyncPacketReader implements Closeable {

    private final PacketProvider provider;

    private volatile IOArgs args;

    private volatile BytePriorityNode<Frame> node;

    private volatile int nodeSize;

    private short lastIdentifier;

    {
        this.args = new IOArgs();
        this.nodeSize = 0;
        this.lastIdentifier = 0;
    }

    AsyncPacketReader(PacketProvider provider) {
        this.provider = provider;
    }

    /**
     * 请求从 {@link #provider}队列中拿一份Packet进行发送
     *
     * @return 如果当前Reader中有可以用于网络发送的数据，则返回True
     */
    boolean requestTakePacket() {
        synchronized (this) {
            if (nodeSize >= 1) {
                return true;
            }
        }
        SendPacket<? extends InputStream> packet = provider.takePacket();
        if (Objects.nonNull(packet)) {
            SendHeaderFrame headerFrame = new SendHeaderFrame(generateIdentifier(), packet);
            appendNewFrame(headerFrame);
        }
        synchronized (this) {
            return nodeSize != 0;
        }
    }

    /**
     * 填充数据到IoArgs中
     *
     * @return 如果当前有可用于发送的帧，则填充数据并返回吗如果填充失败可返回null
     */
    IOArgs fillData() {
        Frame currentFrame = getCurrentFrame();
        if (Objects.nonNull(currentFrame)) {
            try {
                if (currentFrame.handle(args)) {
                    // 消费完本帧 尝试基于本帧构建后续帧
                    Frame nextFrame = currentFrame.nextFrame();
                    if (Objects.nonNull(nextFrame)) {
                        appendNewFrame(nextFrame);
                    } else if (currentFrame instanceof SendEntityFrame) {
                        // 末尾实体帧  通知完成
                        provider.completedPacket(((SendEntityFrame) currentFrame).getPacket(), true);
                    }
                    // 从链表头部弹出
                    popCurrentFrame();
                }
                return args;
            } catch (IOException e) {
                log.error(e);
            }
        }
        return null;
    }

    /**
     * 取消Packet对应的帧发送，如果当前Packet已发送部分数据（就算只是头数据）
     * 也应该在当前帧队列中发送一份取消发送的标志
     *
     * @param packet 带取消的Packet
     */
    synchronized void cancel(SendPacket<? extends InputStream> packet) {
        if (nodeSize == 0) {
            return;
        }
        for (BytePriorityNode<Frame> x = node, before = null; Objects.nonNull(x); before = x, x = x.next) {
            if (x.item instanceof AbsSendPacketFrame) {
                AbsSendPacketFrame absSendPacketFrame = (AbsSendPacketFrame) x.item;
                if (absSendPacketFrame.getPacket() == packet) {
                    if (absSendPacketFrame.abort()) {
                        removeFrame(x, before);
                        if (absSendPacketFrame instanceof SendHeaderFrame) {
                            // 头帧， 并且未被发送任何数据，直接取消后不需要添加取消发送帧
                            break;
                        }
                    }
                    // 添加中止帧， 通知接收方
                    CancelSendFrame cancelSendFrame = new CancelSendFrame(absSendPacketFrame.getBodyIdentifier());
                    appendNewFrame(cancelSendFrame);
                    // 意外终止，返回失败
                    provider.completedPacket(packet, false);
                    break;
                }
            }
        }
    }

    /**
     * 关闭当前Reader，关闭时应关闭所有的Frame对应的Packet
     */
    @Override
    public synchronized void close() {
        while (Objects.nonNull(node)) {
            if (node.item instanceof AbsSendPacketFrame) {
                provider.completedPacket(((AbsSendPacketFrame) node.item).getPacket(), false);
            }
            node = node.next;
        }
        nodeSize = 0;
        node = null;
    }

    private synchronized void appendNewFrame(Frame frame) {
        BytePriorityNode<Frame> newNode = new BytePriorityNode<>(frame);
        if (Objects.nonNull(node)) {
            // 使用优先级别添加到链表
            node.appendWithPriority(newNode);
        } else {
            node = newNode;
        }
        nodeSize++;
    }

    private synchronized Frame getCurrentFrame() {
        if (Objects.isNull(node)) {
            return null;
        }
        return node.item;
    }

    private synchronized void popCurrentFrame() {
        node = node.next;
        nodeSize--;
        if (Objects.isNull(node)) {
            requestTakePacket();
        }
    }

    private synchronized void removeFrame(BytePriorityNode<Frame> removeNode, BytePriorityNode<Frame> before) {
        if (Objects.isNull(before)) {
            node = removeNode.next;
        } else {
            before.next = removeNode.next;
        }
        nodeSize--;
        if (Objects.isNull(node)) {
            requestTakePacket();
        }
    }

    /**
     * 生成唯一标识
     */
    short generateIdentifier() {
        short identifier = ++lastIdentifier;
        if (identifier == 255) {
            identifier = 0;
        }
        return identifier;
    }

    /**
     * Packet提供者
     */
    public interface PacketProvider {

        /**
         * 那Packet操作
         *
         * @return 如果队列有可以发送的Packet则返回不为null
         */
        SendPacket<? extends InputStream> takePacket();

        /**
         * 结束一份Packet
         *
         * @param packet    发送包
         * @param isSucceed 是否成功发送完成
         */
        void completedPacket(SendPacket<? extends InputStream> packet, boolean isSucceed);

    }

}
