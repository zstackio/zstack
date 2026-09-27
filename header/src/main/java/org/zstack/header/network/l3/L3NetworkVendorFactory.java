package org.zstack.header.network.l3;

public interface L3NetworkVendorFactory {
    String getVSwitchType();

    L3NetworkBackend create(L3NetworkBackendContext context);
}
