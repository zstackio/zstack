package org.zstack.sdk;

import okhttp3.MediaType;

/**
 * Created by xing5 on 2016/12/10.
 */
interface Constants {
    String SESSION_ID = "sessionId";
    String HEADER_AUTHORIZATION = "Authorization";
    String OAUTH = "OAuth";
    String LOCATION = "location";

    MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    String HTTP_ERROR = "sdk.1000";
    String POLLING_TIMEOUT_ERROR = "sdk.1001";
    String INTERNAL_ERROR = "sdk.1002";

    String HEADER_JSON_SCHEMA = "X-JSON-Schema";
    String HEADER_JOB_UUID = "X-Job-UUID";
    String HEADER_WEBHOOK = "X-Web-Hook";
    String HEADER_JOB_SUCCESS = "X-Job-Success";
}
