package net.foo.constant;

public class UDPConstant {

    // 公用头部
    public static final byte[] HEADER = new byte[]{7, 7, 7, 7, 7, 7, 7, 7};

    // 服务器固化UDP接收端口
    public static final int PORT_SERVER = 30201;

    // 客户端回送端口
    public static final int PORT_CLIENT_RESPONSE = 30202;

    // UDP广播地址
    public static final String BROADCAST_HOST = "192.168.199.255";

}
