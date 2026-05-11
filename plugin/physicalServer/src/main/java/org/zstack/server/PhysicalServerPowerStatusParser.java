package org.zstack.server;

import org.zstack.header.server.PhysicalServerPowerStatus;

public final class PhysicalServerPowerStatusParser {
    private PhysicalServerPowerStatusParser() {
    }

    public static PhysicalServerPowerStatus parse(String stdout) {
        if (stdout == null) {
            return PhysicalServerPowerStatus.POWER_UNKNOWN;
        }
        String trimmed = stdout.trim();
        if ("Chassis Power is on".equals(trimmed)) {
            return PhysicalServerPowerStatus.POWER_ON;
        }
        if ("Chassis Power is off".equals(trimmed)) {
            return PhysicalServerPowerStatus.POWER_OFF;
        }
        return PhysicalServerPowerStatus.POWER_UNKNOWN;
    }
}
