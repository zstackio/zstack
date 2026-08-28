package org.zstack.sdnController;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class SdnControllerApiInterceptorTest {
    @Test
    public void normalizeResourceUuidsTrimsBeforeValidationAndDeduplication() {
        String uuid = "00000000000040008000000000000001";

        assertEquals(Collections.singletonList(uuid),
                SdnControllerApiInterceptor.normalizeResourceUuids(
                        Arrays.asList("  " + uuid + "\n", uuid)));
    }
}
