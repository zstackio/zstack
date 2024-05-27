package org.zstack.header.xinfini;

import okhttp3.MediaType;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:12 2024/5/28
 */
public interface XInfiniConstants {
    String DEFAULT_CREATOR = "AFA";
    String IDENTITY = "xinfini";

    String XINFINI_MANUFACTURER = "xinfini";
    String HEADER_TOKEN = "x-sddc-token";

    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    int DEFAULT_POLLING_INTERVAL_IN_SECOND = 1;
    int DEFAULT_POLLING_TIMES = 1800;
}
