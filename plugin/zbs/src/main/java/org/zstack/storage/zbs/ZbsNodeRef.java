package org.zstack.storage.zbs;

import org.zstack.header.errorcode.ErrorCode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ZbsNodeRef {
    private String serialNumber;
    private final Set<String> nodeAddresses = new LinkedHashSet<>();
    private ErrorCode unavailableError;

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public List<String> getNodeAddresses() {
        return new ArrayList<>(nodeAddresses);
    }

    public void addNodeAddress(String nodeAddress) {
        nodeAddresses.add(nodeAddress);
    }

    public ErrorCode getUnavailableError() {
        return unavailableError;
    }

    public void setUnavailableError(ErrorCode unavailableError) {
        this.unavailableError = unavailableError;
    }

}
