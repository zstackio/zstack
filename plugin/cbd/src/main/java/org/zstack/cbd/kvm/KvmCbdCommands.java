package org.zstack.cbd.kvm;

import org.zstack.kvm.KVMAgentCommands;

import java.util.List;
import java.util.Map;

/**
 * @author Xingwei Yu
 * @date 2024/4/10 15:40
 */
public class KvmCbdCommands {
    public static final String SETUP_CBD_SELF_FENCER_PATH = "/ha/cbd/setupselffencer";
    public static final String CANCEL_CBD_SELF_FENCER_PATH = "/ha/cbd/cancelselffencer";
    public static final String CBD_CHECK_VMSTATE_PATH = "/cbd/check/vmstate";

    public static class AgentRsp {
        public boolean success = true;
        public String error;
    }

    public static class KvmSetupSelfFencerCmd extends AgentCmd {
        public long interval;
        public int maxAttempts;
        public Map<String, String> heartbeatPathByCoveringPaths;
        public int storageCheckerTimeout;
        public String hostUuid;
        public Integer hostId;
        public Long heartbeatRequiredSpace;
        public String strategy;
        public List<String> fencers;
    }

    public static class KvmCancelSelfFencerCmd extends AgentCmd {
    }

    public static class AgentCmd extends KVMAgentCommands.AgentCommand {
        public String uuid;
    }
}
