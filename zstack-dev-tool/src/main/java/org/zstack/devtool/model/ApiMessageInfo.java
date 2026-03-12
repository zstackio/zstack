package org.zstack.devtool.model;

import java.util.ArrayList;
import java.util.List;

public class ApiMessageInfo {
    private String className;        // e.g. "APICreateZoneMsg"
    private String packageName;      // e.g. "org.zstack.header.zone"
    private String sourceFile;

    // from @RestRequest
    private String path;
    private String httpMethod;       // POST, GET, PUT, DELETE
    private String responseClass;    // simple name of response class
    private String parameterName;    // defaults to "params"
    private boolean isAction;
    private List<String> optionalPaths = new ArrayList<>();

    // from class hierarchy
    private String parentClass;      // e.g. "APICreateMessage"
    private boolean isQuery;         // extends APIQueryMessage
    private boolean suppressCredentialCheck;

    // fields
    private List<ApiParamInfo> params = new ArrayList<>();

    // derived
    public String getActionName() {
        // APICreateZoneMsg -> CreateZoneAction
        String name = className;
        if (name.startsWith("API")) name = name.substring(3);
        if (name.endsWith("Msg")) name = name.substring(0, name.length() - 3);
        return name + "Action";
    }

    public String getResultName() {
        String name = className;
        if (name.startsWith("API")) name = name.substring(3);
        if (name.endsWith("Msg")) name = name.substring(0, name.length() - 3);
        return name + "Result";
    }

    public String getHelperMethodName() {
        // CreateZoneAction -> createZone
        String action = getActionName();
        String name = action.substring(0, action.length() - 6); // strip "Action"
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    public String getClassName() { return className; }
    public void setClassName(String v) { this.className = v; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String v) { this.packageName = v; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String v) { this.sourceFile = v; }
    public String getPath() { return path; }
    public void setPath(String v) { this.path = v; }
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String v) { this.httpMethod = v; }
    public String getResponseClass() { return responseClass; }
    public void setResponseClass(String v) { this.responseClass = v; }
    public String getParameterName() { return parameterName; }
    public void setParameterName(String v) { this.parameterName = v; }
    public boolean isAction() { return isAction; }
    public void setAction(boolean v) { this.isAction = v; }
    public List<String> getOptionalPaths() { return optionalPaths; }
    public void setOptionalPaths(List<String> v) { this.optionalPaths = v; }
    public String getParentClass() { return parentClass; }
    public void setParentClass(String v) { this.parentClass = v; }
    public boolean isQuery() { return isQuery; }
    public void setQuery(boolean v) { this.isQuery = v; }
    public boolean isSuppressCredentialCheck() { return suppressCredentialCheck; }
    public void setSuppressCredentialCheck(boolean v) { this.suppressCredentialCheck = v; }
    public List<ApiParamInfo> getParams() { return params; }
    public void setParams(List<ApiParamInfo> v) { this.params = v; }
}
