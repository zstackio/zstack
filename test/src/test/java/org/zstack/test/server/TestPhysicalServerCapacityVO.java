package org.zstack.test.server;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.header.server.PhysicalServerCapacityState;
import org.zstack.header.server.PhysicalServerCapacityVO;

import java.sql.Timestamp;

/**
 * Unit tests for PhysicalServerCapacityVO field structure and default values.
 *
 * These are pure unit tests (no Spring context, no DB) that validate:
 *   1. Happy-path: all 16 fields can be set and retrieved correctly.
 *   2. FK violation: documented as requiring MySQL FK enforcement.
 *   3. CASCADE delete: documented as requiring a live DB with FK enforcement.
 *
 * Full DB roundtrip (persist / findByUuid / dbf.remove) and FK/CASCADE
 * enforcement are covered by the integration Groovy case
 * PhysicalServerCapacityCase which runs against a real MySQL schema.
 * The in-memory H2 test environment used by the unit test suite does not
 * enforce FK constraints by default, so scenarios 2 and 3 are marked
 * TODO below rather than asserting false positives.
 */
public class TestPhysicalServerCapacityVO {

    // -----------------------------------------------------------------------
    // Scenario 1 — Happy path: all 16 fields round-trip via getters/setters
    // -----------------------------------------------------------------------

    @Test
    public void testAllSixteenFieldsRoundtrip() {
        PhysicalServerCapacityVO vo = new PhysicalServerCapacityVO();

        // PK
        vo.setUuid("aabbccdd11223344aabbccdd11223344");

        // 10 HostCapacityVO-aligned fields
        vo.setTotalMemory(8589934592L);
        vo.setTotalCpu(40000L);
        vo.setCpuNum(8L);
        vo.setCpuSockets(2);
        vo.setCpuCoreNum(4);
        vo.setAvailableMemory(4294967296L);
        vo.setAvailableCpu(20000L);
        vo.setTotalPhysicalMemory(8589934592L);
        vo.setAvailablePhysicalMemory(4294967296L);

        // 6 new governance fields
        vo.setCpuOverprovisioningRatio(4.0f);
        vo.setMemoryOverprovisioningRatio(1.5f);
        vo.setReservedMemory(1073741824L);
        vo.setTotalDisk(107374182400L);
        vo.setAvailableDisk(53687091200L);
        vo.setCapacityState(PhysicalServerCapacityState.Ready);

        // Timestamps
        Timestamp now = new Timestamp(System.currentTimeMillis());
        vo.setCreateDate(now);
        vo.setLastOpDate(now);

        // Assert PK
        Assert.assertEquals("aabbccdd11223344aabbccdd11223344", vo.getUuid());

        // Assert 10 HostCapacityVO-aligned fields
        Assert.assertEquals(8589934592L, vo.getTotalMemory());
        Assert.assertEquals(40000L, vo.getTotalCpu());
        Assert.assertEquals(8L, vo.getCpuNum());
        Assert.assertEquals(2, vo.getCpuSockets());
        Assert.assertEquals(4, vo.getCpuCoreNum());
        Assert.assertEquals(4294967296L, vo.getAvailableMemory());
        Assert.assertEquals(20000L, vo.getAvailableCpu());
        Assert.assertEquals(8589934592L, vo.getTotalPhysicalMemory());
        Assert.assertEquals(4294967296L, vo.getAvailablePhysicalMemory());

        // Assert 6 governance fields
        Assert.assertEquals(4.0f, vo.getCpuOverprovisioningRatio(), 0.001f);
        Assert.assertEquals(1.5f, vo.getMemoryOverprovisioningRatio(), 0.001f);
        Assert.assertEquals(1073741824L, vo.getReservedMemory());
        Assert.assertEquals(107374182400L, vo.getTotalDisk());
        Assert.assertEquals(53687091200L, vo.getAvailableDisk());
        Assert.assertEquals(PhysicalServerCapacityState.Ready, vo.getCapacityState());

        // Assert timestamps
        Assert.assertEquals(now, vo.getCreateDate());
        Assert.assertEquals(now, vo.getLastOpDate());
    }

    // -----------------------------------------------------------------------
    // Scenario 2 — Default field values on a fresh instance
    // -----------------------------------------------------------------------

    @Test
    public void testDefaultValues() {
        PhysicalServerCapacityVO vo = new PhysicalServerCapacityVO();

        Assert.assertNull(vo.getUuid());
        Assert.assertEquals(0L, vo.getTotalMemory());
        Assert.assertEquals(0L, vo.getTotalCpu());
        Assert.assertEquals(0L, vo.getCpuNum());
        Assert.assertEquals(0, vo.getCpuSockets());
        Assert.assertEquals(0, vo.getCpuCoreNum());
        Assert.assertEquals(0L, vo.getAvailableMemory());
        Assert.assertEquals(0L, vo.getAvailableCpu());
        Assert.assertEquals(0L, vo.getTotalPhysicalMemory());
        Assert.assertEquals(0L, vo.getAvailablePhysicalMemory());

        // Governance defaults
        Assert.assertEquals(1.0f, vo.getCpuOverprovisioningRatio(), 0.001f);
        Assert.assertEquals(1.0f, vo.getMemoryOverprovisioningRatio(), 0.001f);
        Assert.assertEquals(0L, vo.getReservedMemory());
        Assert.assertEquals(0L, vo.getTotalDisk());
        Assert.assertEquals(0L, vo.getAvailableDisk());
        Assert.assertNull(vo.getCapacityState());
        Assert.assertNull(vo.getCreateDate());
        Assert.assertNull(vo.getLastOpDate());
    }

    // -----------------------------------------------------------------------
    // Scenario 3 — All PhysicalServerCapacityState enum values accessible
    // -----------------------------------------------------------------------

    @Test
    public void testCapacityStateEnumValues() {
        PhysicalServerCapacityVO vo = new PhysicalServerCapacityVO();

        for (PhysicalServerCapacityState state : PhysicalServerCapacityState.values()) {
            vo.setCapacityState(state);
            Assert.assertEquals(state, vo.getCapacityState());
        }

        // Verify all 5 expected values are present
        PhysicalServerCapacityState[] states = PhysicalServerCapacityState.values();
        Assert.assertEquals(5, states.length);
    }

    // -----------------------------------------------------------------------
    // Scenario 4 — FK violation (DB-level; requires MySQL FK enforcement)
    // TODO: requires MySQL FK enforcement
    // When persisting a PhysicalServerCapacityVO with a uuid that has no
    // matching PhysicalServerVO row, MySQL raises:
    //   java.sql.SQLIntegrityConstraintViolationException (FK violation)
    // This cannot be asserted in the in-memory H2 unit test suite because
    // H2 (used by the ZStack unit test harness) does not enforce FK constraints
    // in the same way MySQL does unless explicitly configured.
    // Full FK enforcement is tested in PhysicalServerCapacityCase (Groovy
    // integration test, runs against MySQL).
    // -----------------------------------------------------------------------

    // -----------------------------------------------------------------------
    // Scenario 5 — CASCADE delete (DB-level; requires MySQL FK enforcement)
    // TODO: requires MySQL FK enforcement
    // When a PhysicalServerVO is deleted, the ON DELETE CASCADE clause on
    // PhysicalServerCapacityVO.uuid must automatically remove the capacity row.
    // Verifiable only in the MySQL integration test environment.
    // The @ForeignKey(onDeleteAction = ReferenceOption.CASCADE) annotation on
    // the uuid field is the DDL directive that produces this SQL constraint;
    // the ZStack schema generator reads this annotation at startup.
    // -----------------------------------------------------------------------
}
