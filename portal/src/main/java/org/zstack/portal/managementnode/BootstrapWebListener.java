package org.zstack.portal.managementnode;

import org.zstack.core.Platform;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 */
public class BootstrapWebListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        // this make sure Platform's static block executes before spring initialization
        Platform.getUuid();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}
