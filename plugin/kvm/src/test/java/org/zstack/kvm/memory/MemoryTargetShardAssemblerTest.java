package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryTargetShardAssemblerTest {
    @Test public void emptyDigestMatchesCanonicalJsonArray() throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest("[]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder expected = new StringBuilder();
        for (byte b : digest) { expected.append(String.format("%02x", b)); }
        assertEquals(expected.toString(), MemoryTargetShardAssembler.digest(Collections.emptyList()));
    }
    private static Map<Integer, List<String>> shards() {
        Map<Integer, List<String>> result = new HashMap<>();
        result.put(0, Arrays.asList("h2", "h1"));
        result.put(1, Collections.singletonList("h3"));
        return result;
    }

    @Test public void completeShardsAreSortedOnlyForDigestAndRetainAllTargets() {
        List<String> expected = Arrays.asList("h2", "h1", "h3");
        assertEquals(expected, MemoryTargetShardAssembler.assemble("g1", "g1", 7, 3,
                MemoryTargetShardAssembler.digest(expected), shards(), 2));
    }

    @Test public void missingShardIsRejected() {
        try {
            MemoryTargetShardAssembler.assemble("g1", "g1", 7, 3,
                    MemoryTargetShardAssembler.digest(Arrays.asList("h1", "h2", "h3")),
                    Collections.singletonMap(0, Arrays.asList("h1")), 2);
            fail("missing shard");
        } catch (MemoryOperationException e) { assertEquals("MEMORY_TARGET_SNAPSHOT_CONFLICT", e.getCode()); }
    }

    @Test public void duplicateAndDigestConflictsAreRejected() {
        Map<Integer, List<String>> duplicate = shards();
        duplicate.put(1, Collections.singletonList("h2"));
        try {
            MemoryTargetShardAssembler.assemble("g1", "g1", 7, 3,
                    MemoryTargetShardAssembler.digest(Arrays.asList("h1", "h2", "h3")), duplicate, 2);
            fail("duplicate");
        } catch (MemoryOperationException e) { assertEquals("MEMORY_TARGET_SNAPSHOT_CONFLICT", e.getCode()); }
    }

    @Test public void generationConflictIsRejected() {
        try {
            MemoryTargetShardAssembler.assemble("g2", "g1", 7, 3, "bad", shards(), 2);
            fail("generation conflict");
        } catch (MemoryOperationException e) { assertEquals("MEMORY_TARGET_SNAPSHOT_CONFLICT", e.getCode()); }
    }
}
