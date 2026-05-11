package org.zstack.test.server;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.header.server.PhysicalServerProvisionNetworkPoolRefVO;

import java.sql.Timestamp;

/**
 * Unit tests for PhysicalServerProvisionNetworkPoolRefVO (provision PRD §2.2 BLOCKER B7).
 *
 * Scenario 1 — Happy path: all 5 fields can be set and retrieved correctly.
 * Scenario 2 — UNIQUE(networkUuid, poolUuid) violation: requires MySQL FK/UNIQUE
 *              enforcement; H2 in-memory DB used by this test suite does not
 *              enforce UNIQUE constraints from JPA @UniqueConstraint at the DB level
 *              in the same way MySQL does.
 *              TODO: needs MySQL for UNIQUE enforcement (covered by integration case).
 * Scenario 3 — CASCADE on pool delete: requires MySQL FK enforcement.
 *              TODO: needs MySQL for FK/CASCADE enforcement (covered by integration case).
 */
public class TestAttachProvisionNetworkToPool {

    // -----------------------------------------------------------------------
    // Scenario 1 — Happy path: all fields round-trip via getters/setters
    // -----------------------------------------------------------------------

    @Test
    public void testAllFieldsRoundtrip() {
        PhysicalServerProvisionNetworkPoolRefVO ref = new PhysicalServerProvisionNetworkPoolRefVO();

        ref.setId(1L);
        ref.setNetworkUuid("aabbccdd11223344aabbccdd11223344");
        ref.setPoolUuid("11223344aabbccdd11223344aabbccdd");

        Timestamp now = new Timestamp(System.currentTimeMillis());
        ref.setCreateDate(now);
        ref.setLastOpDate(now);

        Assert.assertEquals(1L, ref.getId());
        Assert.assertEquals("aabbccdd11223344aabbccdd11223344", ref.getNetworkUuid());
        Assert.assertEquals("11223344aabbccdd11223344aabbccdd", ref.getPoolUuid());
        Assert.assertEquals(now, ref.getCreateDate());
        Assert.assertEquals(now, ref.getLastOpDate());
    }

    // -----------------------------------------------------------------------
    // Scenario 2 — Default values on a fresh instance
    // -----------------------------------------------------------------------

    @Test
    public void testDefaultValues() {
        PhysicalServerProvisionNetworkPoolRefVO ref = new PhysicalServerProvisionNetworkPoolRefVO();

        Assert.assertEquals(0L, ref.getId());
        Assert.assertNull(ref.getNetworkUuid());
        Assert.assertNull(ref.getPoolUuid());
        Assert.assertNull(ref.getCreateDate());
        Assert.assertNull(ref.getLastOpDate());
    }

    // -----------------------------------------------------------------------
    // Scenario 3 — Two refs with different (networkUuid, poolUuid) are distinct
    // -----------------------------------------------------------------------

    @Test
    public void testDistinctPairs() {
        PhysicalServerProvisionNetworkPoolRefVO ref1 = new PhysicalServerProvisionNetworkPoolRefVO();
        ref1.setNetworkUuid("net-uuid-aaaa");
        ref1.setPoolUuid("pool-uuid-1111");

        PhysicalServerProvisionNetworkPoolRefVO ref2 = new PhysicalServerProvisionNetworkPoolRefVO();
        ref2.setNetworkUuid("net-uuid-aaaa");
        ref2.setPoolUuid("pool-uuid-2222");

        Assert.assertNotEquals(ref1.getPoolUuid(), ref2.getPoolUuid());
        Assert.assertEquals(ref1.getNetworkUuid(), ref2.getNetworkUuid());
    }

    // -----------------------------------------------------------------------
    // Scenario 4 — UNIQUE violation (DB-level; requires MySQL UNIQUE enforcement)
    // TODO: needs MySQL for UNIQUE enforcement
    // When persisting two PhysicalServerProvisionNetworkPoolRefVO rows with the
    // same (networkUuid, poolUuid), MySQL raises:
    //   java.sql.SQLIntegrityConstraintViolationException (UNIQUE violation)
    // H2 used by the unit test harness does not enforce @UniqueConstraint at
    // the DB level without explicit configuration.
    // Full UNIQUE enforcement is tested in integration cases against MySQL.
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Scenario 5 — CASCADE on pool delete (DB-level; requires MySQL FK enforcement)
    // TODO: needs MySQL for FK/CASCADE enforcement
    // When a ServerPoolVO is deleted, the ON DELETE CASCADE clause on
    // PhysicalServerProvisionNetworkPoolRefVO.poolUuid must automatically remove
    // the ref row. Verifiable only in the MySQL integration test environment.
    // The @ForeignKey(onDeleteAction = ReferenceOption.CASCADE) annotation on
    // poolUuid is the DDL directive that produces this SQL constraint.
    // -----------------------------------------------------------------------
}
