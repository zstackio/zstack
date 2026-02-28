package scripts

import org.zstack.rest.sdk.SdkFile
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

class GoTestTemplate {
    private static final CLogger logger = Utils.getLogger(GoTestTemplate.class)

    private def inventoryGenerator  // GoInventory instance
    private def allApiTemplates
    private def inventories
    private Set<String> generatedIntegrationFiles = new HashSet<>()

    GoTestTemplate(def inventoryGenerator, def allApiTemplates, def inventories) {
        this.inventoryGenerator = inventoryGenerator
        this.allApiTemplates = allApiTemplates
        this.inventories = inventories
    }

    List<SdkFile> generate() {
        def files = []
        logger.warn("[GoSDK-Test] Starting test generation...")

        // 1. Static base unit test file
        files.add(generateBaseUnitTestFile())

        // 2. Static base integration test file
        files.add(generateBaseIntegrationTestFile())

        // 2. Per-resource test files
        def resourceMap = groupApisByResource()
        logger.warn("[GoSDK-Test] Grouped ${resourceMap.size()} resources for test generation")

        resourceMap.each { String prefix, Map resourceInfo ->
            String snakeName = toSnakeCase(prefix)

            // Unit tests
            def paramTest = generateParamTestFile(prefix, snakeName, resourceInfo)
            if (paramTest != null) files.add(paramTest)

            def viewTest = generateViewTestFile(prefix, snakeName, resourceInfo)
            if (viewTest != null) files.add(viewTest)

            // TODO: client tests require parsing actual method signatures from GoApiTemplate
            // Will be added in a follow-up iteration
            // def clientTest = generateClientTestFile(prefix, snakeName, resourceInfo)
            // if (clientTest != null) files.add(clientTest)

            // Integration tests (generated to pkg/integration_test/ to avoid conflicts)
            def integrationTest = generateIntegrationTestFile(prefix, snakeName, resourceInfo)
            if (integrationTest != null) files.add(integrationTest)
        }

        logger.warn("[GoSDK-Test] Test generation complete. Generated ${files.size()} test files")
        return files
    }

    // ======================== Resource Grouping ========================

    private Map groupApisByResource() {
        def resourceMap = [:]

        inventories.each { Class<?> inventoryClass ->
            String prefix = inventoryClass.simpleName.replaceAll('Inventory\$', '')
            if (!resourceMap.containsKey(prefix)) {
                resourceMap[prefix] = [
                    inventoryClass: inventoryClass,
                    viewStructName: inventoryGenerator.getViewStructName(inventoryClass),
                    templates: []
                ]
            }
        }

        allApiTemplates.each { template ->
            String resName = template.getResourceName()
            if (resName != null && resourceMap.containsKey(resName)) {
                resourceMap[resName].templates.add(template)
            }
        }

        // Filter out resources with no templates
        return resourceMap.findAll { k, v -> !v.templates.isEmpty() }
    }

    // ======================== Base Unit Test ========================

