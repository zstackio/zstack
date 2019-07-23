package org.zstack.identity;

import java.sql.Timestamp;

public interface AccountLoginProcessorExtensionPoint {
    String getResourceIdentity(String name);

    Timestamp getLastOperatedTime(String resourceIdentity);
}
