package org.zstack.kvm.memory;

import java.util.ArrayList;
import java.util.List;

/** Structured, client-stable warning returned by policy preview. */
public class MemoryPreviewWarningInventory {
    public static MemoryPreviewWarningInventory __example__() {
        MemoryPreviewWarningInventory inventory = new MemoryPreviewWarningInventory();
        inventory.setCode("HOST_REVALIDATED_BEFORE_APPLY");
        inventory.setMessage(MemoryPreviewWarnings.legacyMessage(inventory.getCode()));
        inventory.setMessageKey("memory.preview.warning.hostRevalidatedBeforeApply");
        inventory.setFormatArgs(java.util.Collections.emptyList());
        return inventory;
    }

    private String code;
    private String message;
    private String messageKey;
    private List<String> formatArgs = new ArrayList<>();

    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public String getMessage() { return message; }
    public void setMessage(String value) { message = value; }
    public String getMessageKey() { return messageKey; }
    public void setMessageKey(String value) { messageKey = value; }
    public List<String> getFormatArgs() { return formatArgs; }
    public void setFormatArgs(List<String> value) { formatArgs = value == null ? new ArrayList<>() : value; }
}
