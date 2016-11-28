package org.zstack.test.core.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping(value = RESTBeanForTest.ROOT)
public class RESTBeanForTest {
    private static final CLogger logger = Utils.getLogger(RESTBeanForTest.class);

    public static final String ROOT = "/restbeanfortest";
    public static final String CALLBACK_PATH = "/callback";
    public static final String CALLBACK_TIMEOUT_PATH = "/callbacktimeout";
    public static final String CALLBACK_FAIL_PATH = "/callbackfail";
    public static final String CALLBACK_MISSING_TASKUUID_PATH = "/callbackmissingtaskuuid";
    public static final String CALLBACK_JSON_PATH = "/callbackjson";

    @Autowired
    private RESTFacade restf;

    @AsyncThread
    private void callItBack(HttpEntity<String> req) throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        String taskUuid = req.getHeaders().getFirst(RESTConstant.TASK_UUID);
        String callbackUrl = req.getHeaders().getFirst(RESTConstant.CALLBACK_URL);
        logger.debug(String.format("Get taskuuid:%s, callback url: %s", taskUuid, callbackUrl));

        String body = req.getBody();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(body.length());
        headers.set(RESTConstant.TASK_UUID, taskUuid);
        HttpEntity<String> rreq = new HttpEntity<String>(body, headers);
        ResponseEntity<String> rsp = restf.getRESTTemplate().exchange(callbackUrl, HttpMethod.POST, rreq, String.class);
        logger.debug(String.format("called %s, status: %s, body: %s", callbackUrl, rsp.getStatusCode(), rsp.getBody()));
    }

    @RequestMapping(value = RESTBeanForTest.CALLBACK_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String testCallback(HttpServletRequest req, HttpServletResponse rsp) throws InterruptedException, IOException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        callItBack(entity);
        return "";
    }

    @RequestMapping(value = RESTBeanForTest.CALLBACK_JSON_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String testCallbackJson(HttpServletRequest req, HttpServletResponse rsp) throws InterruptedException, IOException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        callItBack(entity);
        return "";
    }

    @RequestMapping(value = RESTBeanForTest.CALLBACK_TIMEOUT_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String testCallbackTimeout(HttpServletRequest req, HttpServletResponse rsp) throws InterruptedException, IOException {
        /* do nothing, emulate a timeout */
        return "";
    }

    @RequestMapping(value = RESTBeanForTest.CALLBACK_FAIL_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String testCallbackFail(HttpServletRequest req, HttpServletResponse rsp) throws InterruptedException, IOException {
        /* do nothing, emulate a timeout */
        throw new CloudRuntimeException("Fail on purpose");
    }

    @AsyncThread
    private void callItBackWithoutTaskUuid(HttpEntity<String> req) throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        String taskUuid = req.getHeaders().getFirst(RESTConstant.TASK_UUID);
        String callbackUrl = req.getHeaders().getFirst(RESTConstant.CALLBACK_URL);
        logger.debug(String.format("Get taskuuid:%s, callback url: %s", taskUuid, callbackUrl));

        String body = req.getBody();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentLength(body.length());
        /* no taskUuid in header*/
        HttpEntity<String> rreq = new HttpEntity<String>(body, headers);
        ResponseEntity<String> rsp = restf.getRESTTemplate().exchange(callbackUrl, HttpMethod.POST, rreq, String.class);
        logger.debug(String.format("called %s, status: %s, body: %s", callbackUrl, rsp.getStatusCode(), rsp.getBody()));
    }

    @RequestMapping(value = RESTBeanForTest.CALLBACK_MISSING_TASKUUID_PATH, method = RequestMethod.POST)
    public
    @ResponseBody
    String testCallbackMissingTaskUuid(HttpServletRequest req, HttpServletResponse rsp) throws InterruptedException, IOException {
        HttpEntity<String> entity = restf.httpServletRequestToHttpEntity(req);
        callItBackWithoutTaskUuid(entity);
        return "";
    }
}
