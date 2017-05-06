package org.zstack.header.vm;


import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
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
        msg.setUuid(uuid());
        msg.setSshKey("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCaaV5YUvz9nx54+pvxIe5L5uQFHFQsvpwRdVRfMObIgWgcliB9vl4hMCPHXfaKqJD79jBpwJWpUBPebKF7vgevWqFJeUgR/LBHTfOnRrEjVsSzanaGGzfjbrwMHdZ5YJVhDTE376+OuXz1Wu5M1mwcarJpcanmqNgyz8YhYjc50xKDusDVvtpLKxdC6WvhR0+7gaDJKkukip1Up8doOUeNUe2cObJfMoOgi2lNrtKorGp1O7Nv+mdTflboYizgQOCFReiW/1ipPjX06OMZZ3Tsx3ZwBib5ocDpLV9CjONvnDBygWb30wydVoUSp1hKIzlWPkfyWHjxCf9pvLcHGUXZ root@10-0-98-199");
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("set ssh key").resource(uuid, VmInstanceVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}