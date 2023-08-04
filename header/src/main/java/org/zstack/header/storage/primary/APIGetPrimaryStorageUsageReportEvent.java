package org.zstack.header.storage.primary;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Map;

@RestResponse(fieldsTo = {"all"})
public class APIGetPrimaryStorageUsageReportEvent extends APIEvent {
    private Map<String, UsageReport> uriUsageForecast;
    private UsageReport usageReport;

    public Map<String, UsageReport> getUriUsageForecast() {
        return uriUsageForecast;
    }

    public void setUriUsageForecast(Map<String, UsageReport> uriUsageForecast) {
        this.uriUsageForecast = uriUsageForecast;
    }

    public UsageReport getUsageReport() {
        return usageReport;
    }

    public void setUsageReport(UsageReport usageReport) {
        this.usageReport = usageReport;
    }

    public APIGetPrimaryStorageUsageReportEvent() {
        super(null);
    }

    public APIGetPrimaryStorageUsageReportEvent(String apiId) {
        super(apiId);
    }

    public static APIGetPrimaryStorageUsageReportEvent __example__() {
        APIGetPrimaryStorageUsageReportEvent event = new APIGetPrimaryStorageUsageReportEvent();

        event.setUsageReport(new UsageReport());
        event.setUriUsageForecast(new java.util.HashMap<String, UsageReport>());

        return event;
    }
}