    private SdkFile generateBaseUnitTestFile() {
        def content = new StringBuilder()
        content.append('''\
// Copyright (c) ZStack.io, Inc.
// Auto-generated test infrastructure. DO NOT EDIT.

package unit_test

import (
\t"encoding/json"
\t"fmt"
\t"net/http"
\t"net/http/httptest"
\t"strings"
\t"testing"
\t"time"

\t"github.com/zstackio/zsphere-sdk-go-v2/pkg/client"
)

// newMockClient creates a ZSClient backed by an httptest server.
// The handler receives all HTTP requests and can assert on method/path/body.
func newMockClient(handler http.HandlerFunc) (*client.ZSClient, func()) {
\tserver := httptest.NewServer(handler)
\t// Parse host and port from server URL
\taddr := server.Listener.Addr().String()
\tparts := strings.SplitN(addr, ":", 2)
\thost := parts[0]
\tport := 80
\tif len(parts) == 2 {
\t\tfmt.Sscanf(parts[1], "%d", &port)
\t}

\tconfig := client.NewZSConfig(host, port, "")
\tconfig.LoginAccount("admin", "password")
\tcli := client.NewZSClient(config)
\tcli.LoadSession("mock-session-id")
\treturn cli, server.Close
}

// mockInventoryResponse builds a JSON response wrapping data in {"inventory": ...}
func mockInventoryResponse(data map[string]interface{}) []byte {
\tresp := map[string]interface{}{"inventory": data}
\tb, _ := json.Marshal(resp)
\treturn b
}

// mockInventoriesResponse builds a JSON response wrapping data in {"inventories": [...]}
func mockInventoriesResponse(items ...map[string]interface{}) []byte {
\tresp := map[string]interface{}{"inventories": items}
\tb, _ := json.Marshal(resp)
\treturn b
}

// stringPtr returns a pointer to the given string.
func stringPtr(s string) *string {
\treturn &s
}

// timePtr parses a time string and returns a pointer.
func timePtr(s string) *time.Time {
\tt, _ := time.Parse(time.RFC3339, s)
\treturn &t
}

// assertEqual is a simple test helper.
func assertEqual(t *testing.T, expected, actual interface{}) {
\tt.Helper()
\tif expected != actual {
\t\tt.Errorf("expected %v, got %v", expected, actual)
\t}
}

// assertNoError fails the test if err is not nil.
func assertNoError(t *testing.T, err error) {
\tt.Helper()
\tif err != nil {
\t\tt.Fatalf("unexpected error: %v", err)
\t}
}

// assertContains checks that s contains substr.
func assertContains(t *testing.T, s, substr string) {
\tt.Helper()
\tif !strings.Contains(s, substr) {
\t\tt.Errorf("expected %q to contain %q", s, substr)
\t}
}
''')

        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/unit_test/"
        sdkFile.fileName = "base_unit_test.go"
        sdkFile.content = content.toString()
        return sdkFile
    }

    // ======================== Param Tests ========================

    private SdkFile generateParamTestFile(String prefix, String snakeName, Map resourceInfo) {
        def templates = resourceInfo.templates
        def paramTemplates = templates.findAll { t ->
            !t.isQueryMessage() && t.getActionType() in ['Create', 'Update', 'Add']
        }
        if (paramTemplates.isEmpty()) return null

        def content = new StringBuilder()
        content.append("// Copyright (c) ZStack.io, Inc.\n")
        content.append("// Auto-generated param tests. DO NOT EDIT.\n\n")
        content.append("package unit_test\n\n")
        content.append("import (\n")
        content.append("\t\"encoding/json\"\n")
        content.append("\t\"testing\"\n\n")
        content.append("\t\"github.com/zstackio/zsphere-sdk-go-v2/pkg/param\"\n")
        content.append(")\n\n")

        paramTemplates.each { template ->
            String paramStruct = template.getParamStructName()
            String detailStruct = template.getDetailParamStructName()
            String methodName = template.clzName

            // Marshal test - zero value should produce valid JSON
            content.append("func Test${methodName}Param_MarshalJSON(t *testing.T) {\n")
            content.append("\tp := param.${paramStruct}{}\n")
            content.append("\tdata, err := json.Marshal(p)\n")
            content.append("\tassertNoError(t, err)\n")
            content.append("\tif len(data) == 0 {\n")
            content.append("\t\tt.Fatal(\"marshaled JSON should not be empty\")\n")
            content.append("\t}\n")
            content.append("\t// Verify it's valid JSON\n")
            content.append("\tvar raw map[string]interface{}\n")
            content.append("\tassertNoError(t, json.Unmarshal(data, &raw))\n")
            content.append("}\n\n")

            // Unmarshal test - minimal JSON should parse without error
            content.append("func Test${methodName}Param_UnmarshalJSON(t *testing.T) {\n")
            content.append("\tjsonStr := `{}`\n")
            content.append("\tvar p param.${paramStruct}\n")
            content.append("\terr := json.Unmarshal([]byte(jsonStr), &p)\n")
            content.append("\tassertNoError(t, err)\n")
            content.append("}\n\n")
        }

        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/unit_test/"
        sdkFile.fileName = "${snakeName}_param_test.go"
        sdkFile.content = content.toString()
        return sdkFile
    }

