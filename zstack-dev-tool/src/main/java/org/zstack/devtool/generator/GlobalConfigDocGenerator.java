package org.zstack.devtool.generator;

import org.zstack.devtool.model.GlobalConfigInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class GlobalConfigDocGenerator {

    public int generate(List<GlobalConfigInfo> configs, Path outputDir, boolean createOnly) {
        int created = 0;

        for (GlobalConfigInfo config : configs) {
            Path mdPath = outputDir.resolve(config.getCategory()).resolve(config.getName() + ".md");

            if (createOnly && Files.exists(mdPath)) {
                continue;
            }

            // also check for deprecated version
            Path deprecatedPath = outputDir.resolve(config.getCategory())
                    .resolve(config.getName() + "#Deprecated.md");
            if (Files.exists(deprecatedPath)) {
                continue;
            }

            try {
                Files.createDirectories(mdPath.getParent());
                String content = generateMarkdown(config);
                Files.write(mdPath, content.getBytes(StandardCharsets.UTF_8));
                created++;
                System.out.println("  Created: " + outputDir.relativize(mdPath));
            } catch (IOException e) {
                System.err.println("  ERROR: Failed to write " + mdPath + ": " + e.getMessage());
            }
        }

        return created;
    }

    public static String generateMarkdown(GlobalConfigInfo config) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n## Name\n\n```\n");
        sb.append(config.getName()).append("(##中文名-必填##)");
        sb.append("\n```\n\n");

        sb.append("### Description\n\n```\n");
        sb.append(config.getDescription() != null ? config.getDescription() : "");
        sb.append("\n```\n\n");

        sb.append("### 含义\n\n```\n");
        sb.append("##该条目的作用是什么-必填##");
        sb.append("\n```\n\n");

        sb.append("### Type\n\n```\n");
        sb.append(config.getType());
        sb.append("\n```\n\n");

        sb.append("### Category\n\n```\n");
        sb.append(config.getCategory());
        sb.append("\n```\n\n");

        sb.append("### 取值范围\n\n```\n");
        sb.append(config.getValueRange());
        sb.append("\n```\n\n");

        sb.append("### 取值范围补充说明\n\n```\n");
        sb.append("##对取值范围的解读-如无需写：无##");
        sb.append("\n```\n\n");

        sb.append("### DefaultValue\n\n```\n");
        sb.append(config.getDefaultValue());
        sb.append("\n```\n\n");

        sb.append("### 默认值补充说明\n\n```\n");
        sb.append("##对默认值的解读-如无需写：无##");
        sb.append("\n```\n\n");

        sb.append("### 支持的资源级配置\n\n");
        List<String> resources = config.getBindResources();
        if (resources != null && !resources.isEmpty()) {
            sb.append("||\n|---|\n");
            for (String res : resources) {
                sb.append("|").append(res).append("\n");
            }
        }
        sb.append("\n");

        sb.append("### 资源粒度说明\n\n```\n");
        sb.append("##该条目支持的资源粒度-如无需写：无##");
        sb.append("\n```\n\n");

        sb.append("### 背景信息\n\n```\n");
        sb.append("##触发该条目增删改的背景-如无需写：无##");
        sb.append("\n```\n\n");

        sb.append("### UI暴露\n\n```\n");
        sb.append("##该条目是否需UI暴露？-必填##");
        sb.append("\n```\n\n");

        sb.append("### CLI手册暴露\n\n```\n");
        sb.append("##该条目是否需CLI手册暴露？-必填##");
        sb.append("\n```\n\n");

        sb.append("## 注意事项\n\n```\n");
        sb.append("##该条目有哪些注意事项-如无需写：无##");
        sb.append("\n```\n");

        return sb.toString();
    }
}
