package org.zstack.kvm.memory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Validates a complete, immutable target snapshot before policy CAS. */
final class MemoryTargetShardAssembler {
    private MemoryTargetShardAssembler() { }

    static List<String> assemble(String generation, String expectedGeneration,
                                  int expectedRevision, long total, String digest,
                                  Map<Integer, List<String>> shards, int shardCount) {
        if (generation == null || !generation.equals(expectedGeneration) ||
                shardCount <= 0 || total < 0 || digest == null || shards == null ||
                shards.size() != shardCount) {
            throw new MemoryOperationException("MEMORY_TARGET_SNAPSHOT_CONFLICT", "Target snapshot metadata changed or is incomplete");
        }
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < shardCount; i++) {
            List<String> shard = shards.get(i);
            if (shard == null) {
                throw new MemoryOperationException("MEMORY_TARGET_SNAPSHOT_INCOMPLETE", "Missing target shard " + i);
            }
            for (String host : shard) {
                if (host == null || host.isEmpty() || !seen.add(host)) {
                    throw new MemoryOperationException("MEMORY_TARGET_SNAPSHOT_CONFLICT", "Duplicate or invalid target host");
                }
                result.add(host);
            }
        }
        if (result.size() != total || !digest(result).equalsIgnoreCase(digest)) {
            throw new MemoryOperationException("MEMORY_TARGET_SNAPSHOT_DIGEST_MISMATCH", "Target count or digest mismatch");
        }
        return result;
    }

    static String digest(Collection<String> hosts) {
        try {
            List<String> ordered = new ArrayList<>(hosts);
            Collections.sort(ordered);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Match Go's service fingerprint([]string): compact JSON array.
            md.update(new com.google.gson.Gson().toJson(ordered).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : md.digest()) { result.append(String.format("%02x", b)); }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
