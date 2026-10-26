package org.zstack.kvm.memory;

import org.zstack.header.search.Inventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.sql.Timestamp;
import java.util.*;

/** Public Host memory state; no savings, which come from GetMemorySummary. */
@Inventory(mappingVOClass = HostMemoryStateVO.class)
public class HostMemoryStateInventory {
    private String hostUuid;
    private String status;
    private String featureState;
    private String reason;
    private Map<String, Object> capabilities;
    private List<String> allowedActions;
    private Map<String, String> uncertainRecovery;
    private String activeJobUuid;
    private Long lastObservedAt;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static HostMemoryStateInventory valueOf(HostMemoryStateVO vo) {
        HostMemoryStateInventory inventory = new HostMemoryStateInventory();
        inventory.hostUuid = vo.getHostUuid();
        inventory.status = vo.getStatus();
        inventory.featureState = vo.getFeatureState();
        inventory.reason = vo.getReason();
        inventory.capabilities = vo.getCapabilities() == null ? null : JSONObjectUtil.toObject(vo.getCapabilities(), LinkedHashMap.class);
        inventory.allowedActions = vo.getAllowedActions() == null ? Collections.emptyList()
                : JSONObjectUtil.toObject(vo.getAllowedActions(), ArrayList.class);
        inventory.uncertainRecovery = vo.getUncertainRecovery() == null ? null
                : JSONObjectUtil.toObject(vo.getUncertainRecovery(), LinkedHashMap.class);
        inventory.activeJobUuid = vo.getActiveJobUuid();
        inventory.lastObservedAt = vo.getLastObservedAt();
        inventory.createDate = vo.getCreateDate();
        inventory.lastOpDate = vo.getLastOpDate();
        return inventory;
    }

    public static List<HostMemoryStateInventory> valueOf(Collection<HostMemoryStateVO> vos) {
        List<HostMemoryStateInventory> inventories = new ArrayList<>(vos.size());
        for (HostMemoryStateVO vo : vos) { inventories.add(valueOf(vo)); }
        return inventories;
    }

    public static HostMemoryStateInventory __example__() {
        HostMemoryStateInventory inventory = new HostMemoryStateInventory();
        inventory.setHostUuid("1234567890abcdef1234567890abcdef");
        inventory.setStatus("Applied");
        inventory.setFeatureState("Active");
        inventory.setCapabilities(Collections.singletonMap("zram", true));
        inventory.setAllowedActions(Arrays.asList("query", "pause", "drain"));
        inventory.setLastObservedAt(1791583200000L);
        return inventory;
    }

    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getFeatureState() { return featureState; }
    public void setFeatureState(String value) { featureState = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public Map<String, Object> getCapabilities() { return capabilities; }
    public void setCapabilities(Map<String, Object> value) { capabilities = value; }
    public List<String> getAllowedActions() { return allowedActions; }
    public void setAllowedActions(List<String> value) { allowedActions = value; }
    public Map<String, String> getUncertainRecovery() { return uncertainRecovery; }
    public void setUncertainRecovery(Map<String, String> value) { uncertainRecovery = value; }
    public String getActiveJobUuid() { return activeJobUuid; }
    public void setActiveJobUuid(String value) { activeJobUuid = value; }
    public Long getLastObservedAt() { return lastObservedAt; }
    public void setLastObservedAt(Long value) { lastObservedAt = value; }
    public Timestamp getCreateDate() { return createDate; }
    public void setCreateDate(Timestamp value) { createDate = value; }
    public Timestamp getLastOpDate() { return lastOpDate; }
    public void setLastOpDate(Timestamp value) { lastOpDate = value; }
}
