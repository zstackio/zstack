package org.zstack.identity.imports.header;

import org.zstack.header.identity.AccountType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/06/12
 */
public class ImportAccountItem {
    private String accountUuid;

    private String credentials;
    private AccountType accountType;
    private String username;
    private List<String> systemTags = new ArrayList<>();

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<String> systemTags) {
        this.systemTags = systemTags;
    }
}
