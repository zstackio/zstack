package org.zstack.tool.doclet;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class APIEventDoc {
    public static class EventField {
        private String name;
        private String description;
        private String choices;
        private boolean nullable;
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

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
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
    private String example;
    private List<EventField> fields = new ArrayList<EventField>();
    private String since;

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public List<EventField> getFields() {
        return fields;
    }

    public void setFields(List<EventField> fields) {
        this.fields = fields;
    }
}
