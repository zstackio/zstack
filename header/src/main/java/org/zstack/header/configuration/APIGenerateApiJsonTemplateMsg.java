package org.zstack.header.configuration;

import org.zstack.header.message.APIMessage;

import java.util.List;

public class APIGenerateApiJsonTemplateMsg extends APIMessage {
    private String exportPath;
    private List<String> basePackageNames;

    public String getExportPath() {
        return exportPath;
    }

    public void setExportPath(String exportPath) {
        this.exportPath = exportPath;
    }

    public List<String> getBasePackageNames() {
        return basePackageNames;
    }

    public void setBasePackageNames(List<String> basePackageNames) {
        this.basePackageNames = basePackageNames;
    }
}
