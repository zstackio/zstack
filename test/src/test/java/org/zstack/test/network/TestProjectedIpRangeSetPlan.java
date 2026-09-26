package org.zstack.test.network;

import org.junit.Test;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.NormalIpRangeVO;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.network.l3.AddressPoolVO;
import org.zstack.header.network.l3.IpRangeVO;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.junit.Assert.*;

public class TestProjectedIpRangeSetPlan {
    @Test
    public void sameValuesKeepCloudIdentityDespiteSourceIdentity() throws Exception {
        NormalIpRangeVO old = existing("cloud", "192.168.10.10", "192.168.10.100");
        IpRangeInventory target = range("192.168.10.10", "192.168.10.100");
        target.setUuid("untrusted-source-uuid");
        Object plan = plan(Arrays.asList(old), Arrays.asList(target), Collections.emptyList(), null);
        assertEquals("cloud", ranges(plan).get(0).getUuid());
        assertTrue(deleted(plan).isEmpty());
        assertEquals("kept-name", ranges(plan).get(0).getName());
    }

    @Test
    public void splitMigratesUsedIpsWithoutChangingInputObjects() throws Exception {
        NormalIpRangeVO old = existing("old", "192.168.10.10", "192.168.10.100");
        UsedIpVO first = used("first", "192.168.10.20", "old");
        UsedIpVO second = used("second", "192.168.10.80", "old");
        Object plan = plan(Arrays.asList(old), Arrays.asList(range("192.168.10.10", "192.168.10.50"),
                range("192.168.10.51", "192.168.10.150")), Arrays.asList(first, second), null);
        assertEquals(Arrays.asList("old"), deleted(plan));
        Map<String, IpRangeInventory> targets = usedTargets(plan);
        assertEquals("192.168.10.50", targets.get("first").getEndIp());
        assertEquals("192.168.10.51", targets.get("second").getStartIp());
        assertEquals("old", first.getIpRangeUuid());
        assertEquals("192.168.10.100", old.getEndIp());
        assertNotEquals(targets.get("first").getUuid(), targets.get("second").getUuid());
    }

    @Test
    public void mergeAssignsBothOldRangesToOneNewCloudIdentity() throws Exception {
        Object plan = plan(Arrays.asList(existing("a", "192.168.10.10", "192.168.10.50"),
                        existing("b", "192.168.10.51", "192.168.10.100")),
                Arrays.asList(range("192.168.10.10", "192.168.10.100")),
                Arrays.asList(used("one", "192.168.10.20", "a"), used("two", "192.168.10.80", "b")), null);
        assertEquals(2, deleted(plan).size());
        assertEquals(usedTargets(plan).get("one").getUuid(), usedTargets(plan).get("two").getUuid());
    }

    @Test
    public void uncoveredUsedIpRejectsWholeBatch() throws Exception {
        assertRejected(Arrays.asList(existing("old", "192.168.10.10", "192.168.10.100")),
                Arrays.asList(range("192.168.10.10", "192.168.10.40")),
                Arrays.asList(used("occupied", "192.168.10.80", "old")), null);
    }

    @Test
    public void overlappingTargetsRejectWholeBatch() throws Exception {
        assertRejected(Collections.emptyList(), Arrays.asList(range("192.168.10.10", "192.168.10.50"),
                range("192.168.10.40", "192.168.10.100")), Collections.emptyList(), null);
    }

    @Test
    public void duplicateLocalValueIsRejectedRatherThanChoosingAnArbitraryUuid() throws Exception {
        assertRejected(Arrays.asList(existing("a", "192.168.10.10", "192.168.10.100"),
                existing("b", "192.168.10.10", "192.168.10.100")),
                Arrays.asList(range("192.168.10.10", "192.168.10.100")), Collections.emptyList(), null);
    }

