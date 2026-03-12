package org.zstack.devtool.checker;

import org.zstack.devtool.model.ApiMessageInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiHelperChecker {

    public static class CheckResult {
        public final List<String> missingMethods = new ArrayList<>();
        public int totalMessages;
        public int totalMethods;

        public boolean passed() {
            return missingMethods.isEmpty();
        }

        public void print() {
            if (passed()) {
                System.out.println("[ApiHelper] OK - " + totalMethods +
                        " helper methods for " + totalMessages + " API messages");
                return;
            }

            System.out.println("[ApiHelper] FAIL - MISSING " + missingMethods.size() + " method(s):");
            for (String msg : missingMethods) {
                System.out.println("  - " + msg);
            }
            System.out.println();
            System.out.println("  Run: ./runMavenProfile apihelper");
        }
    }

    public CheckResult check(List<ApiMessageInfo> messages, Path apiHelperFile) {
        CheckResult result = new CheckResult();
        result.totalMessages = messages.size();

        if (!Files.exists(apiHelperFile)) {
            System.out.println("[ApiHelper] WARN - ApiHelper.groovy not found at " + apiHelperFile);
            result.totalMethods = 0;
            for (ApiMessageInfo msg : messages) {
                result.missingMethods.add(msg.getHelperMethodName() + " (from " + msg.getClassName() + ")");
            }
            return result;
        }

        try {
            String content = new String(Files.readAllBytes(apiHelperFile), StandardCharsets.UTF_8);

            // Extract method names from ApiHelper.groovy
            // Pattern: def methodName(
            Set<String> existingMethods = new HashSet<>();
            Pattern pattern = Pattern.compile("def\\s+(\\w+)\\s*\\(");
            Matcher matcher = pattern.matcher(content);
            while (matcher.find()) {
                existingMethods.add(matcher.group(1));
            }
            result.totalMethods = existingMethods.size();

            // Check each API message has a corresponding method
            for (ApiMessageInfo msg : messages) {
                String methodName = msg.getHelperMethodName();
                if (!existingMethods.contains(methodName)) {
                    result.missingMethods.add(methodName + " (from " + msg.getClassName() + ")");
                }
            }
        } catch (IOException e) {
            System.out.println("[ApiHelper] ERROR - Cannot read " + apiHelperFile + ": " + e.getMessage());
        }

        return result;
    }
}
