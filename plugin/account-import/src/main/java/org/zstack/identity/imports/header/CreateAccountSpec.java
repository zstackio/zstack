package org.zstack.identity.imports.header;

import org.zstack.header.identity.AccountType;
import org.zstack.header.log.NoLogging;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/06/12
 */
public class CreateAccountSpec {
    private String accountUuid;
    private boolean createIfNotExist = true;

    private String credentials;
    private AccountType accountType;
    private String username;
    @NoLogging
    private String password;
    private List<String> systemTags = new ArrayList<>();

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public boolean isCreateIfNotExist() {
        return createIfNotExist;
    }

    public void setCreateIfNotExist(boolean createIfNotExist) {
        this.createIfNotExist = createIfNotExist;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(List<String> systemTags) {
        this.systemTags = systemTags;
    }
}
