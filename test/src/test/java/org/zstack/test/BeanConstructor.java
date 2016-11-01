package org.zstack.test;

import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.exception.CloudRuntimeException;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.StringDSL.ln;

public class BeanConstructor {
    private List<String> xmls = new ArrayList<String>();
    private List<String> excludedXmls = new ArrayList<String>();
    private static final String SPRING_XML_NAME = "spring-config-for-unit-test-from-BeanConstructor.xml";
    protected ComponentLoader loader;
    protected String springConfigPath;
    private boolean loadAll = false;

    private String coreBeans = ln(
            "<import resource=\"springConfigXml/ThreadFacade.xml\" />",
            "<import resource=\"springConfigXml/CloudBus.xml\" />",
            "<import resource=\"springConfigXml/validation.xml\" />",
            "<import resource=\"springConfigXml/InventoryFacade.xml\" />",
            "<import resource=\"springConfigXml/DatabaseFacade.xml\" />",
            "<import resource=\"springConfigXml/JobQueueFacade.xml\" />",
            "<import resource=\"springConfigXml/GlobalConfigFacade.xml\" />",
            "<import resource=\"springConfigXml/ProgressBar.xml\" />",
            "<import resource=\"springConfigXml/RESTFacade.xml\" />",
            "<import resource=\"springConfigXml/QueryFacade.xml\" />",
            "<import resource=\"springConfigXml/ansibleFacade.xml\" />",
            "<import resource=\"springConfigXml/CascadeFacade.xml\" />",
            "<import resource=\"springConfigXml/tag.xml\" />",
            "<import resource=\"springConfigXml/Aspect.xml\" />",
            "<import resource=\"springConfigXml/keyValueFacade.xml\" />",
            "<import resource=\"springConfigXml/jmx.xml\" />",
            "<import resource=\"springConfigXml/logFacade.xml\" />",
            "<import resource=\"springConfigXml/Error.xml\" />",
            "<import resource=\"springConfigXml/gc.xml\" />",
            "<import resource=\"springConfigXml/debug.xml\" />",
            "<import resource=\"springConfigXml/SchedulerFacade.xml\" />",
            "<import resource=\"springConfigXml/jsonlabel.xml\" />",
            "<import resource=\"springConfigXml/EncryptRSA.xml\" />"
    ).toString();

    public BeanConstructor() {
    }

    public BeanConstructor addXml(String xmlName) {
        if (this.getClass().getClassLoader().getResource(String.format("springConfigXml/%s", xmlName)) == null) {
            throw new IllegalArgumentException(String.format("Cannot find %s in test/src/test/resources/springConfigXml/", xmlName));
        }

        xmls.add(xmlName);
        return this;
    }

    public BeanConstructor excludeXml(String name) {
        excludedXmls.add(name);
        return this;
    }

    public BeanConstructor addAllConfigInZstackXml() {
        loadAll = true;
        return this;
    }

    protected void generateSpringConfig() {
        try {
            URL templatePath = this.getClass().getClassLoader().getResource("zstack-template.xml");
            File template = new File(templatePath.getPath());
            BufferedReader input = new BufferedReader(new FileReader(template));
            List<String> contents = new ArrayList<String>();
            String line = null;
            while ((line = input.readLine()) != null) {
                contents.add(line);
            }

            String configPath = "target/test-classes/" + SPRING_XML_NAME;
            int insertPos = contents.size() - 1;
            if (loadAll) {
                String r = String.format("\t<import resource=\"zstack.xml\" />");
                contents.add(insertPos, r);
            } else {
                for (String bean : coreBeans.split("\\n")) {
                    contents.add(insertPos, bean);
                }
            }

            for (String c : xmls) {
                String r = String.format("\t<import resource=\"springConfigXml/%s\" />", c);
                contents.add(insertPos, r);
            }

            File tmpSpringConfig = new File(configPath);
            FileWriter fw = new FileWriter(tmpSpringConfig.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(fw);
            for (String s : contents) {
                bw.write(s + "\n");
            }
            bw.close();
            fw.close();
            springConfigPath = configPath;
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to create temporary spring xml config", e);
        }
    }

    public ComponentLoader build() {
        excludeXmls();
        generateSpringConfig();
        System.setProperty("spring.xml", SPRING_XML_NAME);
        loader = Platform.getComponentLoader();
        return loader;
    }

    private void excludeXmls() {
        List<String> tmp = new ArrayList<String>();
        for (String xml : xmls) {
            if (excludedXmls.contains(xml)) {
                continue;
            }

            tmp.add(xml);
        }

        xmls = tmp;
    }
}
