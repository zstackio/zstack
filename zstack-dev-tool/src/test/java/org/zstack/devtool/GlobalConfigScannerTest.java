package org.zstack.devtool;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.zstack.devtool.checker.GlobalConfigDocChecker;
import org.zstack.devtool.generator.GlobalConfigDocGenerator;
import org.zstack.devtool.model.GlobalConfigInfo;
import org.zstack.devtool.scanner.GlobalConfigScanner;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class GlobalConfigScannerTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private File createJavaFile(String filename, String content) throws Exception {
        File dir = tempDir.newFolder("src", "main", "java");
        File file = new File(dir, filename);
        try (FileWriter w = new FileWriter(file)) {
            w.write(content);
        }
        return dir;
    }

    @Test
    public void testScanBasicGlobalConfig() throws Exception {
        String source =
                "package org.zstack.test;\n" +
                "import org.zstack.core.config.*;\n" +
                "@GlobalConfigDefinition\n" +
                "public class TestGlobalConfig {\n" +
                "    public static final String CATEGORY = \"test\";\n" +
                "    @GlobalConfigValidation\n" +
                "    @GlobalConfigDef(type = Long.class, defaultValue = \"60\", description = \"test desc\")\n" +
                "    public static GlobalConfig FOO = new GlobalConfig(CATEGORY, \"foo.bar\");\n" +
                "}\n";

        File srcDir = createJavaFile("TestGlobalConfig.java", source);
        GlobalConfigScanner scanner = new GlobalConfigScanner();
        List<GlobalConfigInfo> results = scanner.scan(Collections.singletonList(srcDir.toPath()));

        assertEquals(1, results.size());
        GlobalConfigInfo info = results.get(0);
        assertEquals("test", info.getCategory());
        assertEquals("foo.bar", info.getName());
        assertEquals("java.lang.Long", info.getType());
        assertEquals("60", info.getDefaultValue());
        assertEquals("test desc", info.getDescription());
    }

    @Test
    public void testScanWithValidValues() throws Exception {
        String source =
                "package org.zstack.test;\n" +
                "import org.zstack.core.config.*;\n" +
                "@GlobalConfigDefinition\n" +
                "public class TestGlobalConfig {\n" +
                "    public static final String CATEGORY = \"vm\";\n" +
                "    @GlobalConfigValidation(validValues = {\"cirrus\", \"vga\", \"qxl\"})\n" +
                "    @GlobalConfigDef(type = String.class, defaultValue = \"vga\")\n" +
                "    public static GlobalConfig VIDEO = new GlobalConfig(CATEGORY, \"videoType\");\n" +
                "}\n";

        File srcDir = createJavaFile("TestGlobalConfig.java", source);
        GlobalConfigScanner scanner = new GlobalConfigScanner();
        List<GlobalConfigInfo> results = scanner.scan(Collections.singletonList(srcDir.toPath()));

        assertEquals(1, results.size());
        GlobalConfigInfo info = results.get(0);
        assertEquals("{cirrus, vga, qxl}", info.getValueRange());
    }

    @Test
    public void testScanWithNumberRange() throws Exception {
        String source =
                "package org.zstack.test;\n" +
                "import org.zstack.core.config.*;\n" +
                "@GlobalConfigDefinition\n" +
                "public class TestGlobalConfig {\n" +
                "    public static final String CATEGORY = \"test\";\n" +
                "    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 65535)\n" +
                "    @GlobalConfigDef(type = Integer.class, defaultValue = \"8080\")\n" +
                "    public static GlobalConfig PORT = new GlobalConfig(CATEGORY, \"port\");\n" +
                "}\n";

        File srcDir = createJavaFile("TestGlobalConfig.java", source);
        GlobalConfigScanner scanner = new GlobalConfigScanner();
        List<GlobalConfigInfo> results = scanner.scan(Collections.singletonList(srcDir.toPath()));

        assertEquals(1, results.size());
        assertEquals("[0, 65535]", results.get(0).getValueRange());
    }

    @Test
    public void testCheckerDetectsMissing() throws Exception {
        GlobalConfigInfo info = new GlobalConfigInfo();
        info.setCategory("test");
        info.setName("missing.config");
        info.setType("java.lang.String");
        info.setDefaultValue("abc");
        info.setSourceFile("TestGlobalConfig.java");
        info.setFieldName("MISSING");

        Path docDir = tempDir.newFolder("doc").toPath();
        GlobalConfigDocChecker checker = new GlobalConfigDocChecker();
        GlobalConfigDocChecker.CheckResult result = checker.check(
                Collections.singletonList(info), docDir);

        assertFalse(result.passed());
        assertEquals(1, result.missing.size());
        assertEquals("missing.config", result.missing.get(0).getName());
    }

    @Test
    public void testGeneratorCreatesMissing() throws Exception {
        GlobalConfigInfo info = new GlobalConfigInfo();
        info.setCategory("test");
        info.setName("new.config");
        info.setType("java.lang.Integer");
        info.setDefaultValue("42");
        info.setDescription("a test config");

        Path docDir = tempDir.newFolder("doc").toPath();
        GlobalConfigDocGenerator generator = new GlobalConfigDocGenerator();
        int created = generator.generate(Collections.singletonList(info), docDir, true);

        assertEquals(1, created);
        Path mdFile = docDir.resolve("test/new.config.md");
        assertTrue(Files.exists(mdFile));
        String content = new String(Files.readAllBytes(mdFile));
        assertTrue(content.contains("new.config"));
        assertTrue(content.contains("java.lang.Integer"));
        assertTrue(content.contains("42"));
        assertTrue(content.contains("a test config"));
    }

    @Test
    public void testGeneratorSkipsExisting() throws Exception {
        GlobalConfigInfo info = new GlobalConfigInfo();
        info.setCategory("test");
        info.setName("existing");
        info.setType("java.lang.String");
        info.setDefaultValue("x");

        Path docDir = tempDir.newFolder("doc").toPath();
        Files.createDirectories(docDir.resolve("test"));
        Files.write(docDir.resolve("test/existing.md"), "existing content".getBytes());

        GlobalConfigDocGenerator generator = new GlobalConfigDocGenerator();
        int created = generator.generate(Collections.singletonList(info), docDir, true);

        assertEquals(0, created);
        String content = new String(Files.readAllBytes(docDir.resolve("test/existing.md")));
        assertEquals("existing content", content);
    }

    @Test
    public void testRoundTrip() throws Exception {
        GlobalConfigInfo info = new GlobalConfigInfo();
        info.setCategory("ai");
        info.setName("test.round.trip");
        info.setType("java.lang.Long");
        info.setDefaultValue("30");
        info.setDescription("round trip test");
        info.setSourceFile("Test.java");
        info.setFieldName("TEST");

        Path docDir = tempDir.newFolder("doc").toPath();
        List<GlobalConfigInfo> configs = Collections.singletonList(info);

        // generate
        GlobalConfigDocGenerator generator = new GlobalConfigDocGenerator();
        generator.generate(configs, docDir, true);

        // check should pass
        GlobalConfigDocChecker checker = new GlobalConfigDocChecker();
        GlobalConfigDocChecker.CheckResult result = checker.check(configs, docDir);
        assertTrue(result.passed());
    }

    @Test
    public void testSkipsNonGlobalConfigDefinition() throws Exception {
        String source =
                "package org.zstack.test;\n" +
                "public class NotAGlobalConfig {\n" +
                "    public static final String CATEGORY = \"test\";\n" +
                "    public static Object FOO = new Object();\n" +
                "}\n";

        File srcDir = createJavaFile("NotAGlobalConfig.java", source);
        GlobalConfigScanner scanner = new GlobalConfigScanner();
        List<GlobalConfigInfo> results = scanner.scan(Collections.singletonList(srcDir.toPath()));

        assertEquals(0, results.size());
    }
}
