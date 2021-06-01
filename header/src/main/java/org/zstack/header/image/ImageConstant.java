package org.zstack.header.image;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public interface ImageConstant {
    enum ImageMediaType {
        RootVolumeTemplate,
        DataVolumeTemplate,
        ISO,
        Kernel,
    }

    String ACTION_CATEGORY = "image";

    String SERVICE_ID = "image";
    @PythonClass
    String ZSTACK_IMAGE_TYPE = "zstack";

    String ISO_FORMAT_STRING = "iso";
    String QCOW2_FORMAT_STRING = "qcow2";
    String RAW_FORMAT_STRING = "raw";

    // image less than 1MB is useless
    long MINI_IMAGE_SIZE_IN_BYTE = 1048576L;
}
