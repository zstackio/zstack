package org.zstack.devtool;

import org.zstack.devtool.checker.ApiHelperChecker;
import org.zstack.devtool.checker.GlobalConfigDocChecker;
import org.zstack.devtool.checker.SdkChecker;
import org.zstack.devtool.generator.GlobalConfigDocGenerator;
import org.zstack.devtool.model.ApiMessageInfo;
import org.zstack.devtool.model.GlobalConfigInfo;
import org.zstack.devtool.scanner.ApiMessageScanner;
import org.zstack.devtool.scanner.GlobalConfigScanner;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class DevTool {

    private final Path projectRoot;
    // cached scan results
    private List<ApiMessageInfo> cachedApiMessages;

    public DevTool(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        String target = args.length > 1 ? args[1] : "all";

        Path projectRoot = detectProjectRoot();
        if (projectRoot == null) {
            System.err.println("ERROR: Cannot find ZStack project root. Run from within the zstack directory.");
            System.exit(1);
        }

        DevTool tool = new DevTool(projectRoot);

        switch (command) {
            case "check":
                System.exit(tool.check(target) ? 0 : 1);
                break;
            case "generate":
                tool.generate(target);
                break;
            case "scan":
                tool.scan(target);
                break;
            default:
                System.err.println("Unknown command: " + command);
                printUsage();
                System.exit(1);
        }
    }

    public boolean check(String target) {
        boolean allPassed = true;

        if ("all".equals(target) || "globalconfig".equals(target)) {
            if (!checkGlobalConfig()) allPassed = false;
        }

        if ("all".equals(target) || "sdk".equals(target)) {
            if (!checkSdk()) allPassed = false;
        }

        if ("all".equals(target) || "apihelper".equals(target)) {
            if (!checkApiHelper()) allPassed = false;
        }

        if (allPassed) {
            System.out.println();
            System.out.println("All checks passed.");
        } else {
            System.out.println();
            System.out.println("Some checks FAILED. See above for details.");
        }

        return allPassed;
    }

    public void generate(String target) {
        if ("all".equals(target) || "globalconfig".equals(target)) {
            generateGlobalConfig();
        }

        // SDK and ApiHelper generation requires compilation (use ./runMavenProfile)
        if ("sdk".equals(target)) {
            System.out.println("[SDK] Generate not supported yet. Run: ./runMavenProfile sdk");
        }
        if ("apihelper".equals(target)) {
            System.out.println("[ApiHelper] Generate not supported yet. Run: ./runMavenProfile apihelper");
        }
    }

    public void scan(String target) {
        if ("all".equals(target) || "globalconfig".equals(target)) {
            scanGlobalConfig();
        }
        if ("all".equals(target) || "sdk".equals(target) || "apihelper".equals(target)) {
            scanApiMessages();
        }
    }

    // --- GlobalConfig ---

    private boolean checkGlobalConfig() {
        List<GlobalConfigInfo> configs = scanAllGlobalConfigs();
        if (configs.isEmpty()) {
            System.out.println("[GlobalConfig] WARN - no configs found. Check source directories.");
            return false;
        }

        Path docDir = resolveGlobalConfigDocDir();
        GlobalConfigDocChecker checker = new GlobalConfigDocChecker();
        GlobalConfigDocChecker.CheckResult result = checker.check(configs, docDir);
        result.print();
        return result.passed();
    }

    private void generateGlobalConfig() {
        List<GlobalConfigInfo> configs = scanAllGlobalConfigs();
        if (configs.isEmpty()) {
            System.out.println("[GlobalConfig] WARN - no configs found.");
            return;
        }

        Path docDir = resolveGlobalConfigDocDir();
        GlobalConfigDocGenerator generator = new GlobalConfigDocGenerator();
        int created = generator.generate(configs, docDir, true);
        System.out.println("[GlobalConfig] Generated " + created + " new doc(s), " +
                configs.size() + " total configs");
    }

    private void scanGlobalConfig() {
        List<GlobalConfigInfo> configs = scanAllGlobalConfigs();
        System.out.println("[GlobalConfig] Found " + configs.size() + " configs:");
        for (GlobalConfigInfo info : configs) {
            System.out.println("  " + info);
        }
    }

    private List<GlobalConfigInfo> scanAllGlobalConfigs() {
        long start = System.currentTimeMillis();
        List<Path> sourceDirs = getSourceDirs();
        GlobalConfigScanner scanner = new GlobalConfigScanner();
        List<GlobalConfigInfo> configs = scanner.scan(sourceDirs);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[GlobalConfig] Scanned " + sourceDirs.size() +
                " source dirs, found " + configs.size() + " configs in " + elapsed + "ms");
        return configs;
    }

    // --- SDK ---

    private boolean checkSdk() {
        List<ApiMessageInfo> messages = getApiMessages();
        if (messages.isEmpty()) {
            System.out.println("[SDK] WARN - no API messages found.");
            return false;
        }

        Path sdkDir = projectRoot.resolve("sdk/src/main/java/org/zstack/sdk");
        if (!Files.isDirectory(sdkDir)) {
            System.out.println("[SDK] WARN - SDK directory not found: " + sdkDir);
            return false;
        }

        SdkChecker checker = new SdkChecker();
        SdkChecker.CheckResult result = checker.check(messages, sdkDir);
        result.print();
        return result.passed();
    }

    // --- ApiHelper ---

    private boolean checkApiHelper() {
        List<ApiMessageInfo> messages = getApiMessages();
        if (messages.isEmpty()) {
            System.out.println("[ApiHelper] WARN - no API messages found.");
            return false;
        }

        Path apiHelperFile = projectRoot.resolve(
                "testlib/src/main/java/org/zstack/testlib/ApiHelper.groovy");
        if (!Files.exists(apiHelperFile)) {
            // try premium location
            apiHelperFile = projectRoot.resolve(
                    "premium/test-premium/src/main/groovy/org/zstack/testlib/ApiHelper.groovy");
        }

        ApiHelperChecker checker = new ApiHelperChecker();
        ApiHelperChecker.CheckResult result = checker.check(messages, apiHelperFile);
        result.print();
        return result.passed();
    }

    // --- API message scanning (shared by SDK + ApiHelper) ---

    private List<ApiMessageInfo> getApiMessages() {
        if (cachedApiMessages != null) return cachedApiMessages;

        long start = System.currentTimeMillis();
        List<Path> sourceDirs = getSourceDirs();
        ApiMessageScanner scanner = new ApiMessageScanner();
        cachedApiMessages = scanner.scan(sourceDirs);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("[API] Scanned " + sourceDirs.size() +
                " source dirs, found " + cachedApiMessages.size() + " API messages in " + elapsed + "ms");
        return cachedApiMessages;
    }

    private void scanApiMessages() {
        List<ApiMessageInfo> messages = getApiMessages();
        System.out.println("[API] Found " + messages.size() + " API messages:");
        for (ApiMessageInfo info : messages) {
            System.out.println("  " + info.getActionName() + " <- " + info.getClassName() +
                    " [" + info.getHttpMethod() + " " + info.getPath() + "]" +
                    " params=" + info.getParams().size());
        }
    }

    // --- Paths ---

    private Path resolveGlobalConfigDocDir() {
        Path premiumDoc = projectRoot.resolve("premium/doc/globalconfig");
        if (Files.isDirectory(premiumDoc)) return premiumDoc;
        Path doc = projectRoot.resolve("doc/globalconfig");
        if (Files.isDirectory(doc)) return doc;
        try {
            Files.createDirectories(premiumDoc);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create " + premiumDoc, e);
        }
        return premiumDoc;
    }

    private List<Path> getSourceDirs() {
        List<Path> dirs = new ArrayList<>();

        String[] mainDirs = {
                "header/src/main/java",
                "core/src/main/java",
                "compute/src/main/java",
                "storage/src/main/java",
                "network/src/main/java",
                "image/src/main/java",
                "identity/src/main/java",
                "search/src/main/java",
                "configuration/src/main/java",
                "rest/src/main/java",
                "console/src/main/java",
                "tag/src/main/java",
                "longjob/src/main/java",
                "externalservice/src/main/java",
                "resourceconfig/src/main/java",
        };

        for (String dir : mainDirs) {
            Path p = projectRoot.resolve(dir);
            if (Files.isDirectory(p)) dirs.add(p);
        }

        addPluginDirs(dirs, projectRoot.resolve("plugin"));

        Path premiumHeader = projectRoot.resolve("premium/premium-header/src/main/java");
        if (Files.isDirectory(premiumHeader)) dirs.add(premiumHeader);

        addPluginDirs(dirs, projectRoot.resolve("premium/plugin-premium"));

        return dirs;
    }

    private void addPluginDirs(List<Path> dirs, Path pluginRoot) {
        if (!Files.isDirectory(pluginRoot)) return;
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(pluginRoot);
            for (Path child : stream) {
                if (Files.isDirectory(child)) {
                    Path src = child.resolve("src/main/java");
                    if (Files.isDirectory(src)) {
                        dirs.add(src);
                    }
                }
            }
            stream.close();
        } catch (IOException e) {
            System.err.println("WARN: Failed to scan plugin dirs in " + pluginRoot);
        }
    }

    static Path detectProjectRoot() {
        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path current = cwd;
        for (int i = 0; i < 10; i++) {
            Path pom = current.resolve("pom.xml");
            Path header = current.resolve("header");
            Path core = current.resolve("core");
            if (Files.exists(pom) && Files.isDirectory(header) && Files.isDirectory(core)) {
                return current;
            }
            Path parent = current.getParent();
            if (parent == null) break;
            current = parent;
        }
        return null;
    }

    static void printUsage() {
        System.out.println("Usage: dev-tool <command> [target]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  check [globalconfig|sdk|apihelper|all]     Check if generated files are up to date");
        System.out.println("  generate [globalconfig|all]                Generate missing files");
        System.out.println("  scan [globalconfig|sdk|apihelper|all]      List all scanned items (debug)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  dev-tool check all              Check all generated files");
        System.out.println("  dev-tool check globalconfig     Check GlobalConfig docs only");
        System.out.println("  dev-tool check sdk              Check SDK action files only");
        System.out.println("  dev-tool check apihelper         Check ApiHelper.groovy methods");
        System.out.println("  dev-tool generate globalconfig  Generate missing GlobalConfig docs");
    }
}
