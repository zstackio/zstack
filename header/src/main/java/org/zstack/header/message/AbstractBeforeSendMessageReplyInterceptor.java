package org.zstack.header.message;

/**
 * Created by lining on 2021/07/04.
 */
public abstract class AbstractBeforeSendMessageReplyInterceptor implements BeforeSendMessageReplyInterceptor{
    public int orderOfBeforeSendMessageReplyInterceptor() {
        return 0;
    }
}