package org.zstack.test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.zstack.rest.RestConstants;
import org.zstack.sdk.ZSClient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by xing5 on 2017/1/2.
 */
@Controller
public class SdkWebHookController {
    @RequestMapping(
            value = RestConstants.UNIT_TEST_WEBHOOK_PATH,
            method = {RequestMethod.POST}
    )
    public void api(HttpServletRequest request, HttpServletResponse response) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ZSClient.webHookCallback(request, response);
    }
}
