package org.zstack.kvm;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.zstack.core.ansible.AnsibleLogCmd;
import org.zstack.core.logging.Log;
import org.zstack.core.logging.LogLevel;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

/**
 * Created by xing5 on 2016/6/15.
 */
@Controller
public class AnsibleLogController {
    private static final CLogger logger = Utils.getLogger(AnsibleLogController.class);

    @RequestMapping(value = KVMConstant.KVM_ANSIBLE_LOG_PATH_FROMAT, method = {RequestMethod.PUT, RequestMethod.POST})
    public  @ResponseBody
    String log(@PathVariable String uuid, @RequestBody String body) {
        AnsibleLogCmd cmd = JSONObjectUtil.toObject(body, AnsibleLogCmd.class);
        if (cmd.getParameters() != null) {
            new Log(uuid).setLevel(LogLevel.valueOf(cmd.getLevel())).log(cmd.getLabel(), cmd.getParameters());
        } else {
            new Log(uuid).setLevel(LogLevel.valueOf(cmd.getLevel())).log(cmd.getLabel());
        }

        return null;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllException(Exception ex) {
        logger.warn(ex.getMessage(), ex);
        ModelAndView model = new ModelAndView("error/generic_error");
        model.addObject("errMsg", ex.getMessage());
        return model;
    }
}
