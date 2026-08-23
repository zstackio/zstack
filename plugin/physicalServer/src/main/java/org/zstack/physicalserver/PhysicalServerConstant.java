package org.zstack.physicalserver;

public interface PhysicalServerConstant {
    String SERVICE_ID = "physicalServer";
    String ACTION_CATEGORY = "physicalServer";
    String CONTROL_OWNER_KEY = "physical-server-control-owner-v1";
    String ERROR_CODE = "ORG_ZSTACK_PHYSICALSERVER_10000";
    long RECONCILE_INTERVAL_MILLIS = 100_000L;
}
