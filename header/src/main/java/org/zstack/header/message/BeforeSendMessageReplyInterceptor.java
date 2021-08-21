package org.zstack.header.message;

/**
 * Created by lining on 2021/07/04.
 */
public interface BeforeSendMessageReplyInterceptor {
    int orderOfBeforeSendMessageReplyInterceptor();

    void beforeSendMessageReply(Message msg, MessageReply reply);
}