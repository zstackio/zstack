package org.zstack.kvm;

import org.zstack.header.host.APIAddHostMsg;
import org.zstack.header.message.APIParam;
/**
 * @api
 *
 * add a kvm host
 *
 * @category kvm
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.kvm.APIAddKVMHostMsg": {
"username": "root",
"password": "password",
"name": "host1",
"managementIp": "localhost",
"clusterUuid": "0f8b6a4702a840bfaf928f04ff0a5da4",
"session": {
"uuid": "0a1b3dd187af40b5871e5ab12b0d7875"
}
}
}
 *
 * @msg
 * {
"org.zstack.kvm.APIAddKVMHostMsg": {
"username": "root",
"password": "password",
"name": "host1",
"managementIp": "localhost",
"clusterUuid": "0f8b6a4702a840bfaf928f04ff0a5da4",
"session": {
"uuid": "0a1b3dd187af40b5871e5ab12b0d7875"
},
"timeout": 1800000,
"id": "9fca21a7aedb43669a146b0a4c7b9146",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIAddHostEvent`
 */
public class APIAddKVMHostMsg extends APIAddHostMsg {
    /**
     * @desc user name used for ssh login. Must be 'root' for now.
     * Max length of 255 characters
     * @choices root
     */
    @APIParam(maxLength = 255)
    private String username;
    /**
     * @desc password for ssh login
     * Max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String password;
    
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
}
