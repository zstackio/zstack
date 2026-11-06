package org.zstack.plugin.netprobe;

import org.zstack.kvm.KVMAgentCommands;

import java.util.List;

/**
 * The call to the plugin's kvmagent part: all probes due on one host, and their results in the same order.
 */
public class NetProbeCommands {
    public static class ProbeSpec {
        public String taskUuid;
        public String type;
        public String target;
        public Integer port;
        public String url;
        public int timeout;
    }

    public static class ProbeResult {
        public String taskUuid;
        public boolean success;
        public Double latencyMs;
        public Double lossPercent;
        public Integer httpStatus;
        public String error;
    }

    public static class ProbeCmd extends KVMAgentCommands.AgentCommand {
        public List<ProbeSpec> probes;
    }

    public static class ProbeRsp extends KVMAgentCommands.AgentResponse {
        public List<ProbeResult> results;
    }
}
