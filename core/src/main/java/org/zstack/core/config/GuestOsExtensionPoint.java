package org.zstack.core.config;

import org.zstack.core.config.schema.GuestOsCharacter;

public interface GuestOsExtensionPoint {
    void validateGuestOsCharacter(GuestOsCharacter.Config config);
}
