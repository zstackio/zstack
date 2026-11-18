package org.zstack.test.integration.physicalserver

import org.zstack.core.db.SQL
import org.zstack.physicalserver.PhysicalServerResourceAssignmentVO
import org.zstack.physicalserver.PhysicalServerVO
import org.zstack.testlib.SpringSpec
import org.zstack.testlib.Test

class PhysicalServerTest extends Test {
    static SpringSpec springSpec = makeSpring {
        sftpBackupStorage()
        localStorage()
        flatNetwork()
        securityGroup()
        kvm()
        externalPrimaryStorage()
        zbs()
        physicalServer()
    }

    @Override
    void setup() {
        useSpring(springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        runSubCases()
    }

    static void cleanupPhysicalServerRecords() {
        SQL.New("update ManagementNodeVO m set m.serverUuid = null").execute()
        SQL.New(PhysicalServerResourceAssignmentVO.class).delete()
        SQL.New(PhysicalServerVO.class).delete()
    }
}
