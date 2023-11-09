package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * @author shanshan.ning
 * @date 2023-09-11
 */
public class TakeVmConsoleScreenshotReply extends MessageReply {
    private String imageData;

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }
}
