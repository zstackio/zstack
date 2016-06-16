package org.zstack.core.ansible;

import org.zstack.core.logging.LogLevel;

import java.util.List;

/**
 * Created by xing5 on 2016/6/15.
 */
public class AnsibleLogCmd {
    private String label;
    private List<String> parameters;
    private String level = LogLevel.INFO.toString();

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }
}

