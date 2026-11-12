package org.zstack.header;

public interface Component {
    /**
     * Initializes the component before the management node is ready.
     * Resource routing and the HTTP callback endpoint may not be available yet.
     * Start background business tasks that depend on them in
     * ManagementNodeReadyExtensionPoint.managementNodeReady().
     * Keep local initialization and listener registration here.
     */
    boolean start();

    boolean stop();
}
