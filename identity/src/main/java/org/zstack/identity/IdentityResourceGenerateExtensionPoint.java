package org.zstack.identity;

public interface IdentityResourceGenerateExtensionPoint {
    String getIdentityType();

    void prepareResources();
}
