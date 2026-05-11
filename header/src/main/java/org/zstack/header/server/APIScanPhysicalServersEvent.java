package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

@RestResponse(fieldsTo = "all")
public class APIScanPhysicalServersEvent extends APIEvent {
    private int discoveredCount;
    private int existingCount;
    private int unreachableCount;
    private int authFailedCount;
    private List<PhysicalServerInventory> discoveredServers;
    private List<String> authFailedIps;

    public APIScanPhysicalServersEvent() {
        super(null);
    }

    public APIScanPhysicalServersEvent(String apiId) {
        super(apiId);
    }

    public int getDiscoveredCount() {
        return discoveredCount;
    }

    public void setDiscoveredCount(int discoveredCount) {
        this.discoveredCount = discoveredCount;
    }

    public int getExistingCount() {
        return existingCount;
    }

    public void setExistingCount(int existingCount) {
        this.existingCount = existingCount;
    }

    public int getUnreachableCount() {
        return unreachableCount;
    }

    public void setUnreachableCount(int unreachableCount) {
        this.unreachableCount = unreachableCount;
    }

    public int getAuthFailedCount() {
        return authFailedCount;
    }

    public void setAuthFailedCount(int authFailedCount) {
        this.authFailedCount = authFailedCount;
    }

    public List<PhysicalServerInventory> getDiscoveredServers() {
        return discoveredServers;
    }

    public void setDiscoveredServers(List<PhysicalServerInventory> discoveredServers) {
        this.discoveredServers = discoveredServers;
    }

    public List<String> getAuthFailedIps() {
        return authFailedIps;
    }

    public void setAuthFailedIps(List<String> authFailedIps) {
        this.authFailedIps = authFailedIps;
    }

    public static APIScanPhysicalServersEvent __example__() {
        APIScanPhysicalServersEvent event = new APIScanPhysicalServersEvent();
        PhysicalServerInventory inv = new PhysicalServerInventory();
        inv.setUuid(uuid());
        inv.setName("server1");
        inv.setZoneUuid(uuid());
        inv.setPoolUuid(uuid());
        inv.setManagementIp("192.168.1.100");
        inv.setArchitecture("x86_64");
        inv.setState("Enabled");
        inv.setPowerStatus("POWER_ON");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setDiscoveredCount(1);
        event.setExistingCount(0);
        event.setUnreachableCount(0);
        event.setAuthFailedCount(0);
        event.setDiscoveredServers(java.util.Arrays.asList(inv));
        event.setAuthFailedIps(java.util.Collections.emptyList());
        return event;
    }
}
