package org.zstack.core.apicost;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class APIHistoryVO {
    @Id
    @Column
    private String requestUuid;

    @Column
    private String apiName;

    @Column
    private String requestDump;

    @Column
    private String responseDump;

    @Column
    private Timestamp requestDate;

    @Column
    private Timestamp responseDate;

    public String getRequestUuid() {
        return requestUuid;
    }

    public void setRequestUuid(String requestUuid) {
        this.requestUuid = requestUuid;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getRequestDump() {
        return requestDump;
    }

    public void setRequestDump(String requestDump) {
        this.requestDump = requestDump;
    }

    public String getResponseDump() {
        return responseDump;
    }

    public void setResponseDump(String responseDump) {
        this.responseDump = responseDump;
    }

    public Timestamp getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Timestamp requestDate) {
        this.requestDate = requestDate;
    }

    public Timestamp getResponseDate() {
        return responseDate;
    }

    public void setResponseDate(Timestamp responseDate) {
        this.responseDate = responseDate;
    }
}
