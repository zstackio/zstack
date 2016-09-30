package org.zstack.header.console;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConsoleInventory {
    private String scheme;
    private String hostname;
    private int port;
    private String token;

    public static ConsoleInventory valueOf(ConsoleProxyVO vo) {
        ConsoleInventory inv = new ConsoleInventory();
        inv.setToken(vo.getToken());
        inv.setHostname(vo.getProxyHostname());
        inv.setPort(vo.getProxyPort());
        inv.setScheme(vo.getScheme());
        return inv;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
