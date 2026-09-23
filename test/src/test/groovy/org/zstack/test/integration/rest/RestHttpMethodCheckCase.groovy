package org.zstack.test.integration.rest

import org.zstack.core.db.Q
import org.zstack.header.tag.UserTagVO
import org.zstack.header.tag.UserTagVO_
import org.zstack.rest.RestGlobalConfig
import org.zstack.sdk.UserTagInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.WebBeanConstructor
import org.zstack.utils.gson.JSONObjectUtil

class RestHttpMethodCheckCase extends SubCase {
    EnvSpec env
    ZoneInventory zone

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "http-method-check"
            }
        }
    }

    @Override
    void test() {
        env.create {
            zone = env.inventoryByName("http-method-check")
            assert RestGlobalConfig.CHECK_HTTP_METHOD.value(Boolean.class)
            try {
                String protectedTag = createTag("protected")
                assert request("GET", "/v1/tags/${protectedTag}").status == 405
                assert Q.New(UserTagVO.class).eq(UserTagVO_.uuid, protectedTag).isExists()

                Map deletion = request("DELETE", "/v1/tags/${protectedTag}")
                assert deletion.status == 202
                awaitSuccess(deletion.body.location as String)
                assert !Q.New(UserTagVO.class).eq(UserTagVO_.uuid, protectedTag).isExists()

                updateCheck(false)
                String legacyTag = createTag("legacy")
                Map legacyDeletion = request("GET", "/v1/tags/${legacyTag}")
                assert legacyDeletion.status == 202
                awaitSuccess(legacyDeletion.body.location as String)
                assert !Q.New(UserTagVO.class).eq(UserTagVO_.uuid, legacyTag).isExists()

                updateCheck(true)
                String protectedAgain = createTag("protected-again")
                assert request("GET", "/v1/tags/${protectedAgain}").status == 405
                assert Q.New(UserTagVO.class).eq(UserTagVO_.uuid, protectedAgain).isExists()
                assert request("GET", "/v1/zones/${zone.uuid}").status == 200
            } finally {
                updateCheck(true)
            }
        }
    }

    private String createTag(String text) {
        UserTagInventory inventory = createUserTag {
            resourceUuid = zone.uuid
            resourceType = "ZoneVO"
            tag = "http-method-${text}"
        }
        return inventory.uuid
    }

    private void updateCheck(boolean enabled) {
        updateGlobalConfig {
            category = RestGlobalConfig.CATEGORY
            name = "checkHttpMethod"
            value = enabled.toString()
        }
    }

    private void awaitSuccess(String location) {
        retryInSecs {
            Map result = request("GET", location)
            assert result.status == 200
            assert result.body.error == null
        }
    }

    private Map request(String method, String path) {
        String url = path.startsWith("http") ? path : "http://127.0.0.1:${WebBeanConstructor.port}${path}"
        HttpURLConnection connection = new URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Authorization", "OAuth ${adminSession()}")
            connection.setRequestProperty("Accept", "application/json")
            int status = connection.responseCode
            String text = (status >= 400 ? connection.errorStream : connection.inputStream)?.getText("UTF-8") ?: "{}"
            return [status: status, body: JSONObjectUtil.toObject(text, LinkedHashMap.class)]
        } finally {
            connection.disconnect()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
