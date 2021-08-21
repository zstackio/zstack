package org.zstack.testlib;

import org.apache.logging.log4j.ThreadContext;
import org.zstack.core.Platform;
import org.zstack.core.apicost.analyzer.service.MsgLogFinder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.Constants;
import org.zstack.header.message.*;
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor;
import org.zstack.header.rest.RESTFacade;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2017/3/24.
 */
public class ApiPathTracker {
    private CloudBus bus;
    private RESTFacade restf;

    final List<Path> paths = new ArrayList<>();

    private enum Type {
        Message,
        HttpRPC,
    }

    private static class Path {
        Type type;
        String path;
    }

    public List<String> getApiPath() {
        synchronized (paths) {
            return paths.stream().map(p -> String.format("(%s) %s", p.type, p.path)).collect(Collectors.toList());
        }
    }

    public ApiPathTracker(String apiId) {
        bus = Platform.getComponentLoader().getComponent(CloudBus.class);
        restf = Platform.getComponentLoader().getComponent(RESTFacade.class);

        bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
            @Override
            public void beforeSendMessage(Message msg) {
                if (msg instanceof CarrierMessage || msg instanceof MessageReply)
                    return;

                String id = ThreadContext.get(Constants.THREAD_CONTEXT_API);

                if (id == null || !id.equals(apiId)) {
                    return;
                }

                Path p = new Path();
                p.type = Type.Message;
                p.path = msg.getClass().getName();
                paths.add(p);
            }
        });

        bus.installBeforeSendMessageReplyInterceptor(new AbstractBeforeSendMessageReplyInterceptor() {
            @Override
            public void beforeSendMessageReply(Message msg, MessageReply reply) {
                String THREAD_CONTEXT = "thread-context";
                if (!msg.getHeaders().containsKey(THREAD_CONTEXT))
                    return;

                Map<String, String> threadContext = (Map<String, String>) msg.getHeaders().get(THREAD_CONTEXT);
                String id = threadContext.get(Constants.THREAD_CONTEXT_API);
                String taskName = threadContext.get(Constants.THREAD_CONTEXT_TASK_NAME);
                if (id == null || !id.equals(apiId))
                    return;

                // HTTP调用消息是异步消息，不计算时间
                if (msg.getMessageName().endsWith("HttpCallMsg"))
                    return;

                // 写入步骤开始的时间和应答时间：这里存储msgLog，记录apiId，msgId，msgName，startTime，replyTime, wait, status --huaxin
                long startTime = msg.getCreatedTime();
                long endTime = System.currentTimeMillis();
                BigDecimal wait = BigDecimal.valueOf((endTime - startTime) / 1000.0);

                new MsgLogFinder().save(msg.getId(), msg.getMessageName(), apiId, taskName,
                        startTime, endTime, wait, MsgLogFinder.NOT_UPDATE);
            }
        });

        restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            public void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
                String id = ThreadContext.get(Constants.THREAD_CONTEXT_API);

                if (id == null || !id.equals(apiId)) {
                    return;
                }

                Path p = new Path();
                p.type = Type.HttpRPC;
                p.path = String.format("[url:%s, cmd: %s]", url, body.getClass().getName());
                paths.add(p);
            }

            @Override
            public void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
                String id = ThreadContext.get(Constants.THREAD_CONTEXT_API);

                if (id == null || !id.equals(apiId)) {
                    return;
                }

                Path p = new Path();
                p.type = Type.HttpRPC;
                p.path = String.format("[url:%s, cmd body: %s]", url, body);
                paths.add(p);
            }
        });
    }
}
