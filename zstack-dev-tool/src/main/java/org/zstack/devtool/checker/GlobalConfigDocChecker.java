package org.zstack.devtool.checker;

import org.zstack.devtool.model.GlobalConfigInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GlobalConfigDocChecker {

    public static class CheckResult {
        public final List<GlobalConfigInfo> missing = new ArrayList<>();
        public final List<String> inconsistent = new ArrayList<>();
        public int total;

        public boolean passed() {
            return missing.isEmpty() && inconsistent.isEmpty();
        }

        public void print() {
            if (passed()) {
                System.out.println("[GlobalConfig] OK - " + total + " configs, all have docs");
                return;
            }

            if (!missing.isEmpty()) {
                System.out.println("[GlobalConfig] MISSING " + missing.size() + " doc(s):");
                for (GlobalConfigInfo info : missing) {
                    System.out.println("  - " + info.getCategory() + "/" + info.getName() + ".md");
                    System.out.println("    source: " + info.getSourceFile() + " field " + info.getFieldName());
                }
            }

            if (!inconsistent.isEmpty()) {
                System.out.println("[GlobalConfig] INCONSISTENT " + inconsistent.size() + " doc(s):");
                for (String msg : inconsistent) {
                    System.out.println("  - " + msg);
                }
            }
        }
    }

    public CheckResult check(List<GlobalConfigInfo> configs, Path docDir) {
        CheckResult result = new CheckResult();
        result.total = configs.size();

        for (GlobalConfigInfo config : configs) {
            Path mdPath = docDir.resolve(config.getCategory()).resolve(config.getName() + ".md");
            Path deprecatedPath = docDir.resolve(config.getCategory())
                    .resolve(config.getName() + "#Deprecated.md");

            if (Files.exists(deprecatedPath)) {
                continue; // deprecated, skip
            }

            if (!Files.exists(mdPath)) {
                result.missing.add(config);
                continue;
            }

            // check consistency of metadata
            try {
                String content = new String(Files.readAllBytes(mdPath), StandardCharsets.UTF_8);
                checkConsistency(config, content, mdPath, result);
            } catch (IOException e) {
                result.inconsistent.add(mdPath + ": cannot read - " + e.getMessage());
            }
        }

        return result;
    }

    private void checkConsistency(GlobalConfigInfo config, String content, Path mdPath, CheckResult result) {
        String relativePath = config.getCategory() + "/" + config.getName() + ".md";

        // check Type
        String expectedType = config.getType();
        if (expectedType != null && !content.contains(expectedType)) {
            result.inconsistent.add(relativePath + ": type mismatch, expected " + expectedType);
        }

        // check Category
        if (!content.contains(config.getCategory())) {
            result.inconsistent.add(relativePath + ": category mismatch, expected " + config.getCategory());
        }

        // check DefaultValue
        String expectedDefault = config.getDefaultValue();
        if (expectedDefault != null && !expectedDefault.isEmpty() && !content.contains(expectedDefault)) {
            result.inconsistent.add(relativePath + ": defaultValue mismatch, expected " + expectedDefault);
        }

        // check Name is in the file
        if (!content.contains(config.getName())) {
            result.inconsistent.add(relativePath + ": name mismatch, expected " + config.getName());
        }
    }
}
