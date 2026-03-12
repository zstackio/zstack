package org.zstack.devtool.checker;

import org.zstack.devtool.model.ApiMessageInfo;
import org.zstack.devtool.model.ApiParamInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class SdkChecker {

    public static class CheckResult {
        public final List<String> missingActions = new ArrayList<>();
        public final List<String> extraActions = new ArrayList<>();
        public final List<String> fieldMismatches = new ArrayList<>();
        public int totalMessages;
        public int totalSdkFiles;

        public boolean passed() {
            return fieldMismatches.isEmpty();
        }

        public void print() {
            if (!missingActions.isEmpty()) {
                System.out.println("[SDK] INFO - " + missingActions.size() +
                        " API message(s) have no SDK action file (may be excluded by @NoSDK):");
                for (String msg : missingActions) {
                    System.out.println("  - " + msg);
                }
            }

            if (!fieldMismatches.isEmpty()) {
                System.out.println("[SDK] FAIL - " + fieldMismatches.size() + " action(s) out of sync:");
                for (String msg : fieldMismatches) {
                    System.out.println("  - " + msg);
                }
                System.out.println();
                System.out.println("  Run: ./runMavenProfile sdk");
            }

            if (passed()) {
                System.out.println("[SDK] OK - " + totalMessages + " API messages, " +
                        totalSdkFiles + " SDK action files" +
                        (missingActions.isEmpty() ? ", all in sync" :
                                ", " + missingActions.size() + " without action file (advisory)"));
            }
        }
    }

    public CheckResult check(List<ApiMessageInfo> messages, Path sdkDir) {
        CheckResult result = new CheckResult();
        result.totalMessages = messages.size();

        // count existing SDK action files
        try {
            result.totalSdkFiles = (int) Files.list(sdkDir)
                    .filter(p -> p.getFileName().toString().endsWith("Action.java"))
                    .count();
        } catch (IOException e) {
            result.totalSdkFiles = 0;
        }

        for (ApiMessageInfo msg : messages) {
            String actionName = msg.getActionName();
            Path actionFile = findActionFile(sdkDir, actionName);

            if (actionFile == null) {
                result.missingActions.add(actionName + " (from " + msg.getClassName() +
                        " in " + shortenPath(msg.getSourceFile()) + ")");
                continue;
            }

            // compare fields
            checkFields(msg, actionFile, result);
        }

        return result;
    }

    private Path findActionFile(Path sdkDir, String actionName) {
        // Check default location: sdk/src/main/java/org/zstack/sdk/ActionName.java
        Path defaultPath = sdkDir.resolve(actionName + ".java");
        if (Files.exists(defaultPath)) return defaultPath;

        // Check subdirectories (some actions are in sub-packages)
        try {
            Optional<Path> found = Files.walk(sdkDir)
                    .filter(p -> p.getFileName().toString().equals(actionName + ".java"))
                    .findFirst();
            return found.orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private void checkFields(ApiMessageInfo msg, Path actionFile, CheckResult result) {
        try {
            String content = new String(Files.readAllBytes(actionFile), StandardCharsets.UTF_8);

            // Get non-NoSee, non-inherited API params from source (own fields only)
            Set<String> sourceFields = msg.getParams().stream()
                    .filter(p -> !p.isNoSee() && !p.isInherited())
                    .map(ApiParamInfo::getFieldName)
                    .collect(Collectors.toSet());

            // Parse @Param fields from SDK action file
            Set<String> sdkFields = extractSdkFields(content);

            // Remove credential/framework fields always present in SDK base classes
            Set<String> frameworkFields = new HashSet<>(Arrays.asList(
                    "sessionId", "accessKeyId", "accessKeySecret", "requestIp",
                    "systemTags", "userTags", "timeout", "pollingInterval"
            ));
            sdkFields.removeAll(frameworkFields);

            Set<String> missingInSdk = new HashSet<>(sourceFields);
            missingInSdk.removeAll(sdkFields);

            if (!missingInSdk.isEmpty()) {
                result.fieldMismatches.add(msg.getActionName() + ": source has fields not in SDK: " +
                        missingInSdk);
            }
        } catch (IOException e) {
            // can't read file, skip field check
        }
    }

    private Set<String> extractSdkFields(String content) {
        Set<String> fields = new HashSet<>();
        // Match: public java.lang.String fieldName;
        // or: public java.util.List fieldName;
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("public ") && line.endsWith(";") &&
                    !line.contains("(") && !line.contains("class ") &&
                    !line.contains("static ")) {
                // extract field name (handle initializers like "sshPort = 22")
                String withoutSemicolon = line.substring(0, line.length() - 1).trim();
                // strip initializer: "int sshPort = 22" -> "int sshPort"
                int eqIdx = withoutSemicolon.indexOf('=');
                if (eqIdx > 0) {
                    withoutSemicolon = withoutSemicolon.substring(0, eqIdx).trim();
                }
                int lastSpace = withoutSemicolon.lastIndexOf(' ');
                if (lastSpace > 0) {
                    String fieldName = withoutSemicolon.substring(lastSpace + 1);
                    fields.add(fieldName);
                }
            }
        }
        return fields;
    }

    private String shortenPath(String path) {
        if (path == null) return "unknown";
        int srcIdx = path.indexOf("src/main/java/");
        if (srcIdx >= 0) return path.substring(srcIdx + 14);
        return path;
    }
}
