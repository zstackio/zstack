package org.zstack.header.host;


import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import static java.util.Arrays.asList;


@RestRequest(
        path = "/hosts/{uuid}/resource-allocation",
        responseClass = APIGetHostResourceAllocationReply.class,
        method = HttpMethod.GET
)
public class APIGetHostResourceAllocationMsg extends APISyncCallMessage {
    @APIParam
    private String uuid;

    @APIParam
    private String strategy;

    @APIParam
    private String scene;

    @APIParam
    private String vcpu;

    @APIParam(required = false)
    private String memSize;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getVcpu() {
        return Integer.parseInt(vcpu);
    }

    public void setVcpu(String vcpu) {
        this.vcpu = vcpu;
    }

    public void setVcpu(int vcpu) {
        this.vcpu = String.valueOf(vcpu);
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getScene() {
        return scene;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setMemSize(Long memSize) {
        this.memSize = memSize.toString();
    }

    public void setMemSize(String memSize) {
        this.memSize = memSize;
    }

    public Long getMemSize() {
        if (memSize == null) {
            return 0L;
        } else {
            return Long.parseLong(memSize);
        }
    }

    public static List<String> __example__() {
        return asList("uuid="+uuid());
    }
}
