package net.library.clink.core.ds;

import java.util.Objects;

/**
 * 带优先级的节点，可用于构成链表
 */
public class BytePriorityNode<Item> {

    // 优先级
    public byte priority;

    // 当前节点
    public Item item;

    // 下一节点
    public BytePriorityNode<Item> next;

    public BytePriorityNode(Item item) {
        this.item = item;
    }

    /**
     * 按照优先级追加到当前链表中
     * @param node 节点
     */
    public void appendWithPriority(BytePriorityNode<Item> node) {
        if (Objects.isNull(next)) {
            next = node;
        } else {
            BytePriorityNode<Item> after = this.next;
            if (after.priority < node.priority) {
                // 中间位置插入
                this.next = node;
                node.next = after;
            } else {
                after.appendWithPriority(node);
            }
        }
    }

}
