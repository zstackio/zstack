package org.zstack.test.unittest.core;

import org.junit.Test;
import org.zstack.core.convert.SpecialDataConverter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpecialDataConverterCase {
    @Test
    public void testMobileNumberSupportsCountryCodeAndSeparators() {
        assertTrue(SpecialDataConverter.isMobileNO("17717418234"));
        assertTrue(SpecialDataConverter.isMobileNO("+8617717418234"));
        assertTrue(SpecialDataConverter.isMobileNO("+86 17717418234"));
        assertTrue(SpecialDataConverter.isMobileNO("+86-17717418234"));
        assertTrue(SpecialDataConverter.isMobileNO("177 1741 8234"));

        assertFalse(SpecialDataConverter.isMobileNO("+1 17717418234"));
        assertFalse(SpecialDataConverter.isMobileNO(""));
        assertFalse(SpecialDataConverter.isMobileNO(null));
    }
}
