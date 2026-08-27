package org.zstack.storage.zbs;

import org.zstack.header.errorcode.ErrorCode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ZbsNodeRef {
    private String serverUuid;
    private String serialNumber;
    private final Set<String> primaryStorageUuids = new LinkedHashSet<>();
    private final Set<String> nodeAddresses = new LinkedHashSet<>();
    private int sourceRefCount;
    private ErrorCode unavailableError;

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public List<String> getPrimaryStorageUuids() {
        return new ArrayList<>(primaryStorageUuids);
    }

    public void addPrimaryStorageUuid(String primaryStorageUuid) {
        primaryStorageUuids.add(primaryStorageUuid);
    }

    public List<String> getNodeAddresses() {
        return new ArrayList<>(nodeAddresses);
    }

    public void addNodeAddress(String nodeAddress) {
        nodeAddresses.add(nodeAddress);
    }

    public int getSourceRefCount() {
        return sourceRefCount;
    }

    public void incrementSourceRefCount() {
        sourceRefCount++;
    }

    public ErrorCode getUnavailableError() {
        return unavailableError;
    }

    public void setUnavailableError(ErrorCode unavailableError) {
        this.unavailableError = unavailableError;
    }

    public boolean includesAnyPrimaryStorage(Collection<String> uuids) {
        for (String uuid : uuids) {
            if (primaryStorageUuids.contains(uuid)) {
                return true;
            }
        }
        return false;
    }
}
