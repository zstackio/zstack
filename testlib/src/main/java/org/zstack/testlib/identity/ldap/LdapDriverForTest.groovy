package org.zstack.testlib.identity.ldap

import org.zstack.header.errorcode.ErrorCode
import org.zstack.ldap.driver.LdapUtil
import org.zstack.ldap.entity.LdapServerVO

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
}
