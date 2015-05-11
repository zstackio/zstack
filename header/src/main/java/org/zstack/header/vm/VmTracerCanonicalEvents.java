package org.zstack.header.vm;

import org.zstack.header.message.NeedJsonSchema;

/**
 */
public interface VmTracerCanonicalEvents {
    public static final String HOST_CHANGED_PATH = "/vmTracer/hostChanged";
    public static final String VM_STATE_CHANGED_PATH = "/vmTracer/vmStateChanged";
    public static final String STRANGER_VM_FOUND_PATH = "/vmTracer/strangerVmFound";

    @NeedJsonSchema
    public static class StrangerVmFoundData {
        private String vmIdentity;
        private String hostUuid;
        private String vmState;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getVmIdentity() {
            return vmIdentity;
        }

        public void setVmIdentity(String vmIdentity) {
            this.vmIdentity = vmIdentity;
        }

        public VmInstanceState getVmState() {
            if (vmState != null) {
                return VmInstanceState.valueOf(vmState);
            }
            return null;
        }

        public void setVmState(VmInstanceState vmState) {
            if (vmState != null) {
                this.vmState = vmState.toString();
            }
        }
    }

    @NeedJsonSchema
    public static class VmStateChangedData {
        private String vmUuid;
        private String from;
        private String to;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public VmInstanceState getFrom() {
            if (from != null) {
                return VmInstanceState.valueOf(from);
            }
            return null;
        }

        public void setFrom(VmInstanceState from) {
            if (from != null) {
                this.from = from.toString();
            }
        }

        public VmInstanceState getTo() {
            if (to != null) {
                return VmInstanceState.valueOf(to);
            }
            return null;
        }

        public void setTo(VmInstanceState to) {
            if (to != null) {
                this.to = to.toString();
            }
        }
    }

    @NeedJsonSchema
    public static class HostChangedData {
        private String vmUuid;
        private String from;
        private String to;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }
    }
}
