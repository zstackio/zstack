package org.zstack.test.core.plugin;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.test.BeanConstructor;

import java.util.List;

public class TestPluginOrder2 {

    ComponentLoader loader;
    PluginRegistry plugRgty;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        con.addXml("PluginOrderTest2.xml");
        loader = con.build();
        plugRgty = loader.getPluginRegistry();
    }

    @Test
    public void test() {
        List<PluginExtension> exts = plugRgty.getExtensionByInterfaceName(PluginOrderTestInterface.class.getName());
        PluginExtension ext1 = exts.get(2);
        Assert.assertEquals("PluginOrder2", ext1.getBeanName());
    }
}
