package org.zstack.test.network.zns;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class TestZcf5485Fixture {
    @Test
    public void fixtureMatchesManifest() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        byte[] fixture;
        try (InputStream stream = loader.getResourceAsStream("zns/zcf5485/segment-cloud-contract.json")) {
            Assert.assertNotNull(stream);
            fixture = readAll(stream);
        }
        String expected;
        try (InputStream stream = loader.getResourceAsStream("zns/zcf5485/SHA256SUMS")) {
            Assert.assertNotNull(stream);
            expected = new String(readAll(stream), StandardCharsets.UTF_8).trim().split("\\s+")[0];
        }
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(fixture);
        StringBuilder actual = new StringBuilder();
        for (byte value : digest) actual.append(String.format("%02x", value));
        Assert.assertEquals(expected, actual.toString());
    }

    private static byte[] readAll(InputStream stream) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = stream.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
        return out.toByteArray();
    }
}
