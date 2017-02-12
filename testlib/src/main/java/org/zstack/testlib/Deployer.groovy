package org.zstack.testlib

import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.header.Constants
import org.zstack.header.rest.RESTConstant
import org.zstack.sdk.ZSClient
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by xing5 on 2017/2/12.
 */

class Deployer {
    static final CLogger logger = Utils.getLogger(Deployer.class)

    EnvSpec envSpec = new EnvSpec()
    SpringSpec springSpec = new SpringSpec()

    private static Map<String, Closure> httpHandlers = [:]
    private static Map<String, Closure> httpPostHandlers = [:]
    private static RestTemplate restTemplate
    private BeanConstructor beanConstructor
    ComponentLoader componentLoader

    static {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory()
        factory.setReadTimeout(CoreGlobalProperty.REST_FACADE_READ_TIMEOUT)
        factory.setConnectTimeout(CoreGlobalProperty.REST_FACADE_CONNECT_TIMEOUT)
        restTemplate = new RestTemplate(factory)
    }

    Deployer() {
    }

    void buildBeanConstructor(boolean useWeb = true) {
        beanConstructor = useWeb ? new WebBeanConstructor() : new BeanConstructor()
        if (springSpec.all) {
            beanConstructor.loadAll = true
        } else {
            springSpec.xmls.each { beanConstructor.addXml(it) }
        }

        componentLoader = beanConstructor.build()
    }

    private static void replyHttpCall(HttpEntity<String> entity, HttpServletResponse response, Object rsp) {
        String taskUuid = entity.getHeaders().getFirst(RESTConstant.TASK_UUID)
        if (taskUuid == null) {
            response.status = HttpStatus.OK.value()
            response.writer.write(rsp == null ? "" : JSONObjectUtil.toJsonString(rsp))
            return
        }

        String callbackUrl = entity.getHeaders().getFirst(RESTConstant.CALLBACK_URL)
        String rspBody = rsp == null ? "" : JSONObjectUtil.toJsonString(rsp)
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        headers.setContentLength(rspBody.length())
        headers.set(RESTConstant.TASK_UUID, taskUuid)
        HttpEntity<String> rreq = new HttpEntity<String>(rspBody, headers)
        restTemplate.exchange(callbackUrl, HttpMethod.POST, rreq, String.class)
    }

    static void simulator(String path, Closure c) {
        httpHandlers[path] = c
    }

    static void afterSimulator(String path, Closure c) {
        httpPostHandlers[path] = c
    }

    void handleSimulatorHttpRequests(HttpServletRequest req, HttpServletResponse rsp) {
        def url = req.getRequestURI()
        if (WebBeanConstructor.WEB_HOOK_PATH.toString().contains(url)) {
            ZSClient.webHookCallback(req, rsp)
            return
        }

        def handler = httpHandlers[url]
        if (handler == null) {
            rsp.sendError(HttpStatus.NOT_FOUND.value(), "not handler found for the path $url")
            return
        }

        StringBuilder sb = new StringBuilder()
        String line
        while ((line = req.getReader().readLine()) != null) {
            sb.append(line)
        }
        req.getReader().close()

        HttpHeaders header = new HttpHeaders()
        for (Enumeration e = req.getHeaderNames() ; e.hasMoreElements() ;) {
            String name = e.nextElement().toString()
            header.add(name, req.getHeader(name))
        }

        def entity = new HttpEntity<String>(sb.toString(), header)
        try {
            def ret
            if (handler.maximumNumberOfParameters == 0) {
                ret = handler()
            } else if (handler.maximumNumberOfParameters == 1) {
                ret = handler(entity)
            } else {
                ret = handler(entity, envSpec)
            }

            Closure postHandler = httpPostHandlers[url]
            if (postHandler != null) {
                if (postHandler.maximumNumberOfParameters <= 1) {
                    ret = postHandler(ret)
                } else if (handler.maximumNumberOfParameters == 2) {
                    ret = postHandler(ret, entity)
                } else {
                    ret = postHandler(ret, entity, envSpec)
                }
            }

            replyHttpCall(entity, rsp, ret)
        } catch (HttpError he) {
            logger.warn("the simulator[$url] reports a http error[status code:${he.status}, message:${he.message}]", he)
            rsp.sendError(he.status, he.message)
        } catch (Throwable t) {
            logger.warn("error happened when handlign $url", t)
            rsp.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), t.message)
        }
    }
}
