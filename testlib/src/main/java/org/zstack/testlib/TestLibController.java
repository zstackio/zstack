package org.zstack.testlib;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * Created by xing5 on 2017/2/12.
 */
@Controller
public class TestLibController {
    @RequestMapping(
            value = "/**",
            method = {
                    RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.GET,
                    RequestMethod.HEAD, RequestMethod.OPTIONS, RequestMethod.PATCH, RequestMethod.TRACE
            }
    )
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (request.getMethod().equalsIgnoreCase(RequestMethod.HEAD.toString())) {
            response.setStatus(200);
            return;
        }

        Test.handleHttp(request, response);
    }
}
