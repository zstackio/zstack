package org.zstack.utils.string;

import java.util.Locale;

/**
 * Created by mingjian.deng on 2018/11/28.
 */
public class ErrorCodeElaboration {
    private String category;
    private String code;
    private String regex;
    private String message_cn;
    private String message_en;
    private String source = "zstack";
    private double distance = 0;
    private ElaborationSearchMethod method;
    private String formatSrcError;

    public ErrorCodeElaboration() {
    }

    public ErrorCodeElaboration(ErrorCodeElaboration other, Locale locale, Object...args) {
        if (args != null) {
            this.message_en = String.format(other.message_en, args);
            this.message_cn = Locale.SIMPLIFIED_CHINESE.equals(locale) ? String.format(other.message_cn, args) : null;
        } else {
            this.message_en = other.message_en;
            this.message_cn = Locale.SIMPLIFIED_CHINESE.equals(locale) ? other.message_cn : null;
        }
        this.distance = other.distance;
        this.method = other.method;
        this.code = other.code;
    }

    public ErrorCodeElaboration(ErrorCodeElaboration other, Locale locale) {
        category = other.category;
        code = other.code;
        regex = other.regex;
        message_en = other.message_en;
        message_cn = Locale.SIMPLIFIED_CHINESE.equals(locale) ? other.message_cn : null;
        source = other.source;
        distance = other.distance;
        formatSrcError = other.formatSrcError;
        method = other.method;
    }

    public ErrorCodeElaboration(ErrorCodeElaboration other) {
        category = other.category;
        code = other.code;
        regex = other.regex;
        message_en = other.message_en;
        message_cn = other.message_cn;
        source = other.source;
        distance = other.distance;
        formatSrcError = other.formatSrcError;
        method = other.method;
    }

    public ErrorCodeElaboration addElaborationMessage(ErrorCodeElaboration other) {
        if (message_en != null) {
            message_en = message_en + "," + other.message_en;
        } else {
            message_en = other.message_en;
        }

        if (message_cn != null) {
            message_cn = message_cn + "," + other.message_cn;
        } else {
            message_cn = other.message_cn;
        }
        return this;
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

    public ElaborationSearchMethod getMethod() {
        return method;
    }

    public void setMethod(ElaborationSearchMethod method) {
        this.method = method;
    }
}
