package org.zstack.header.image;

/**
 * Created by mingjian.deng on 2017/9/14.
 */
public interface CreateImageExtensionPoint {
    void beforeCreateImage(ImageInventory img, String bsUuid);
}
