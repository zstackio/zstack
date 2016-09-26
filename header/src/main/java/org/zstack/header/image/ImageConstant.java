package org.zstack.header.image;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface ImageConstant {
    enum ImageMediaType {
        RootVolumeTemplate,
        DataVolumeTemplate,
        ISO,
    }

    String ACTION_CATEGORY = "image";

    String SERVICE_ID = "image";
    @PythonClass
    String ZSTACK_IMAGE_TYPE = "zstack";

    String ISO_FORMAT_STRING = "iso";

    String QUOTA_IMAGE_NUM = "image.num";
    String QUOTA_IMAGE_SIZE = "image.size";
}
