package org.zstack.kvm;

import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

import java.util.Set;

/**
 * Created by xing5 on 2016/3/14.
 */
public class APIKvmRunShellMsg extends APIMessage {
    @APIParam(resourceType = HostVO.class, nonempty = true)
    private Set<String> hostUuids;
    @APIParam
    private String script;

    public Set<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(Set<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
}
