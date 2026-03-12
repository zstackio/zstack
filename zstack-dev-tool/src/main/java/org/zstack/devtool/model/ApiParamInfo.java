package org.zstack.devtool.model;

public class ApiParamInfo {
    private String fieldName;
    private String fieldType;        // fully qualified, e.g. "java.lang.String"
    private boolean required = true;
    private boolean noSee;           // @APINoSee - excluded from SDK
    private int maxLength = 0;
    private int minLength = 0;
    private String validRegexValues;
    private String[] validValues;
    private boolean nonempty;
    private boolean nullElements;
    private boolean emptyString = true;
    private boolean noTrim;
    private long[] numberRange;
    private boolean inherited;       // from parent class, handled by SDK base class

    public String getFieldName() { return fieldName; }
    public void setFieldName(String v) { this.fieldName = v; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String v) { this.fieldType = v; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean v) { this.required = v; }
    public boolean isNoSee() { return noSee; }
    public void setNoSee(boolean v) { this.noSee = v; }
    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int v) { this.maxLength = v; }
    public int getMinLength() { return minLength; }
    public void setMinLength(int v) { this.minLength = v; }
    public String getValidRegexValues() { return validRegexValues; }
    public void setValidRegexValues(String v) { this.validRegexValues = v; }
    public String[] getValidValues() { return validValues; }
    public void setValidValues(String[] v) { this.validValues = v; }
    public boolean isNonempty() { return nonempty; }
    public void setNonempty(boolean v) { this.nonempty = v; }
    public boolean isNullElements() { return nullElements; }
    public void setNullElements(boolean v) { this.nullElements = v; }
    public boolean isEmptyString() { return emptyString; }
    public void setEmptyString(boolean v) { this.emptyString = v; }
    public boolean isNoTrim() { return noTrim; }
    public void setNoTrim(boolean v) { this.noTrim = v; }
    public long[] getNumberRange() { return numberRange; }
    public void setNumberRange(long[] v) { this.numberRange = v; }
    public boolean isInherited() { return inherited; }
    public void setInherited(boolean v) { this.inherited = v; }
}
