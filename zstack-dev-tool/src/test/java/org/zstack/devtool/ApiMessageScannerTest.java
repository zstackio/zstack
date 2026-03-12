package org.zstack.devtool;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.zstack.devtool.checker.ApiHelperChecker;
import org.zstack.devtool.checker.SdkChecker;
import org.zstack.devtool.model.ApiMessageInfo;
import org.zstack.devtool.model.ApiParamInfo;
import org.zstack.devtool.scanner.ApiMessageScanner;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ApiMessageScannerTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private File createJavaFile(String subdir, String filename, String content) throws Exception {
        File dir = tempDir.newFolder(subdir, "src", "main", "java");
        File file = new File(dir, filename);
        try (FileWriter w = new FileWriter(file)) {
            w.write(content);
        }
        return dir;
    }

    @Test
    public void testScanBasicRestRequest() throws Exception {
        String source =
                "package org.zstack.test;\n" +
                "import org.zstack.header.rest.RestRequest;\n" +
                "import org.zstack.header.message.APIParam;\n" +
                "@RestRequest(path = \"/zones\", method = HttpMethod.POST, responseClass = APICreateZoneEvent.class)\n" +
                "public class APICreateZoneMsg extends APICreateMessage {\n" +
                "    @APIParam(maxLength = 255)\n" +
                "    private String name;\n" +
                "    @APIParam(required = false, maxLength = 2048)\n" +
                "    private String description;\n" +
                "}\n";

        File srcDir = createJavaFile("zone", "APICreateZoneMsg.java", source);
        ApiMessageScanner scanner = new ApiMessageScanner();
        List<ApiMessageInfo> results = scanner.scan(Collections.singletonList(srcDir.toPath()));

        assertEquals(1, results.size());
        ApiMessageInfo info = results.get(0);
        assertEquals("APICreateZoneMsg", info.getClassName());
        assertEquals("/zones", info.getPath());
        assertEquals("POST", info.getHttpMethod());
        assertEquals("CreateZoneAction", info.getActionName());
        assertEquals("createZone", info.getHelperMethodName());
        assertEquals(2, info.getParams().size());
    }

    @Test
    public void testSkipsAbstractClass() throws Exception {
        String source =
                "package org.zstack.test;\n" +
                "import org.zstack.header.rest.RestRequest;\n" +
                "@RestRequest(path = \"/base\", method = HttpMethod.GET)\n" +
                "public abstract class APIBaseMsg extends APIMessage {\n" +
                "}\n";

        File srcDir = createJavaFile("base", "APIBaseMsg.java", source);
        ApiMessageScanner scanner = new ApiMessageScanner();
        List<ApiMessageInfo> results = scanner.scan(Collections.singletonList(srcDir.toPath()));

        assertEquals(0, results.size());
    }

    @Test
    public void testSkipsNoSDK() throws Exception {
        String source =
                "package org.zstack.test;\n" +
                "import org.zstack.header.rest.RestRequest;\n" +
                "@RestRequest(path = \"/internal\", method = HttpMethod.POST)\n" +
                "@NoSDK\n" +
                "public class APIInternalMsg extends APIMessage {\n" +
                "}\n";

        File srcDir = createJavaFile("internal", "APIInternalMsg.java", source);
        ApiMessageScanner scanner = new ApiMessageScanner();
        List<ApiMessageInfo> results = scanner.scan(Collections.singletonList(srcDir.toPath()));

        assertEquals(0, results.size());
    }

    @Test
    public void testApiParamDetails() throws Exception {
        String source =
                "package org.zstack.test;\n" +
                "import org.zstack.header.rest.RestRequest;\n" +
                "import org.zstack.header.message.APIParam;\n" +
                "@RestRequest(path = \"/vms\", method = HttpMethod.POST)\n" +
                "public class APICreateVmMsg extends APIMessage {\n" +
                "    @APIParam(required = true, maxLength = 255)\n" +
                "    private String name;\n" +
                "    @APIParam(required = false, validValues = {\"Linux\", \"Windows\"})\n" +
                "    private String platform;\n" +
                "    private String internalField;\n" +  // no @APIParam -> excluded
                "}\n";

        File srcDir = createJavaFile("vm", "APICreateVmMsg.java", source);
        ApiMessageScanner scanner = new ApiMessageScanner();
        List<ApiMessageInfo> results = scanner.scan(Collections.singletonList(srcDir.toPath()));

        assertEquals(1, results.size());
        ApiMessageInfo info = results.get(0);
        // only fields with @APIParam are included
        assertEquals(2, info.getParams().size());

        ApiParamInfo nameParam = info.getParams().stream()
                .filter(p -> "name".equals(p.getFieldName())).findFirst().orElse(null);
        assertNotNull(nameParam);
        assertTrue(nameParam.isRequired());
        assertEquals(255, nameParam.getMaxLength());

        ApiParamInfo platformParam = info.getParams().stream()
                .filter(p -> "platform".equals(p.getFieldName())).findFirst().orElse(null);
        assertNotNull(platformParam);
        assertFalse(platformParam.isRequired());
    }

    @Test
    public void testActionNameDerivation() {
        ApiMessageInfo info = new ApiMessageInfo();
        info.setClassName("APICreateZoneMsg");
        assertEquals("CreateZoneAction", info.getActionName());
        assertEquals("CreateZoneResult", info.getResultName());
        assertEquals("createZone", info.getHelperMethodName());

        info.setClassName("APIQueryVmInstanceMsg");
        assertEquals("QueryVmInstanceAction", info.getActionName());
        assertEquals("queryVmInstance", info.getHelperMethodName());
    }

    @Test
    public void testSdkCheckerFieldMismatchFails() throws Exception {
        // Create a fake SDK action file missing a field
        Path sdkDir = tempDir.newFolder("sdk").toPath();
        Files.write(sdkDir.resolve("CreateTestAction.java"),
                ("public class CreateTestAction {\n" +
                 "    public java.lang.String name;\n" +
                 "    public java.lang.String sessionId;\n" +
                 "}\n").getBytes());

        ApiMessageInfo msg = new ApiMessageInfo();
        msg.setClassName("APICreateTestMsg");
        msg.setSourceFile("Test.java");
        ApiParamInfo nameParam = new ApiParamInfo();
        nameParam.setFieldName("name");
        ApiParamInfo descParam = new ApiParamInfo();
        descParam.setFieldName("description");
        msg.setParams(java.util.Arrays.asList(nameParam, descParam));

        SdkChecker checker = new SdkChecker();
        SdkChecker.CheckResult result = checker.check(Collections.singletonList(msg), sdkDir);

        // description is in source but not in SDK -> should fail
        assertFalse(result.passed());
        assertEquals(1, result.fieldMismatches.size());
        assertTrue(result.fieldMismatches.get(0).contains("description"));
    }

    @Test
    public void testApiHelperCheckerMissingMethodFails() throws Exception {
        Path helperFile = tempDir.newFolder("testlib").toPath().resolve("ApiHelper.groovy");
        Files.write(helperFile,
                ("class ApiHelper {\n" +
                 "    def createZone(Map args) { }\n" +
                 "    def deleteZone(Map args) { }\n" +
                 "}\n").getBytes());

        ApiMessageInfo msg1 = new ApiMessageInfo();
        msg1.setClassName("APICreateZoneMsg");
        ApiMessageInfo msg2 = new ApiMessageInfo();
        msg2.setClassName("APIUpdateZoneMsg");  // updateZone not in helper

        ApiHelperChecker checker = new ApiHelperChecker();
        ApiHelperChecker.CheckResult result = checker.check(
                java.util.Arrays.asList(msg1, msg2), helperFile);

        assertFalse(result.passed());
        assertEquals(1, result.missingMethods.size());
        assertTrue(result.missingMethods.get(0).contains("updateZone"));
    }
}
