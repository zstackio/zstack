package org.zstack.kvm.memory;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.message.MessageReply;
import java.util.Map;

/** Internal VM-owner routing; all writes share the VM lifecycle queue. */
public class MemoryVmConfigurationMsg extends NeedReplyMessage {
    public enum Action { READ, RECONCILE, APPLY_KSM }
    private String vmUuid;
    private Action action;
    private Boolean ksmEnabled;
    public String getVmUuid() { return vmUuid; }
    public void setVmUuid(String value) { vmUuid = value; }
    public Action getAction() { return action; }
    public void setAction(Action value) { action = value; }
    public Boolean getKsmEnabled() { return ksmEnabled; }
    public void setKsmEnabled(Boolean value) { ksmEnabled = value; }

    public static class Reply extends MessageReply {
        private Map<String, Object> state;
        public Map<String, Object> getState() { return state; }
        public void setState(Map<String, Object> value) { state = value; }
    }
}
