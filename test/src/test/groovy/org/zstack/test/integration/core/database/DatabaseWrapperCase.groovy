package org.zstack.test.integration.core.database

import org.hibernate.exception.ConstraintViolationException
import org.zstack.core.Platform
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQLBatch
import org.zstack.header.configuration.InstanceOfferingVO
import org.zstack.header.configuration.InstanceOfferingVO_
import org.zstack.header.identity.AccountConstant
import org.zstack.testlib.SubCase

import java.sql.SQLException

/**
 * Created by david on 7/13/17.
 */
class DatabaseWrapperCase extends SubCase {

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testQ()
        testSQLBatch()
    }

    void testQ() {
        def offerings = Q.New(InstanceOfferingVO.class)
                .eq(InstanceOfferingVO_.memorySize, 1234)
                .list()

        assert offerings != null : "list should always return non-null"
        assert offerings.isEmpty() : "expect no offerings found"

        offerings = Q.New(InstanceOfferingVO.class)
                .eq(InstanceOfferingVO_.memorySize, 1234)
                .select(InstanceOfferingVO_.cpuNum)
                .listValues()

        assert offerings != null : "listValues should always return non-null"
        assert offerings.isEmpty() : "expect no values found"

        offerings = Q.New(InstanceOfferingVO.class)
                .eq(InstanceOfferingVO_.memorySize, 1234)
                .select(InstanceOfferingVO_.cpuNum)
                .select(InstanceOfferingVO_.memorySize)
                .listTuple()
        assert offerings != null : "listTuples should always return non-null"
        assert offerings.isEmpty() : "expect no tuples found"
    }

    void testSQLBatch() {
        def offeringUuid = Platform.uuid
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        InstanceOfferingVO v2 = new InstanceOfferingVO()
        def offeringUuid2 = Platform.uuid
        v2.memorySize = 22345
        v2.cpuNum = 5
        v2.cpuSpeed = 0
        v2.uuid = offeringUuid2
        v2.duration = "Permanent"
        v2.name = "offeringA"
        v2.state = "Enabled"
        v2.sortKey = 0
        v2.type = "UserVm"
        v2.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
        dbf.persist(v2)

        try {
            new SQLBatch() {

                @Override
                protected void scripts() {
                    InstanceOfferingVO v = new InstanceOfferingVO()
                    v.memorySize = 12345
                    v.cpuNum = 5
                    v.uuid = offeringUuid

                    sql(InstanceOfferingVO.class)
                            .eq(InstanceOfferingVO_.uuid, offeringUuid2)
                            .set(InstanceOfferingVO_.memorySize, 1122)
                            .update()

                    dbf.getEntityManager().persist(v)
                    throw new ConstraintViolationException("on purpose", new SQLException(), "constraint")
                }

            }.execute()
        } catch (Throwable ignored) {
        }

        assert !dbf.isExist(offeringUuid, InstanceOfferingVO.class)
        def v3 = dbf.findByUuid(offeringUuid2, InstanceOfferingVO.class)
        assert v3 != null
        assert v3.memorySize == v2.memorySize

        dbf.remove(v2)
    }

    @Override
    void clean() {
    }
}
