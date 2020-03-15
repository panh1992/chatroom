package net.client.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class ServerInfo {

    private String sn;

    private int port;

    private String address;

}