    // ======================== View Tests ========================

    private SdkFile generateViewTestFile(String prefix, String snakeName, Map resourceInfo) {
        String viewStructName = resourceInfo.viewStructName
        if (viewStructName == null) return null

        def content = new StringBuilder()
        content.append("// Copyright (c) ZStack.io, Inc.\n")
        content.append("// Auto-generated view tests. DO NOT EDIT.\n\n")
        content.append("package unit_test\n\n")
        content.append("import (\n")
        content.append("\t\"encoding/json\"\n")
        content.append("\t\"testing\"\n\n")
        content.append("\t\"github.com/zstackio/zsphere-sdk-go-v2/pkg/view\"\n")
        content.append(")\n\n")

        // InventoryView unmarshal test
        content.append("func Test${viewStructName}_UnmarshalJSON(t *testing.T) {\n")
        content.append("\tjsonStr := `{\n")
        content.append("\t\t\"uuid\": \"test-uuid-001\",\n")
        content.append("\t\t\"name\": \"test-${snakeName}\",\n")
        content.append("\t\t\"createDate\": \"2024-01-01T00:00:00.000+08:00\",\n")
        content.append("\t\t\"lastOpDate\": \"2024-01-01T00:00:00.000+08:00\"\n")
        content.append("\t}`\n")
        content.append("\tvar v view.${viewStructName}\n")
        content.append("\terr := json.Unmarshal([]byte(jsonStr), &v)\n")
        content.append("\tassertNoError(t, err)\n")
        content.append("}\n\n")

        // Empty JSON should not error
        content.append("func Test${viewStructName}_UnmarshalEmpty(t *testing.T) {\n")
        content.append("\tvar v view.${viewStructName}\n")
        content.append("\terr := json.Unmarshal([]byte(`{}`), &v)\n")
        content.append("\tassertNoError(t, err)\n")
        content.append("}\n\n")

        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/unit_test/"
        sdkFile.fileName = "${snakeName}_view_test.go"
        sdkFile.content = content.toString()
        return sdkFile
    }

    // ======================== Client Tests ========================

    private SdkFile generateClientTestFile(String prefix, String snakeName, Map resourceInfo) {
        def templates = resourceInfo.templates
        if (templates.isEmpty()) return null

        String viewStructName = resourceInfo.viewStructName

        def content = new StringBuilder()
        content.append("// Copyright (c) ZStack.io, Inc.\n")
        content.append("// Auto-generated client tests. DO NOT EDIT.\n\n")
        content.append("package unit_test\n\n")
        content.append("import (\n")
        content.append("\t\"net/http\"\n")
        content.append("\t\"testing\"\n\n")
        content.append("\t\"github.com/zstackio/zsphere-sdk-go-v2/pkg/param\"\n")
        content.append(")\n\n")
        content.append("var _ = param.BaseParam{} // avoid unused import\n\n")

        templates.each { template ->
            String actionType = template.getActionType()
            String methodName = template.clzName

            if (template.isQueryMessage()) {
                // Query test
                content.append(generateQueryClientTest(prefix, methodName, snakeName))
                // Get test (derived from Query)
                String getMethodName = methodName.replaceFirst('^Query', 'Get')
                content.append(generateGetClientTest(prefix, getMethodName, snakeName))
            } else if (actionType == 'Create' || actionType == 'Add') {
                content.append(generateCreateClientTest(prefix, methodName, snakeName, template.getParamStructName()))
            } else if (actionType == 'Update' || actionType == 'Change') {
                content.append(generateUpdateClientTest(prefix, methodName, snakeName, template.getParamStructName()))
            } else if (actionType == 'Delete') {
                content.append(generateDeleteClientTest(prefix, methodName, snakeName))
            }
        }

        if (content.toString().count("func Test") == 0) return null

        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/unit_test/"
        sdkFile.fileName = "${snakeName}_client_test.go"
        sdkFile.content = content.toString()
        return sdkFile
    }

