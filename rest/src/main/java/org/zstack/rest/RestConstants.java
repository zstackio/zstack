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

    String UNIT_TEST_WEBHOOK_PATH = "/rest-webhook";
}
