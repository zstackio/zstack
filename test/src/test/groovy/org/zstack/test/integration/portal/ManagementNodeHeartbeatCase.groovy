package org.zstack.test.integration.portal

import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.managementnode.ManagementNodeState
import org.zstack.header.managementnode.ManagementNodeVO
import org.zstack.portal.managementnode.ManagementNodeGlobalConfig
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
                '127.0.0.111' : Timestamp.valueOf(now.minusMinutes(2)),
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

        prepareInvalidRecords()
        TimeUnit.SECONDS.sleep(6)
        long count = dbf.count(ManagementNodeVO.class)
        assert count == 1
    }
}
