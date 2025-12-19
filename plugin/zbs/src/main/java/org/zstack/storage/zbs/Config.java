package org.zstack.storage.zbs;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Xingwei Yu
 * @date 2024/4/2 11:13
 */
public class Config {
    public static class Pool {
        public String logicalName;
        public String aliasName;

        public Pool(String logicalName, String aliasName) {
            this.logicalName = logicalName;
            this.aliasName = aliasName;
        }

        public Pool() {}
    }

    private List<String> mdsUrls;
    private List<Pool> pools;
    private String logicalPoolName;
    private transient List<String> poolNames;

    public List<String> getMdsUrls() {
        return mdsUrls;
    }

    public void setMdsUrls(List<String> mdsUrls) {
        this.mdsUrls = mdsUrls;
    }

    public String getLogicalPoolName() {
        return logicalPoolName;
    }

    public void setLogicalPoolName(String logicalPoolName) {
        this.logicalPoolName = logicalPoolName;
    }

    public void setPools(List<Pool> pools) {
        this.pools = pools;
        poolNames = getPoolNames();
    }

    public List<Pool> getPools() {
        return pools;
    }

    public List<String> getPoolNames() {
        if (poolNames == null) {
            poolNames = pools == null ? Collections.emptyList() : pools.stream().map(pool -> pool.logicalName).collect(Collectors.toList());
        }
        return poolNames;
    }
}
