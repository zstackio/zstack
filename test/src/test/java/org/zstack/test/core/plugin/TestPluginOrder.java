package org.zstack.test.core.plugin;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.componentloader.PluginExtension;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.test.BeanConstructor;

import java.util.List;

public class TestPluginOrder {

    ComponentLoader loader;
    PluginRegistry plugRgty;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        con.addXml("PluginOrderTest.xml");
        loader = con.build();
        plugRgty = loader.getPluginRegistry();
    }

    @Test
    public void test() {
        List<PluginExtension> exts = plugRgty.getExtensionByInterfaceName(PluginOrderTestInterface.class.getName());
        PluginExtension ext1 = exts.get(0);
        Assert.assertEquals("PluginOrder1", ext1.getBeanName());
        PluginExtension ext2 = exts.get(1);
        Assert.assertEquals("PluginOrder2", ext2.getBeanName());
        PluginExtension ext3 = exts.get(2);
        Assert.assertEquals("PluginOrder3", ext3.getBeanName());
    }
}
