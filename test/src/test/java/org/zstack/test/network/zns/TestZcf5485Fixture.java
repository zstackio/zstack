package org.zstack.test.network.zns;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class TestZcf5485Fixture {
    @Test
    public void fixtureMatchesManifest() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        byte[] fixture;
        try (InputStream stream = loader.getResourceAsStream("zns/zcf5485/segment-cloud-contract.json")) {
            Assert.assertNotNull(stream);
            fixture = stream.readAllBytes();
        }
        String expected;
        try (InputStream stream = loader.getResourceAsStream("zns/zcf5485/SHA256SUMS")) {
            Assert.assertNotNull(stream);
            expected = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim().split("\\s+")[0];
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(fixture);
        StringBuilder actual = new StringBuilder();
        for (byte value : digest) actual.append(String.format("%02x", value));
        Assert.assertEquals(expected, actual.toString());
    }
}
