package org.zstack.ldap.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.ldap.entity.LdapServerVO;

/**
 * Created by Wenhao.Zhang on 2024/06/04
 */
@RestRequest(
        path = "/ldap/servers/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APISyncAccountsFromLdapServerEvent.class
)
public class APISyncAccountsFromLdapServerMsg extends APIMessage {
    @APIParam(resourceType = LdapServerVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APISyncAccountsFromLdapServerMsg __example__() {
        APISyncAccountsFromLdapServerMsg msg = new APISyncAccountsFromLdapServerMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
