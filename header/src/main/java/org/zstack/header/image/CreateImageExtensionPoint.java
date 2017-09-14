package org.zstack.header.image;

/**
 * Created by mingjian.deng on 2017/9/14.
 */
public interface CreateImageExtensionPoint {
    void afterCreateImage(ImageInventory img, String bsUuid);
}
