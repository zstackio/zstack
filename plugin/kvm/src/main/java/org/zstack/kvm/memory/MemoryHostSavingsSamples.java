package org.zstack.kvm.memory;

import java.util.Collections;
import java.util.Map;

/** Host savings samples of one monitoring query; a reason names an answer only partly obtained. */
public class MemoryHostSavingsSamples {
    private final Map<String, Map<String, Object>> hosts;
    private final String reason;

    public MemoryHostSavingsSamples(Map<String, Map<String, Object>> hosts, String reason) {
        this.hosts = hosts == null ? Collections.emptyMap() : hosts;
        this.reason = reason;
    }

    public Map<String, Map<String, Object>> getHosts() { return hosts; }
    public String getReason() { return reason; }
}
