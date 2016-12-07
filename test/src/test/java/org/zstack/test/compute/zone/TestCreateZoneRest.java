package org.zstack.test.compute.zone;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.rest.RESTFacade;
import org.zstack.rest.AsyncRestQueryResult;
import org.zstack.rest.RestConstants;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class TestCreateZoneRest {
    CLogger logger = Utils.getLogger(TestCreateZoneRest.class);

    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    RESTFacade restf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml")
                .addXml("AccountManager.xml").addXml("rest.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        restf = loader.getComponent(RESTFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        api.loginAsAdmin();

        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(restf.getBaseUrl());
        ub.path(RestConstants.API_VERSION);
        ub.path("/zones");

        RestTemplate tmp = restf.getRESTTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", String.format("OAuth %s", api.getAdminSession().getUuid()));
        headers.set(RestConstants.HEADER_JSON_SCHEMA, "true");

        Map m = map(
                e("zone", map(
                        e("name", "zone1"),
                        e("description", "test")
                ))
        );

        try {
            HttpEntity<String> req = new HttpEntity<>(JSONObjectUtil.toJsonString(m), headers);
            ResponseEntity rsp = tmp.exchange(ub.build().toString(), HttpMethod.POST, req, String.class);
            Map map = JSONObjectUtil.toObject((String) rsp.getBody(), LinkedHashMap.class);
            String url = (String) map.get("location");

            while (true) {
                logger.debug(String.format("xxxxxxxxxxxxxxxxxx %s", url));
                ResponseEntity<String> r = tmp.exchange(url, HttpMethod.GET, new HttpEntity<>("", headers), String.class);
                AsyncRestQueryResult ret = JSONObjectUtil.toObject(r.getBody(), AsyncRestQueryResult.class);
                if (r.getStatusCode() == HttpStatus.OK) {
                    logger.debug(JSONObjectUtil.toJsonString(ret.getResult()));
                    break;
                }

                TimeUnit.SECONDS.sleep(1);
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            TimeUnit.SECONDS.sleep(3);
        }
    }
}
