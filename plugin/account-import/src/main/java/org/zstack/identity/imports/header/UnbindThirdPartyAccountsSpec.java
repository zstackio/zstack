package org.zstack.identity.imports.header;

import java.util.ArrayList;
import java.util.List;

public class UnbindThirdPartyAccountsSpec {
    private String sourceUuid;
    private String sourceType;
    private List<UnbindThirdPartyAccountSpecItem> items = new ArrayList<>();

    public String getSourceUuid() {
        return sourceUuid;
    }

    public void setSourceUuid(String sourceUuid) {
        this.sourceUuid = sourceUuid;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public List<UnbindThirdPartyAccountSpecItem> getItems() {
        return items;
    }

    public void setItems(List<UnbindThirdPartyAccountSpecItem> items) {
        this.items = items;
    }
}
