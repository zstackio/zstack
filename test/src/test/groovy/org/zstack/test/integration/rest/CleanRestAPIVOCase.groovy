package org.zstack.test.integration.rest

import groovy.json.JsonSlurper
import org.zstack.core.Platform
import org.zstack.core.config.GlobalConfigFacade
import org.zstack.core.rest.RESTApiFacadeImpl
import org.zstack.core.rest.RESTApiGlobalProperty
import org.zstack.core.timeout.ApiTimeoutManager
import org.zstack.header.identity.SessionInventory
import org.zstack.header.identity.SessionVO
import org.zstack.header.rest.RestAPIState
import org.zstack.header.rest.RestAPIVO
import org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotMsg
import org.zstack.header.zone.APICreateZoneEvent
import org.zstack.header.zone.APICreateZoneMsg
import org.zstack.header.zone.APIQueryZoneMsg
import org.zstack.header.zone.APIQueryZoneReply
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.Query
import java.sql.Timestamp
import java.util.concurrent.TimeUnit;

class CleanRestAPIVOCase extends SubCase{
    EnvSpec env
    RESTApiFacadeImpl restApiImpl
    EntityManagerFactory entityManagerFactory
    EntityManager mgr
    EntityTransaction tran

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
        spring {
            include("RESTFacade.xml")
        }
    }

    @Override
    void environment() {
        env = env {
        }
    }

    @Override
    void test() {
        env.create{
            restApiImpl = bean(RESTApiFacadeImpl.class)
            entityManagerFactory = restApiImpl.entityManagerFactory
            mgr = entityManagerFactory.createEntityManager()
            tran = mgr.getTransaction()

            testCleanRestAPIVO()
            testNotCleanRestAPIVO()
            deleteRestAPIVO()
            testLegacyAsyncTimeout()
        }
    }

    void testLegacyAsyncTimeout() {
        GlobalConfigFacade gcf = bean(GlobalConfigFacade.class)
        ApiTimeoutManager timeoutManager = bean(ApiTimeoutManager.class)
        String configName = APICreateZoneMsg.class.name
        String originalTimeout = gcf.getConfigValue("apiTimeout", configName, String.class)
        SessionInventory session = SessionInventory.valueOf(dbFindByUuid(adminSession(), SessionVO.class))
        List<String> zoneUuids = []
        List<String> resultUuids = []
        JsonSlurper json = new JsonSlurper()

        try {
            assert timeoutManager.getMessageTimeout(new APICreateDataVolumeFromVolumeSnapshotMsg()) == 10800000L :
                    "snapshot-to-volume default must be three hours in milliseconds"
            updateGlobalConfig {
                category = "apiTimeout"
                name = configName
                value = "45m"
            }

            [-1L, 7200000L].each { long requestedTimeout ->
                String zoneUuid = Platform.getUuid()
                zoneUuids.add(zoneUuid)
                APICreateZoneMsg msg = new APICreateZoneMsg(
                        name: "legacy-timeout-${requestedTimeout}".toString(), resourceUuid: zoneUuid,
                        session: session, timeout: requestedTimeout)
                def initial = restApiImpl.send(msg)
                resultUuids.add(initial.uuid)
                Map initialJson = json.parseText(JSONObjectUtil.toJsonString(initial)) as Map
                assert initialJson.state == "Processing"
                assert initialJson.uuid == msg.id

                def completed
                retryInSecs {
                    completed = restApiImpl.getResult(initial.uuid)
                    assert completed?.state == "Done" : "real API event must complete the legacy result row"
                }
                Map result = json.parseText(completed.result) as Map
                Map event = result[APICreateZoneEvent.class.name] as Map
                assert event.success : "CreateZone must complete successfully: ${completed.result}"
                assert event.inventory.uuid == zoneUuid
                assert completed.uuid == initial.uuid
                assert !((Map) json.parseText(JSONObjectUtil.toJsonString(completed))).containsKey("apiTimeout") :
                        "polling response must retain its existing shape"

                long expectedTimeout = requestedTimeout == -1L ? 2700000L : requestedTimeout
                assert initialJson.apiTimeout instanceof Number && initialJson.apiTimeout == expectedTimeout :
                        "legacy Processing response must advertise ${expectedTimeout} ms: ${initialJson}"
            }

            APIQueryZoneMsg query = new APIQueryZoneMsg(session: session, conditions: [])
            def response = restApiImpl.call(query)
            Map responseJson = json.parseText(JSONObjectUtil.toJsonString(response)) as Map
            assert responseJson.state == "Done"
            assert !responseJson.containsKey("apiTimeout") : "sync response must omit the optional async timeout"
            Map reply = (json.parseText(response.result) as Map)[APIQueryZoneReply.class.name] as Map
            assert reply?.success : "sync query must succeed: ${response.result}"
            assert reply.inventories*.uuid.containsAll(zoneUuids) : "sync query must see both real created zones"
        } finally {
            updateGlobalConfig {
                category = "apiTimeout"
                name = configName
                value = originalTimeout
            }
            zoneUuids.each { String zoneUuid ->
                if (!queryZone { conditions = ["uuid=${zoneUuid}".toString()] }.isEmpty()) {
                    deleteZone { uuid = zoneUuid }
                }
            }
            EntityManager cleanupManager = entityManagerFactory.createEntityManager()
            try {
                cleanupManager.transaction.begin()
                if (!resultUuids.isEmpty()) {
                    int removed = cleanupManager.createQuery("delete from RestAPIVO where uuid in (:uuids)")
                            .setParameter("uuids", resultUuids).executeUpdate()
                    assert removed == resultUuids.size() : "remove only this regression's legacy result rows"
                }
                cleanupManager.transaction.commit()
            } finally {
                if (cleanupManager.transaction.active) {
                    cleanupManager.transaction.rollback()
                }
                cleanupManager.close()
            }
        }
    }

    void testCleanRestAPIVO() {
        for (int i = 0; i < 1200; i++){
            RestAPIVO vo = new RestAPIVO()
            vo.setUuid(String.valueOf(i))
            vo.setApiMessageName("org.zstack.core.rest.DeleteRestAPpiVOMsg")
            vo.setState(RestAPIState.Processing)
            vo.setLastOpDate(new Timestamp(System.currentTimeMillis() - (1000 * 60 * 60 * 24)))
            tran.begin()
            mgr.persist(vo)
            mgr.flush()
            mgr.refresh(vo)
            tran.commit()
        }

        for (int i = 1200; i < 1400; i++){
            RestAPIVO vo = new RestAPIVO()
            vo.setUuid(String.valueOf(i))
            vo.setApiMessageName("org.zstack.core.rest.DeleteRestAPpiVOMsg")
            vo.setState(RestAPIState.Processing)
            vo.setLastOpDate(new Timestamp(System.currentTimeMillis()))
            tran.begin()
            mgr.persist(vo)
            mgr.flush()
            mgr.refresh(vo)
            tran.commit()
        }

        RESTApiGlobalProperty.CLEAN_RESTAPIVO_DELAY = 1
        RESTApiGlobalProperty.RESTAPIVO_RETENTION_DAY = 1
        restApiImpl.refreshIntervalClean()

        retryInSecs {
            tran.begin()
            String sql = "select count(*) from RestAPIVO"
            Query query = mgr.createQuery(sql)
            List result = query.resultList
            tran.commit()
            assert result.get(0) == 200
        }
    }

    void testNotCleanRestAPIVO() {
        RESTApiGlobalProperty.RESTAPIVO_RETENTION_DAY = -1
        restApiImpl.refreshIntervalClean()
        TimeUnit.SECONDS.sleep(3)
        retryInSecs {
            tran.begin()
            String sql = "select count(*) from RestAPIVO"
            Query query = mgr.createQuery(sql)
            List result = query.resultList
            tran.commit()
            assert result.get(0) == 200
        }
    }

    void deleteRestAPIVO() {
            tran.begin()
            String sql = "delete from RestAPIVO"
            Query query = mgr.createQuery(sql)
            int ret = query.executeUpdate()
            tran.commit()
            assert ret == 200
        mgr.close()
    }
}
