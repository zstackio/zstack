package org.zstack.kvm.memory;

import org.zstack.header.message.NeedReplyMessage;

/** Internal durable-delivery request; sent to the management node owning the Host. */
public class MemoryTaskFailureNotificationMsg extends NeedReplyMessage {
    private MemoryTaskFailureEvent event;
    public MemoryTaskFailureEvent getEvent() { return event; }
    public void setEvent(MemoryTaskFailureEvent value) { event = value; }
}
