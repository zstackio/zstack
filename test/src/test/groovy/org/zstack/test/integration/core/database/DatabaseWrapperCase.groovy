package org.zstack.test.integration.core.database

import org.zstack.core.db.Q
import org.zstack.header.configuration.InstanceOfferingVO
import org.zstack.header.configuration.InstanceOfferingVO_
import org.zstack.testlib.SubCase
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

    @Override
    void clean() {
    }
}
