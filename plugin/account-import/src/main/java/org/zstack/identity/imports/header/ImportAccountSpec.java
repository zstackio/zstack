package org.zstack.identity.imports.header;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/05/31
 */
public class ImportAccountSpec {
    private String sourceUuid;
    private String sourceType;
    public List<CreateAccountSpec> accountList = new ArrayList<>();

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

    public List<CreateAccountSpec> getAccountList() {
        return accountList;
    }

    public void setAccountList(List<CreateAccountSpec> accountList) {
        this.accountList = accountList;
    }
}
