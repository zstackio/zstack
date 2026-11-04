package org.zstack.utils.test;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.utils.FeaturePluginClasses;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class TestFeaturePluginClasses {
    private static void assertNotFound(String name) {
        try {
            FeaturePluginClasses.forName(name);
            Assert.fail(name + " found");
        } catch (ClassNotFoundException expected) {
            // not visible
        }
    }

    @Test
    public void findsClassesOfAddedLoadersOnly() throws Exception {
        File dir = Files.createTempDirectory("plugin-classes").toFile();
        File src = new File(dir, "OnlyInPlugin.java");
        Files.write(src.toPath(), "package plug; public class OnlyInPlugin {}".getBytes(StandardCharsets.UTF_8));
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        Assert.assertEquals(0, javac.run(null, null, null, "-d", dir.getAbsolutePath(), src.getAbsolutePath()));

        try (URLClassLoader plugin = new URLClassLoader(new URL[]{dir.toURI().toURL()}, null)) {
            assertNotFound("plug.OnlyInPlugin");
            FeaturePluginClasses.add(plugin);
            try {
                Assert.assertSame(plugin, FeaturePluginClasses.forName("plug.OnlyInPlugin").getClassLoader());
                Assert.assertSame(String.class, FeaturePluginClasses.forName("java.lang.String"));
            } finally {
                FeaturePluginClasses.remove(plugin);
            }
            assertNotFound("plug.OnlyInPlugin");
        }
    }
}
