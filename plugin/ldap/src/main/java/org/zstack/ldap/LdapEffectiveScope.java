package org.zstack.ldap;

import org.zstack.header.host.HypervisorType;

import java.util.*;

public class LdapEffectiveScope {
    private static Map<String, LdapEffectiveScope> scopes = Collections.synchronizedMap(new HashMap<String, LdapEffectiveScope>());
    private final String scopeName;
    private boolean exposed = true;

    public LdapEffectiveScope(String scopeName) {
        this.scopeName = scopeName;
        scopes.put(scopeName, this);
    }

    public LdapEffectiveScope(String scopeName, boolean exposed) {
        this(scopeName);
        this.exposed = exposed;
    }

    public static boolean hasScope(String scope) {
        return scopes.keySet().contains(scope);
    }

    public static LdapEffectiveScope valueOf(String scopeName) {
        LdapEffectiveScope scope = scopes.get(scopeName);
        if (scope == null) {
            throw new IllegalArgumentException("Ldap effective scope: " + scopeName + " was not registered");
        }
        return scope;
    }

    public boolean isExposed() {
        return exposed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }

    @Override
    public String toString() {
        return scopeName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof HypervisorType)) {
            return false;
        }

        HypervisorType scope = (HypervisorType) t;
        return scope.toString().equals(scopeName);
    }

    @Override
    public int hashCode() {
        return scopeName.hashCode();
    }

    public static Set<String> getAllTypeNames() {
        HashSet<String> exposedTypes = new HashSet<String>();
        for (LdapEffectiveScope scope : scopes.values()) {
            if (scope.exposed) {
                exposedTypes.add(scope.toString());
            }
        }
        return exposedTypes;
    }
}
