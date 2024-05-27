package org.zstack.xinfini;

import org.zstack.header.log.NoLogging;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @example
 * {"pools":[{"id": 1, "name":"pool", "aliasName":"test"}].
 *  "token": "sddc-xxxxx",
 *  "nodes":[{"ip": 10.0.0.1, "port":"80", "status":"active"}]}
 */
public class XInfiniConfig {
    @NoLogging
    private String token;
    private List<Pool> pools;
    private List<Node> nodes;

    public static class Pool {
        private int id;
        private String name;
        private String aliasName;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAliasName() {
            return aliasName;
        }

        public void setAliasName(String aliasName) {
            this.aliasName = aliasName;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    public static class Node {
        private String ip;
        private int port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public void setPools(List<Pool> pools) {
        this.pools = pools;
    }

    public List<Pool> getPools() {
        return pools;
    }

    public Set<String> getPoolNames() {
        return pools == null ? Collections.emptySet() : pools.stream().map(Pool::getName).collect(Collectors.toSet());
    }

    public Set<Integer> getPoolIds() {
        return pools == null ? Collections.emptySet() : pools.stream().map(Pool::getId).collect(Collectors.toSet());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
