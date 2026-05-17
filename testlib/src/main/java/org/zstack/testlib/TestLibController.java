package org.zstack.testlib;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.zstack.testlib.util.TestConfigUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.annotation.PreDestroy;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2017/2/12.
 */
@Controller
public class TestLibController {
    private static final CLogger logger = Utils.getLogger(TestLibController.class);

    private static final ExecutorService pool = Executors.newFixedThreadPool(32);

    @RequestMapping(
            value = {"/**", "/v1/sites/**", "/v1/quota/**"},
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

        if (isMultipartRequest(request)) {
            Test.handleHttp(request, response);
            return;
        }

        final AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(TestConfigUtils.getMessageTimeoutMillisConfig());

        pool.submit(() -> {
            try {
                Test.handleHttp((HttpServletRequest) asyncContext.getRequest(),
                        (HttpServletResponse) asyncContext.getResponse());
            } catch (Throwable t) {
                logger.error(t.getMessage(), t);
                try {
                    ((HttpServletResponse) asyncContext.getResponse()).sendError(500);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            } finally {
                asyncContext.complete();
            }
        });
    }

    private boolean isMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }

    @PreDestroy
    public void shutdownPool() {
        logger.info("Shutting down TestLibController pool");
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Pool did not terminate within timeout, forcing shutdown");
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
