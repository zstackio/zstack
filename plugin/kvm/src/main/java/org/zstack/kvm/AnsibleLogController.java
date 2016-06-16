package org.zstack.kvm;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.zstack.core.ansible.AnsibleLogCmd;
import org.zstack.core.logging.Log;
import org.zstack.core.logging.LogLevel;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by xing5 on 2016/6/15.
 */
@Controller
public class AnsibleLogController {
    @RequestMapping(value = KVMConstant.KVM_ANSIBLE_LOG_PATH_FROMAT, method = {RequestMethod.PUT, RequestMethod.POST})
    public  @ResponseBody
    String log(@PathVariable String uuid, @RequestBody String body) {
        AnsibleLogCmd cmd = JSONObjectUtil.toObject(body, AnsibleLogCmd.class);
        if (cmd.getParameters() != null) {
            new Log(uuid).setLevel(LogLevel.valueOf(cmd.getLevel())).setText(cmd.getLabel(), cmd.getParameters()).write();
        } else {
            new Log(uuid).setLevel(LogLevel.valueOf(cmd.getLevel())).setText(cmd.getLabel()).write();
        }

        return null;
    }
}