    @Test
    public void onlyIdentifiedDhcpReservationCanRemainOutsidePool() throws Exception {
        NormalIpRangeVO old = existing("old", "192.168.10.2", "192.168.10.100");
        UsedIpVO dhcp = used("dhcp", "192.168.10.2", "old");
        Object result = plan(Arrays.asList(old), Arrays.asList(range("192.168.10.10", "192.168.10.100")),
                Arrays.asList(dhcp), "dhcp");
        assertTrue(usedTargets(result).containsKey("dhcp"));
        assertNull(usedTargets(result).get("dhcp"));
        dhcp.setVmNicUuid("vm-nic");
        assertRejected(Arrays.asList(old), Arrays.asList(range("192.168.10.10", "192.168.10.100")),
                Arrays.asList(dhcp), "dhcp");
        dhcp.setVmNicUuid(null);
        assertRejected(Arrays.asList(old), Collections.emptyList(), Arrays.asList(dhcp), "dhcp");
    }

    @Test
    public void ipv6TextNormalizationPreservesIdentity() throws Exception {
        NormalIpRangeVO old = existing("v6", "2001:db8::10", "2001:db8::100");
        old.setIpVersion(6);
        old.setGateway("2001:db8::1");
        old.setNetmask("ffff:ffff:ffff:ffff::");
        old.setPrefixLen(64);
        old.setNetworkCidr("2001:db8::/64");
        IpRangeInventory target = IpRangeInventory.valueOf(old);
        target.setStartIp("2001:0db8:0:0:0:0:0:10");
        target.setUuid(null);
        Object result = plan(Arrays.asList(old), Arrays.asList(target), Collections.emptyList(), null);
        assertEquals("v6", ranges(result).get(0).getUuid());
    }

    @Test
    public void anotherL3RangeCannotBeOverlappedByProjection() throws Exception {
        NormalIpRangeVO other = existing("other", "192.168.10.50", "192.168.10.100");
        other.setL3NetworkUuid("other-l3");
        Object result = plan(Collections.emptyList(), Arrays.asList(range("192.168.10.10", "192.168.10.100")),
                Collections.emptyList(), null);
        assertProtectedRejected(result, Arrays.asList(other));
    }

    @Test
    public void addressPoolPreventsRemovingLastNormalIpv4Range() throws Exception {
        AddressPoolVO pool = new AddressPoolVO();
        pool.setUuid("pool");
        pool.setL3NetworkUuid("l3");
        pool.setIpVersion(4);
        pool.setStartIp("192.168.20.10");
        pool.setEndIp("192.168.20.100");
        Object result = plan(Arrays.asList(existing("old", "192.168.10.10", "192.168.10.100")),
                Collections.emptyList(), Collections.emptyList(), null);
        assertProtectedRejected(result, Arrays.asList(pool));
    }

    @Test
    public void explicitlyObsoleteDhcpReservationIsReleasedWithEmptySet() throws Exception {
        UsedIpVO dhcp = used("dhcp", "192.168.10.20", "old");
        Object result = removalPlan(Arrays.asList(dhcp), "dhcp");
        assertEquals(Arrays.asList("dhcp"), result.getClass().getMethod("getDeletedUsedIpUuids").invoke(result));
        assertTrue(usedTargets(result).isEmpty());
        assertEquals("old", dhcp.getIpRangeUuid());
    }

