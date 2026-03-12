package org.zstack.devtool.model;

import java.util.ArrayList;
import java.util.List;

public class GlobalConfigInfo {
    private String category;
    private String name;
    private String type;
    private String defaultValue;
    private String description;
    private String validatorRegularExpression;

    // from @GlobalConfigValidation
    private long numberGreaterThan = Long.MIN_VALUE;
    private long numberLessThan = Long.MAX_VALUE;
    private long[] inNumberRange = {};
    private String[] validValues = {};

    // from @BindResourceConfig
    private List<String> bindResources = new ArrayList<>();

    // source location
    private String sourceFile;
    private String fieldName;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getValidatorRegularExpression() { return validatorRegularExpression; }
    public void setValidatorRegularExpression(String v) { this.validatorRegularExpression = v; }
    public long getNumberGreaterThan() { return numberGreaterThan; }
    public void setNumberGreaterThan(long v) { this.numberGreaterThan = v; }
    public long getNumberLessThan() { return numberLessThan; }
    public void setNumberLessThan(long v) { this.numberLessThan = v; }
    public long[] getInNumberRange() { return inNumberRange; }
    public void setInNumberRange(long[] v) { this.inNumberRange = v; }
    public String[] getValidValues() { return validValues; }
    public void setValidValues(String[] v) { this.validValues = v; }
    public List<String> getBindResources() { return bindResources; }
    public void setBindResources(List<String> v) { this.bindResources = v; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getValueRange() {
        if (validValues != null && validValues.length > 0) {
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < validValues.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(validValues[i]);
            }
            sb.append("}");
            return sb.toString();
        }

        if (inNumberRange != null && inNumberRange.length == 2) {
            return "[" + inNumberRange[0] + ", " + inNumberRange[1] + "]";
        }

        if (numberGreaterThan != Long.MIN_VALUE || numberLessThan != Long.MAX_VALUE) {
            return "[" + numberGreaterThan + ", " + numberLessThan + "]";
        }

        // default range based on type
        if (type != null) {
            switch (type) {
                case "java.lang.Long":
                    return "[" + Long.MIN_VALUE + ", " + Long.MAX_VALUE + "]";
                case "java.lang.Integer":
                    return "[" + Integer.MIN_VALUE + ", " + Integer.MAX_VALUE + "]";
                case "java.lang.Boolean":
                    return "{true, false}";
                default:
                    return "";
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return category + "/" + name + " (" + type + ", default=" + defaultValue + ")";
    }
}
