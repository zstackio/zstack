package org.zstack.header.vm;


import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by luchukun on 8/4/16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmSshKeyEvent.class
)
public class APISetVmSshKeyMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam
    private String SshKey;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setSshKey(String SshKey) {
        this.SshKey = SshKey;
    }

    public String getSshKey() {
        return SshKey;
    }

 
    public static APISetVmSshKeyMsg __example__() {
        APISetVmSshKeyMsg msg = new APISetVmSshKeyMsg();
        msg.uuid = uuid();
        msg.SshKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCtgdrSt2i/4ayXoiR6qNd7dykOOcz205NVSUgw41GOZW3PdXa9WWMF1APHtj8L2tYm3lKgIDSy1aQtpIEenu6L03BeqfPwepf2L89aZ/W4UPRpce9/bO4mSSJ6kvbZ7hFv+4KLUJCB9O7UrcFu7J/QwrkHNVNX1NsVUpqebp3Ny8bxj0Wbr9ecqPKTclzygARRGz71iDQaEhLrQqy/Q8vr+/G1/uyAYfTnifCuuMTfh5DEsuKD1AEHMBVZEbJ4zupR4gywXnGRxHmEwE464scACxeFWVx2flIXkTK8f3W0KBLCJ8VtTd8KxvKQBu2jJ70avmXNOzb5IaBDS root@172-20-12-46";
        return msg;
    }
}