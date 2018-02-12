package org.zstack.tool.doclet;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class APIMessageDoc {
    public static class ParameterDoc {
        private String name;
        private String description;
        private String choices;
        private boolean optional;
        private String since;

        public String getChoices() {
            return choices;
        }

        public void setChoices(String choices) {
            this.choices = choices;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isOptional() {
            return optional;
        }

        public void setOptional(boolean optional) {
            this.optional = optional;
        }

        public String getSince() {
            return since;
        }

        public void setSince(String since) {
            this.since = since;
        }
    }

    private String name;
    private String fullName;
    private String description;
    private String message;
    private String httpMessage;
    private String cli;
    private String since;
    private String result;
    private List<ParameterDoc> parameters = new ArrayList<ParameterDoc>();

    public String getHttpMessage() {
        return httpMessage;
    }

    public void setHttpMessage(String httpMessage) {
        this.httpMessage = httpMessage;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCli() {
        return cli;
    }

    public void setCli(String cli) {
        this.cli = cli;
    }

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public List<ParameterDoc> getParameters() {
        return parameters;
    }

    public void setParameters(List<ParameterDoc> parameters) {
        this.parameters = parameters;
    }
}
