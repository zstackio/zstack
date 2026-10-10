package org.zstack.kvm.memory;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class MemoryTargetRulesTest {
    @Test public void globalNeverImplicitlyTargetsTheEntireEnvironment() {
        List<String> hosts = Arrays.asList("host-a", "host-b");
        for (List<String> requested : Arrays.<List<String>>asList(null, Collections.emptyList(),
                Collections.singletonList("missing"))) {
            try { MemoryTargetRules.select("Global", hosts, requested); fail(); }
            catch (MemoryOperationException expected) { }
        }
        assertEquals(Collections.singletonList("host-b"), MemoryTargetRules.select("Global", hosts,
                Arrays.asList("host-b", "host-b")));
    }
    @Test public void hostAndVmCannotSmuggleAdditionalTargets() {
        try { MemoryTargetRules.select("Host", Collections.singletonList("a"), Collections.singletonList("b")); fail(); }
        catch (MemoryOperationException expected) { }
    }
    @Test public void targetSetIsNotLimitedToInternalBatchSize() {
        List<String> hosts = new ArrayList<>();
        for (int i = 0; i < 501; i++) { hosts.add("host-" + i); }
        assertEquals(501, MemoryTargetRules.select("Global", hosts, hosts).size());
        assertEquals(501, MemoryTargetRules.select("Cluster", hosts, hosts).size());
        try {
            MemoryTargetRules.select("Cluster", hosts, Collections.singletonList("other-cluster-host"));
            fail("cross-cluster host accepted");
        } catch (MemoryOperationException expected) { }
    }
}
