package org.zstack.header.errorcode;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.zstack.utils.string.ErrorCodeElaboration;

import java.util.stream.Collectors;

/**
 * Created by mingjian.deng on 2020/3/25.
 */
public class JobResultError {
    private ErrorCodeElaboration message;
    private String detail;
    private String cause;

    public JobResultError() {}

    public JobResultError(ErrorCodeElaboration message, String detail) {
        this.message = message;
        this.detail = detail;
    }

    public static JobResultError valueOf(ErrorCode error) {
        if (error instanceof ErrorCodeList && !CollectionUtils.isEmpty(((ErrorCodeList) error).getCauses())) {
            return valueOf((ErrorCodeList) error);
        }
        return parseErrorCode(error);
    }

    private static JobResultError parseErrorCode(ErrorCode error) {
        JobResultError result = new JobResultError(error.getMessages(), error.getDetails());
        result.setCause(error.getRootCauseDetails());
        return result;
    }

    public static JobResultError valueOf(ErrorCodeList error) {
        if (error.getMessages() != null || error.getDetails() != null || CollectionUtils.isEmpty(error.getCauses())) {
            return parseErrorCode(error);
        }
        if (error.getCauses().size() == 1) {
            return JobResultError.valueOf(error.getCauses().get(0));
        }

        JobResultError result = new JobResultError(error.getMessages(), error.getDetails());
        result.setCause(StringUtils.join(
                error.getCauses().stream().map(JobResultError::valueOf).collect(Collectors.toList()), "; "));
        return result;
    }

    public ErrorCodeElaboration getMessage() {
        return message;
    }

    public void setMessage(ErrorCodeElaboration message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}
