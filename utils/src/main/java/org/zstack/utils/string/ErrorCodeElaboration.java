package org.zstack.utils.string;

/**
 * Created by mingjian.deng on 2018/11/28.
 */
public class ErrorCodeElaboration {
    private String category;
    private String code;
    private String regex;
    private String message_cn;
    private String message_en;
    private String operation_cn;
    private String operation_en;
    private String causes_cn;
    private String causes_en;
    private String extension_cn;
    private String extension_en;
    private String source = "zstack";
    private double distance = 0;
    private ElaborationSearchMethod method = ElaborationSearchMethod.distance;
    private String formatSrcError;
    private String url;

    public ErrorCodeElaboration() {
    }

    public ErrorCodeElaboration(String en, String cn) {
        message_en = en;
        message_cn = cn;
    }

    public ErrorCodeElaboration(ErrorCodeElaboration other) {
        category = other.category;
        code = other.code;
        regex = other.regex;
        message_en = other.message_en;
        message_cn = other.message_cn;
        operation_en = other.operation_en;
        operation_cn = other.operation_cn;
        causes_en = other.causes_en;
        causes_cn = other.causes_cn;
        extension_en = other.extension_en;
        extension_cn = other.extension_cn;
        source = other.source;
        distance = other.distance;
        formatSrcError = other.formatSrcError;
        url = other.url;
        method = other.method;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getMessage_cn() {
        return message_cn;
    }

    public void setMessage_cn(String message_cn) {
        this.message_cn = message_cn;
    }

    public String getMessage_en() {
        return message_en;
    }

    public void setMessage_en(String message_en) {
        this.message_en = message_en;
    }

    public String getOperation_cn() {
        return operation_cn;
    }

    public void setOperation_cn(String operation_cn) {
        this.operation_cn = operation_cn;
    }

    public String getOperation_en() {
        return operation_en;
    }

    public void setOperation_en(String operation_en) {
        this.operation_en = operation_en;
    }

    public String getCauses_cn() {
        return causes_cn;
    }

    public void setCauses_cn(String causes_cn) {
        this.causes_cn = causes_cn;
    }

    public String getCauses_en() {
        return causes_en;
    }

    public void setCauses_en(String causes_en) {
        this.causes_en = causes_en;
    }

    public String getExtension_cn() {
        return extension_cn;
    }

    public void setExtension_cn(String extension_cn) {
        this.extension_cn = extension_cn;
    }

    public String getExtension_en() {
        return extension_en;
    }

    public void setExtension_en(String extension_en) {
        this.extension_en = extension_en;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getFormatSrcError() {
        return formatSrcError;
    }

    public void setFormatSrcError(String formatSrcError) {
        this.formatSrcError = formatSrcError;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ElaborationSearchMethod getMethod() {
        return method;
    }

    public void setMethod(ElaborationSearchMethod method) {
        this.method = method;
    }
}
