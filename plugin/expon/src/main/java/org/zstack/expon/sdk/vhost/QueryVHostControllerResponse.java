package org.zstack.expon.sdk.vhost;

import org.zstack.expon.sdk.ExponQueryResponse;

import java.util.List;

public class QueryVHostControllerResponse extends ExponQueryResponse {
    private List<VHostControllerModule> vhosts;

    public List<VHostControllerModule> getVhosts() {
        return vhosts;
    }

    public void setVhosts(List<VHostControllerModule> vhosts) {
        this.vhosts = vhosts;
    }
}
