package org.zstack.rest;

/**
 * Created by xing5 on 2016/12/7.
 */
public interface RestConstants {
    String API_VERSION = "/v1";
    String ASYNC_JOB_PATH = "/api-jobs";
    String ALL_PATH = "/v1/**";

    String HEADER_JSON_SCHEMA = "X-JSON-Schema";
    String HEADER_WEBHOOK = "X-Web-Hook";
    String HEADER_JOB_UUID = "X-Job-UUID";
    String HEADER_JOB_SUCCESS = "X-Job-Success";
    String HEADER_OAUTH = "OAuth";
    String HEADER_ACCESSKEY = "ZStack";
    String HEADER_Date = "Date";
    String HEADER_CONTENT_MD5 = "Content-MD5";
    String HEADER_CONTENT_Type = "Content-Type";

    String ACCESS_KEY_ALGORITHM = "HmacSHA1";

    /* request must be received in 15 minutes */
    int REQUEST_DURATION_MINUTES = 15;

    String UNIT_TEST_WEBHOOK_PATH = "/rest-webhook";
}
