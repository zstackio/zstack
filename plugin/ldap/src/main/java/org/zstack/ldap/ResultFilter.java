package org.zstack.ldap;

public abstract class ResultFilter{
    public abstract boolean needSelect(String dn);
}
