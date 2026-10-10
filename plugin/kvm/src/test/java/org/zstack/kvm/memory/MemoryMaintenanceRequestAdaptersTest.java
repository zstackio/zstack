package org.zstack.kvm.memory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MemoryMaintenanceRequestAdaptersTest {
    private static final String HASH = repeat('a', 64);
    private static final String BOOT = "123e4567-e89b-12d3-a456-426614174000";
    private static final String UUID = repeat('1', 32);
    private final Gson gson = new GsonBuilder().create();

    @Test public void standaloneRequestObjectsAreRegisteredForPlatformSdkGeneration() {
        for (Class<?> type : new Class<?>[]{MemoryBackendPreparation.class,
                MemoryZramPoolPreparation.class, MemoryUncertainRecovery.class}) {
            assertTrue(type.getName(), type.isAnnotationPresent(org.zstack.header.rest.SDK.class));
        }
    }

    @Test public void rejectsDuplicateMembersFromTheOriginalJsonStream() {
        bad("{\"candidateId\":\"candidate:" + HASH + "\",\"expectedHostBootId\":\"" + BOOT
                + "\",\"expectedPoolGeneration\":\"none\",\"backendCapacityBytes\":8192,"
                + "\"resetConfirmed\":true,\"resetConfirmed\":false}", MemoryBackendPreparation.class);
        bad("{\"expectedHostBootId\":\"" + BOOT + "\",\"expectedPoolGeneration\":\"" + HASH
                + "\",\"resetConfirmed\":true,\"resetConfirmed\":false}", MemoryZramPoolPreparation.class);
        bad("{\"expectedHostBootId\":\"" + BOOT + "\",\"expectedPoolGeneration\":\"" + HASH
                + "\",\"drainControlOperationUuid\":\"" + UUID
                + "\",\"confirmed\":true,\"confirmed\":false}", MemoryUncertainRecovery.class);
    }

    @Test public void rejectsUnknownMembersAndWrongJsonKinds() {
        bad("{\"expectedHostBootId\":\"" + BOOT + "\",\"expectedPoolGeneration\":\"" + HASH
                + "\",\"resetConfirmed\":true,\"device\":\"/dev/zram0\"}", MemoryZramPoolPreparation.class);
        bad("{\"expectedHostBootId\":\"" + BOOT + "\",\"expectedPoolGeneration\":\"" + HASH
                + "\",\"resetConfirmed\":\"true\"}", MemoryZramPoolPreparation.class);
        bad("{\"candidateId\":\"candidate:" + HASH + "\",\"expectedHostBootId\":\"" + BOOT
                + "\",\"expectedPoolGeneration\":\"none\",\"backendCapacityBytes\":\"8192\","
                + "\"resetConfirmed\":true}", MemoryBackendPreparation.class);
        bad("{\"candidateId\":\"candidate:" + HASH + "\",\"expectedHostBootId\":\"" + BOOT
                + "\",\"expectedPoolGeneration\":\"none\",\"backendCapacityBytes\":8192.5,"
                + "\"resetConfirmed\":true}", MemoryBackendPreparation.class);
        bad("{\"candidateId\":\"candidate:" + HASH + "\",\"expectedHostBootId\":\"" + BOOT
                + "\",\"expectedPoolGeneration\":\"none\",\"backendCapacityBytes\":-8192,"
                + "\"resetConfirmed\":true}", MemoryBackendPreparation.class);
        bad("{\"candidateId\":\"candidate:" + HASH + "\",\"expectedHostBootId\":\"" + BOOT
                + "\",\"expectedPoolGeneration\":\"none\",\"backendCapacityBytes\":9223372036854775808,"
                + "\"resetConfirmed\":true}", MemoryBackendPreparation.class);
    }

    @Test public void acceptsExactDecimalAndExponentIntegerRepresentations() {
        for (String representation : new String[]{"8192.0", "8.192e3"}) {
            MemoryBackendPreparation decoded = gson.fromJson("{\"candidateId\":\"candidate:" + HASH
                    + "\",\"expectedHostBootId\":\"" + BOOT + "\",\"expectedPoolGeneration\":\"none\","
                    + "\"backendCapacityBytes\":" + representation + ",\"resetConfirmed\":true}",
                    MemoryBackendPreparation.class);
            assertEquals(8192L, decoded.getBackendCapacityBytes());
        }
    }

    @Test public void rejectsMissingRequiredMembersAndNullObjects() {
        bad("{}", MemoryBackendPreparation.class);
        bad("{\"expectedHostBootId\":\"" + BOOT + "\",\"resetConfirmed\":true}",
                MemoryZramPoolPreparation.class);
        bad("{\"expectedHostBootId\":\"" + BOOT + "\",\"expectedPoolGeneration\":\"" + HASH
                + "\",\"confirmed\":true}", MemoryUncertainRecovery.class);
        bad("null", MemoryZramPoolPreparation.class);
    }

    @Test public void keepsLargeAlignedCapacityExactWithoutDoubleConversion() {
        long exact = 8_796_093_022_208L;
        MemoryBackendPreparation decoded = gson.fromJson("{\"candidateId\":\"candidate:" + HASH
                + "\",\"expectedHostBootId\":\"" + BOOT + "\",\"expectedPoolGeneration\":\"none\","
                + "\"backendCapacityBytes\":" + exact + ",\"resetConfirmed\":true}", MemoryBackendPreparation.class);
        assertEquals(exact, decoded.getBackendCapacityBytes());
        assertTrue(decoded.isResetConfirmed());
        String encoded = gson.toJson(decoded);
        MemoryBackendPreparation roundTrip = gson.fromJson(encoded, MemoryBackendPreparation.class);
        assertEquals(exact, roundTrip.getBackendCapacityBytes());
    }

    private <T> void bad(String raw, Class<T> type) {
        try {
            gson.fromJson(raw, type);
            fail("invalid raw JSON was accepted: " + raw);
        } catch (JsonParseException expected) {
            // expected
        }
    }

    private static String repeat(char c, int count) {
        StringBuilder value = new StringBuilder(count);
        for (int i = 0; i < count; i++) { value.append(c); }
        return value.toString();
    }
}
