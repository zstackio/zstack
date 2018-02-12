package org.zstack.sdk;

public class PolicyInventory  {

    public java.util.List<PolicyStatement> statements;
    public void setStatements(java.util.List<PolicyStatement> statements) {
        this.statements = statements;
    }
    public java.util.List<PolicyStatement> getStatements() {
        return this.statements;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String accountUuid;
    public void setAccountUuid(java.lang.String accountUuid) {
        this.accountUuid = accountUuid;
    }
    public java.lang.String getAccountUuid() {
        return this.accountUuid;
    }

}
