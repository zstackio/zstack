package org.zstack.test;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.zstack.utils.Utils;

import java.io.File;

public class WarBuilder {
    private String springConfigPath;
    private String warExportedToPath;

    public void build() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "zstack.war");
        war.setWebXML(new File("src/test/resources/webapp/WEB-INF/web.xml"));
        war.addAsWebInfResource(new File("src/test/resources/webapp/WEB-INF/zstack-servlet-context.xml"), "classes/zstack-servlet-context.xml");
        new ZipExporterImpl(war).exportTo(new File(Utils.getPathUtil().join(warExportedToPath, war.getName())), true);
    }

    public String getSpringConfigPath() {
        return springConfigPath;
    }

    public void setSpringConfigPath(String springConfigPath) {
        this.springConfigPath = springConfigPath;
    }

    public String getWarExportedToPath() {
        return warExportedToPath;
    }

    public void setWarExportedToPath(String warExportedToPath) {
        this.warExportedToPath = warExportedToPath;
    }
}
