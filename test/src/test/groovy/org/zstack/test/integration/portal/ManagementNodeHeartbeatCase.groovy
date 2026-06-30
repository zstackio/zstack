package org.zstack.test.integration.portal

import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.managementnode.ManagementNodeState
import org.zstack.header.managementnode.ManagementNodeVO
import org.zstack.header.managementnode.ManagementNodeVO_
import org.zstack.portal.managementnode.ManagementNodeGlobalConfig
import org.zstack.portal.managementnode.ManagementNodeManagerImpl
import org.zstack.portal.managementnode.PortalGlobalProperty
import org.zstack.testlib.SubCase

import java.sql.Timestamp
import java.time.LocalDateTime

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
        testRecreateManagementNodeRecordWithManagedExistingRecord()
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
        prepareInvalidRecords()
        ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.updateValue(1)
        PortalGlobalProperty.MAX_HEARTBEAT_FAILURE = 2

        int failureInterval = ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.value(Integer.class)

        try {
            retryInSecs(failureInterval * 8, failureInterval) {
                long count = dbf.count(ManagementNodeVO.class)
                assert count == 1

                count = Q.New(ManagementNodeVO.class)
                        .notIn(ManagementNodeVO_.hostName, ['127.0.0.111', '127.0.0.222'])
                        .count()
                assert count == 1
            }
        } finally {
            PortalGlobalProperty.MAX_HEARTBEAT_FAILURE = 5
        }
    }

    void testRecreateManagementNodeRecordWithManagedExistingRecord() {
        String uuid = Platform.getManagementServerId()
        String ip = Platform.getManagementServerIp()
        ManagementNodeVO original = Q.New(ManagementNodeVO.class)
                .eq(ManagementNodeVO_.uuid, uuid)
                .find()
        ManagementNodeState state = original.state
        Timestamp heartBeat = original.heartBeat
        int port = original.port

        def method = ManagementNodeManagerImpl.class.getDeclaredMethod("recreateManagementNodeRecord", String.class, String.class)
        method.setAccessible(true)
        method.invoke(bean(ManagementNodeManagerImpl.class), ip, uuid)

        List<ManagementNodeVO> nodes = Q.New(ManagementNodeVO.class)
                .eq(ManagementNodeVO_.uuid, uuid)
                .list()
        assert nodes.size() == 1
        assert nodes[0].hostName == ip

        nodes[0].state = state
        nodes[0].heartBeat = heartBeat
        nodes[0].port = port
        dbf.update(nodes[0])
    }
}
