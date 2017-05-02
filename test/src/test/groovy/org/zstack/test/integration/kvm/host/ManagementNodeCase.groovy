package org.zstack.test.integration.kvm.host

import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.core.scheduler.SchedulerVO
import org.zstack.header.core.scheduler.SchedulerVO_
import org.zstack.header.managementnode.ManagementNodeState
import org.zstack.header.managementnode.ManagementNodeVO
import org.zstack.header.managementnode.ManagementNodeVO_
import org.zstack.portal.managementnode.ManagementNodeGlobalConfig
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by Administrator on 2017-03-22.
 */
class ManagementNodeCase extends SubCase{

    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.oneVmBasicEnv()
    }

    @Override
    void test() {
        env.create {
            testManagementNodeAbnormalQuit()
            ManagementNodeGlobalConfig.NODE_HEARTBEAT_INTERVAL.updateValue(1)
            testHeartbeat()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testManagementNodeAbnormalQuit() {
        def vo = Q.New(ManagementNodeVO.class).notNull(ManagementNodeVO_.uuid).list().get(0) as ManagementNodeVO
        SQL.New(ManagementNodeVO.class).eq(ManagementNodeVO_.uuid, vo.uuid).delete()
        assert Q.New(ManagementNodeVO.class).notNull(ManagementNodeVO_.uuid).list().get(0) as ManagementNodeVO == null
    }

    void testHeartbeat(){
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        ManagementNodeVO fake = new ManagementNodeVO()
        fake.setUuid(Platform.getUuid())
        fake.setHostName("192.168.0.11")
        fake.setPort(8080)
        fake.setState(ManagementNodeState.RUNNING)
        dbf.persist(fake)
        TimeUnit.SECONDS.sleep(5)
        assert dbf.count(ManagementNodeVO.class) == 1
    }
}
