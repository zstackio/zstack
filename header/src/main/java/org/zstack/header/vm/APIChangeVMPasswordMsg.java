package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.tag.TagResourceType;

/**
 * Created by mingjian.deng on 16/10/17.
 */
@TagResourceType(VmInstanceVO.class)
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APIChangeVMPasswordMsg extends APIMessage implements VmInstanceMessage {

    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    /**
     * regex only contains A-Z,a-z,0-9,_ and noTrim
     */
    @APIParam(noTrim = true, validRegexValues = VmInstanceConstant.USER_VM_REGEX_PASSWORD)
    private String vmAccountPassword;

    @APIParam(noTrim = true, nonempty = true)
    private String vmAccountName;


    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setVmInstanceUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVmAccountPassword() {
        return vmAccountPassword;
    }

    public void setVmAccountPassword(String vmAccountPassword) {
        this.vmAccountPassword = vmAccountPassword;
    }

    public String getVmAccountName() {
        return vmAccountName;
    }

    public void setVmAccountName(String vmAccountName) {
        this.vmAccountName = vmAccountName;
    }
}

