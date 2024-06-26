package org.zstack.testlib.identity.ldap

import org.zstack.core.db.Q
import org.zstack.header.errorcode.ErrorCode
import org.zstack.ldap.driver.LdapSearchSpec
import org.zstack.ldap.driver.LdapUtil
import org.zstack.ldap.entity.LdapEntryInventory
import org.zstack.ldap.entity.LdapServerVO
import org.zstack.ldap.entity.LdapServerVO_

import java.util.function.Function
import java.util.function.Supplier

class LdapDriverForTest extends LdapUtil {
    public Supplier<List<LdapVirtualEndpointSpec>> findAllEndpointsFunction
    public Function<LdapServerVO, LdapVirtualEndpointSpec> findEndpointByLdapVOFunction

    @Override
    ErrorCode testLdapServerConnection(LdapServerVO ldap) {
        def endpoint = findEndpointByLdapVOFunction.apply(ldap)
        if (endpoint != null) {
            return endpoint.testConnectionHandler.get()
        }
        return super.testLdapServerConnection(ldap)
    }

    @Override
    List<LdapEntryInventory> searchLdapEntry(LdapSearchSpec spec) {
        def ldap = Q.New(LdapServerVO.class)
                .eq(LdapServerVO_.uuid, spec.getLdapServerUuid())
                .find() as LdapServerVO
        def endpoint = findEndpointByLdapVOFunction.apply(ldap)
        if (endpoint != null) {
            return endpoint.searchHandler.apply(spec)
        }
        return super.searchLdapEntry(spec)
    }

    @Override
    boolean isValid(String uid, String password, LdapServerVO ldap) {
        def endpoint = findEndpointByLdapVOFunction.apply(ldap)
        if (endpoint != null) {
            return endpoint.authenticateHandler.apply(uid, password)
        }
        return super.isValid(uid, password, ldap)
    }

    @Override
    String getFullUserDn(LdapServerVO ldap, String key, String val) {
        def endpoint = findEndpointByLdapVOFunction.apply(ldap)
        if (endpoint != null) {
            return endpoint.fullDnGetter.apply(key, val)
        }
        return super.getFullUserDn(ldap, key, val)
    }
}
