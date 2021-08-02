package org.zstack.portal.managementnode;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContext;

public class BootstrapContextLoaderListener extends ContextLoaderListener {
    private static ApplicationContext parent;

    @Override
    protected ApplicationContext loadParentContext(ServletContext servletContext) {
        String configurationXml = System.getProperty("spring.xml");
        String configLocation;
        if (configurationXml != null) {
            configLocation = String.format("classpath:%s", configurationXml);
        } else {
            configLocation = "classpath:zstack.xml";
        }

        if (parent == null) {
            parent = new ClassPathXmlApplicationContext(configLocation);
        }

        return parent;
    }
}
