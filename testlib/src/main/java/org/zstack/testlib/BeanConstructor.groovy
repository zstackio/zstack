package org.zstack.testlib

import org.zstack.core.Platform
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.header.exception.CloudRuntimeException

/**
 * Created by xing5 on 2017/2/12.
 */
class BeanConstructor {
    private List<String> coreBeans = [
            "ThreadFacade.xml",
            "CloudBus.xml",
            "validation.xml",
            "InventoryFacade.xml",
            "DatabaseFacade.xml",
            "JobQueueFacade.xml",
            "GlobalConfigFacade.xml",
            "ProgressBar.xml",
            "RESTFacade.xml",
            "QueryFacade.xml",
            "ansibleFacade.xml",
            "CascadeFacade.xml",
            "tag.xml",
            "Aspect.xml",
            "keyValueFacade.xml",
            "jmx.xml",
            "Error.xml",
            "gc.xml",
            "debug.xml",
            "jsonlabel.xml",
            "encrypt.xml",
            "rest.xml",
            "Progress.xml",
            "notification.xml"
    ]

    private List<String> xmls = []

    private String SPRING_XML_NAME = "spring-config-for-unit-test-from-BeanConstructor.xml"
    private String springConfigPath
    boolean loadAll

    BeanConstructor addXml(String xmlName) {
        if (this.getClass().getClassLoader().getResource(String.format("springConfigXml/%s", xmlName)) == null) {
            throw new IllegalArgumentException(String.format("Cannot find %s in test/src/test/resources/springConfigXml/", xmlName))
        }

        xmls.add(xmlName)
        return this
    }

    protected void generateSpringConfig() {
        try {
            //URL templatePath = this.getClass().getClassLoader().getResource("zstack-template.xml")
            URL templatePath = this.getClass().getClassLoader().getResource("zstack-template.xml")
            File template = new File(templatePath.getPath())
            List<String> contents = template.readLines()

            int insertPos = contents.size() - 1
            springConfigPath = "target/test-classes/" + SPRING_XML_NAME
            if (loadAll) {
                contents.add(insertPos, """\t<import resource="zstack.xml" />""")
            } else {
                (coreBeans + xmls).collect {
                    """\t<import resource="springConfigXml/$it" />"""
                }.each {
                    contents.add(insertPos, it)
                }
            }

            File tmpSpringConfig = new File(springConfigPath)
            tmpSpringConfig.write(contents.join("\n"))
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to create temporary spring xml config", e);
        }
    }

    ComponentLoader build() {
        generateSpringConfig()
        System.setProperty("spring.xml", SPRING_XML_NAME);
        return Platform.getComponentLoader()
    }
}
