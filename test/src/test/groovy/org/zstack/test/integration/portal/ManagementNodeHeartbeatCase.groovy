package org.zstack.test.integration.portal

import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.managementnode.ManagementNodeState
import org.zstack.header.managementnode.ManagementNodeVO
import org.zstack.header.managementnode.ManagementNodeVO_
import org.zstack.portal.managementnode.ManagementNodeGlobalConfig
import org.zstack.portal.managementnode.PortalGlobalProperty
import org.zstack.testlib.SubCase

import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ManagementNodeHeartbeatCase extends SubCase {

    DatabaseFacade dbf

    @Override
    void clean() {
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)

        testUnexpectedManagementNodeRecord()
    }

    void prepareInvalidRecords() {
        def now = LocalDateTime.now()
        def data = [
                '127.0.0.111' : Timestamp.valueOf(now),
                '127.0.0.222' : Timestamp.valueOf(now.plusMinutes(2))
            ]


        data.each { it ->
            ManagementNodeVO vo = new ManagementNodeVO()
            vo.setHostName(it.key)
            vo.setHeartBeat(it.value)
            vo.setUuid(Platform.uuid)
            vo.setPort(8080)
            vo.setState(ManagementNodeState.RUNNING)
            dbf.persist(vo)
        }
    }

    void testUnexpectedManagementNodeRecord() {
        ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.updateValue(1)
        PortalGlobalProperty.MAX_HEARTBEAT_FAILURE = 2

        prepareInvalidRecords()
        int heartbeatFailureTimeout = ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Integer.class) * PortalGlobalProperty.MAX_HEARTBEAT_FAILURE
        int heartbeatUpdateDelay = 1 * ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Integer.class)

        int waitBeforeClean = heartbeatFailureTimeout + heartbeatUpdateDelay
        int failureInterval = ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Integer.class)

        // wait a interval before all nodes failed
        sleep(TimeUnit.SECONDS.toMillis(waitBeforeClean - failureInterval))
        long count = dbf.count(ManagementNodeVO.class)
        assert count == 2

        // confirm 127.0.0.222 is cleaned at first
        count = Q.New(ManagementNodeVO.class)
                .notEq(ManagementNodeVO_.hostName, '127.0.0.222')
                .count()
        assert count == 2

        // wait one more interval to wait 127.0.0.111 cleaned
        sleep(TimeUnit.SECONDS.toMillis(failureInterval * 3))
        count = dbf.count(ManagementNodeVO.class)
        assert count == 1

        // confirm 127.0.0.111 is cleaned
        count = Q.New(ManagementNodeVO.class)
                .notEq(ManagementNodeVO_.hostName, '127.0.0.111')
                .count()
        assert count == 1

        PortalGlobalProperty.MAX_HEARTBEAT_FAILURE = 5
    }
}
