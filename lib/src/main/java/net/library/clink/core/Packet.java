package net.library.clink.core;

import lombok.Getter;

import java.io.Closeable;

/**
 * 公共的数据封装
 * 提供了类型以及基本的长度的定义
 */
@Getter
public abstract class Packet implements Closeable {

    protected byte type;

    protected int length;

}
