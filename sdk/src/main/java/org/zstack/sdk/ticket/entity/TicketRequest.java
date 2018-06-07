package org.zstack.sdk.ticket.entity;



public class TicketRequest  {

    public java.lang.String requestName;
    public void setRequestName(java.lang.String requestName) {
        this.requestName = requestName;
    }
    public java.lang.String getRequestName() {
        return this.requestName;
    }

    public java.lang.String apiName;
    public void setApiName(java.lang.String apiName) {
        this.apiName = apiName;
    }
    public java.lang.String getApiName() {
        return this.apiName;
    }

    public int executeTimes;
    public void setExecuteTimes(int executeTimes) {
        this.executeTimes = executeTimes;
    }
    public int getExecuteTimes() {
        return this.executeTimes;
    }

    public java.lang.Object apiBody;
    public void setApiBody(java.lang.Object apiBody) {
        this.apiBody = apiBody;
    }
    public java.lang.Object getApiBody() {
        return this.apiBody;
    }

}
