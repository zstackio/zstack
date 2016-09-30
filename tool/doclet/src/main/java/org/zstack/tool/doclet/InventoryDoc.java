package org.zstack.tool.doclet;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class InventoryDoc {
    public static class InventoryField {
        private String name;
        private String description;
        private String type;
        private String since;
        private boolean nullable;
        private String choices;

        public String getChoices() {
            return choices;
        }

        public String getSince() {
            return since;
        }

        public void setSince(String since) {
            this.since = since;
        }

        public boolean isNullable() {
            return nullable;
        }

        public void setNullable(boolean nullable) {
            this.nullable = nullable;
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

        public void setChoices(String choices) {
            this.choices = choices;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    private String name;
    private String fullName;
    private String description;
    private String example;
    private String since;
    private List<InventoryField> fields = new ArrayList<InventoryField>();

    public List<InventoryField> getFields() {
        return fields;
    }

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public void setFields(List<InventoryField> fields) {
        this.fields = fields;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
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
}