    @Test
    public void obsoleteDhcpDoesNotExcuseAnotherOccupiedAddress() throws Exception {
        try {
            removalPlan(Arrays.asList(used("dhcp", "192.168.10.20", "old"),
                    used("occupied", "192.168.10.30", "old")), "dhcp");
            fail("ordinary allocation must reject the entire removal");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("occupied"));
        }
    }

    @Test
    public void obsoleteReservationMustBeAnUnattachedIpv4AddressOfThisL3() throws Exception {
        for (String invalid : Arrays.asList("nic", "purpose", "family", "l3")) {
            UsedIpVO ip = used("dhcp", "192.168.10.20", "old");
            if (invalid.equals("nic")) ip.setVmNicUuid("nic");
            if (invalid.equals("purpose")) ip.setUsedFor("service");
            if (invalid.equals("family")) ip.setIpVersion(6);
            if (invalid.equals("l3")) ip.setL3NetworkUuid("another-l3");
            try {
                removalPlan(Arrays.asList(ip), "dhcp");
                fail("invalid reservation identity must be rejected: " + invalid);
            } catch (IllegalArgumentException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }

    private Object removalPlan(List<UsedIpVO> used, String obsolete) throws Exception {
        Class<?> type = Class.forName("org.zstack.network.l3.ProjectedIpRangeSet");
        try {
            return type.getMethod("plan", String.class, List.class, List.class, List.class, String.class, String.class)
                    .invoke(null, "l3", Arrays.asList(existing("old", "192.168.10.10", "192.168.10.100")),
                            Collections.emptyList(), used, null, obsolete);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("range projection cannot release an explicitly obsolete DHCP reservation", missing);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof IllegalArgumentException) throw (IllegalArgumentException) error.getCause();
            throw error;
        }
    }

    private void assertProtectedRejected(Object plan, List<? extends IpRangeVO> protectedRanges) throws Exception {
        try {
            plan.getClass().getMethod("validateUnmanagedRanges", String.class, List.class)
                    .invoke(plan, "l3", protectedRanges);
            fail("projection must preserve constraints for ranges it does not manage");
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("projection does not validate unmanaged ranges", missing);
        } catch (InvocationTargetException error) {
            assertTrue(error.getCause() instanceof IllegalArgumentException);
        }
    }

    private void assertRejected(List<NormalIpRangeVO> old, List<IpRangeInventory> target,
                                List<UsedIpVO> used, String dhcp) throws Exception {
        try {
            plan(old, target, used, dhcp);
            fail("invalid range set must be rejected before changing the database");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    private Object plan(List<NormalIpRangeVO> old, List<IpRangeInventory> target,
                        List<UsedIpVO> used, String dhcp) throws Exception {
        Class<?> type;
        try {
            type = Class.forName("org.zstack.network.l3.ProjectedIpRangeSet");
        } catch (ClassNotFoundException absent) {
            throw new AssertionError("atomic range set planning is not implemented", absent);
        }
        try {
            return type.getMethod("plan", String.class, List.class, List.class, List.class, String.class)
                    .invoke(null, "l3", old, target, used, dhcp);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) error.getCause();
            }
            throw error;
        }
    }

    @SuppressWarnings("unchecked")
    private List<IpRangeInventory> ranges(Object plan) throws Exception {
        return (List<IpRangeInventory>) plan.getClass().getMethod("getRanges").invoke(plan);
    }

    @SuppressWarnings("unchecked")
    private List<String> deleted(Object plan) throws Exception {
        return (List<String>) plan.getClass().getMethod("getDeletedRangeUuids").invoke(plan);
    }

    @SuppressWarnings("unchecked")
    private Map<String, IpRangeInventory> usedTargets(Object plan) throws Exception {
        return (Map<String, IpRangeInventory>) plan.getClass().getMethod("getUsedIpTargets").invoke(plan);
    }

    private NormalIpRangeVO existing(String uuid, String start, String end) {
        NormalIpRangeVO range = new NormalIpRangeVO();
        range.setUuid(uuid);
        range.setL3NetworkUuid("l3");
        range.setName("kept-name");
        range.setStartIp(start);
        range.setEndIp(end);
        range.setGateway("192.168.10.1");
        range.setNetmask("255.255.255.0");
        range.setNetworkCidr("192.168.10.0/24");
        range.setPrefixLen(24);
        range.setIpVersion(4);
        return range;
    }

    private IpRangeInventory range(String start, String end) {
        IpRangeInventory range = IpRangeInventory.valueOf(existing(null, start, end));
        range.setName("source-name");
        return range;
    }

    private UsedIpVO used(String uuid, String ip, String rangeUuid) {
        UsedIpVO used = new UsedIpVO();
        used.setUuid(uuid);
        used.setIp(ip);
        used.setIpRangeUuid(rangeUuid);
        used.setL3NetworkUuid("l3");
        used.setIpVersion(4);
        return used;
    }
}