    private String generateQueryClientTest(String prefix, String methodName, String snakeName) {
        return """\
func Test${methodName}_Client(t *testing.T) {
\tcli, cleanup := newMockClient(func(w http.ResponseWriter, r *http.Request) {
\t\tassertEqual(t, http.MethodGet, r.Method)
\t\tw.WriteHeader(http.StatusOK)
\t\tw.Write(mockInventoriesResponse(map[string]interface{}{
\t\t\t"uuid": "test-uuid-001",
\t\t\t"name": "test-${snakeName}",
\t\t}))
\t})
\tdefer cleanup()
ctx := context.Background()
\tqueryParam := param.NewQueryParam()
\tresult, err := cli.${methodName}(ctx, &queryParam)
\tassertNoError(t, err)
\tif len(result) == 0 {
\t\tt.Fatal("expected at least one result")
\t}
\tassertEqual(t, "test-uuid-001", result[0].UUID)
}

"""
    }

    private String generateGetClientTest(String prefix, String methodName, String snakeName) {
        return """\
func Test${methodName}_Client(t *testing.T) {
\tcli, cleanup := newMockClient(func(w http.ResponseWriter, r *http.Request) {
\t\tassertEqual(t, http.MethodGet, r.Method)
\t\tw.WriteHeader(http.StatusOK)
\t\tw.Write(mockInventoriesResponse(map[string]interface{}{
\t\t\t"uuid": "test-uuid-001",
\t\t\t"name": "test-${snakeName}",
\t\t}))
\t})
\tdefer cleanup()

\tresult, err := cli.${methodName}("test-uuid-001")
\tassertNoError(t, err)
\tassertEqual(t, "test-uuid-001", result.UUID)
}

"""
    }

    private String generateCreateClientTest(String prefix, String methodName, String snakeName, String paramStruct) {
        return """\
func Test${methodName}_Client(t *testing.T) {
\tcli, cleanup := newMockClient(func(w http.ResponseWriter, r *http.Request) {
\t\tassertEqual(t, http.MethodPost, r.Method)
\t\tw.WriteHeader(http.StatusOK)
\t\tw.Write(mockInventoryResponse(map[string]interface{}{
\t\t\t"uuid": "new-uuid-001",
\t\t\t"name": "test-${snakeName}",
\t\t}))
\t})
\tdefer cleanup()

\tresult, err := cli.${methodName}(param.${paramStruct}{})
\tassertNoError(t, err)
\tassertEqual(t, "new-uuid-001", result.UUID)
}

"""
    }

    private String generateUpdateClientTest(String prefix, String methodName, String snakeName, String paramStruct) {
        return """\
func Test${methodName}_Client(t *testing.T) {
\tcli, cleanup := newMockClient(func(w http.ResponseWriter, r *http.Request) {
\t\tassertEqual(t, http.MethodPut, r.Method)
\t\tw.WriteHeader(http.StatusOK)
\t\tw.Write(mockInventoryResponse(map[string]interface{}{
\t\t\t"uuid": "test-uuid-001",
\t\t\t"name": "updated-${snakeName}",
\t\t}))
\t})
\tdefer cleanup()

\tresult, err := cli.${methodName}("test-uuid-001", param.${paramStruct}{})
\tassertNoError(t, err)
\tassertEqual(t, "test-uuid-001", result.UUID)
}

"""
    }

    private String generateDeleteClientTest(String prefix, String methodName, String snakeName) {
        return """\
func Test${methodName}_Client(t *testing.T) {
\tcli, cleanup := newMockClient(func(w http.ResponseWriter, r *http.Request) {
\t\tassertEqual(t, http.MethodDelete, r.Method)
\t\tw.WriteHeader(http.StatusOK)
\t\tw.Write([]byte(`{}`))
\t})
\tdefer cleanup()

\terr := cli.${methodName}("test-uuid-001", param.DeleteModePermissive)
\tassertNoError(t, err)
}

"""
    }

