package org.zstack.kvm;

public interface VmEventAlarmHandlerExtensionPoint {
    void handleVmEventAlarm(KVMAgentCommands.VmEventAlarmCmd cmd);
}
