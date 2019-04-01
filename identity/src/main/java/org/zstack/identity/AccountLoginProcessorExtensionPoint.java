package org.zstack.identity;

public interface AccountLoginProcessorExtensionPoint {
    String getResourceIdentity(String name);
}
