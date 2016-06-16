package org.zstack.core.ansible;

import java.util.List;

/**
 * Created by xing5 on 2016/6/15.
 */
public class AnsibleLogCmd {
    private String label;
    private List<String> parameters;

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
