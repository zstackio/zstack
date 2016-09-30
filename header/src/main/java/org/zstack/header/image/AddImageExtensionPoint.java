package org.zstack.header.image;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by frank on 9/16/2015.
 */
public interface AddImageExtensionPoint {
    void preAddImage(ImageInventory img);

    void beforeAddImage(ImageInventory img);

    void afterAddImage(ImageInventory img);

    void failedToAddImage(ImageInventory img, ErrorCode err);
}