    // ======================== Integration Tests ========================

    private SdkFile generateBaseIntegrationTestFile() {
        def content = new StringBuilder()
        content.append('''\
// Copyright (c) ZStack.io, Inc.
// Auto-generated integration test infrastructure. DO NOT EDIT.

package integration_test

import (
\t"context"
\t"os"
\t"testing"

\t"github.com/kataras/golog"
\t"github.com/zstackio/zsphere-sdk-go-v2/pkg/client"
)

const (
\tdefaultHostname = "localhost"
\tdefaultAccount  = "admin"
\tdefaultPassword = "password"
)

var testCli *client.ZSClient

func TestMain(m *testing.M) {
\tctx := context.Background()
\thostname := os.Getenv("ZSTACK_HOST")
\tif hostname == "" {
\t\thostname = defaultHostname
\t}
\taccount := os.Getenv("ZSTACK_ACCOUNT")
\tif account == "" {
\t\taccount = defaultAccount
\t}
\tpassword := os.Getenv("ZSTACK_PASSWORD")
\tif password == "" {
\t\tpassword = defaultPassword
\t}

\tconfig := client.DefaultZSConfig(hostname).
\t\tLoginAccount(account, password).
\t\tDebug(true)
\ttestCli = client.NewZSClient(config)

\t_, err := testCli.Login(ctx)
\tif err != nil {
\t\tgolog.Errorf("Integration test login failed: %v", err)
\t\tos.Exit(1)
\t}
\tdefer testCli.Logout(ctx)

\tos.Exit(m.Run())
}
''')

        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/integration_test/"
        sdkFile.fileName = "base_test.go"
        sdkFile.content = content.toString()
        return sdkFile
    }

    private SdkFile generateIntegrationTestFile(String prefix, String snakeName, Map resourceInfo) {
        def templates = resourceInfo.templates

        // Only generate for resources that have a Query API
        def queryTemplate = templates.find { it.isQueryMessage() }
        if (queryTemplate == null) return null

        String fileName = "${snakeName}_query_test.go"

        // Track generated filenames to avoid collisions within this run
        if (generatedIntegrationFiles.contains(fileName)) {
            logger.warn("[GoSDK-Test] Skipping duplicate integration test file: ${fileName}")
            return null
        }
        generatedIntegrationFiles.add(fileName)

        String methodName = queryTemplate.clzName  // e.g. "QueryCluster"

        def content = new StringBuilder()
        content.append("// Copyright (c) ZStack.io, Inc.\n")
        content.append("// Auto-generated integration tests. DO NOT EDIT.\n\n")
        content.append("package integration_test\n\n")
        content.append("import (\n")
        content.append("\t\"context\"\n")
        content.append("\t\"testing\"\n\n")
        content.append("\t\"github.com/kataras/golog\"\n\n")
        content.append("\t\"github.com/zstackio/zsphere-sdk-go-v2/pkg/param\"\n")
        content.append(")\n\n")

        // Query test
        content.append("func Test${methodName}(t *testing.T) {\n")
        content.append("\tctx := context.Background()\n")
        content.append("\tqueryParam := param.NewQueryParam()\n")
        content.append("\tresult, err := testCli.${methodName}(ctx, &queryParam)\n")
        content.append("\tif err != nil {\n")
        content.append("\t\tt.Errorf(\"Test${methodName} error: %v\", err)\n")
        content.append("\t\treturn\n")
        content.append("\t}\n")
        content.append("\tgolog.Infof(\"${methodName} result count: %d\", len(result))\n")
        content.append("}\n\n")

        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/integration_test/"
        sdkFile.fileName = fileName
        sdkFile.content = content.toString()
        return sdkFile
    }

    // ======================== Utilities ========================

    private String toSnakeCase(String name) {
        return name.replaceAll('([a-z])([A-Z])', '$1_$2').toLowerCase()
    }
}
