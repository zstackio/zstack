package org.zstack.header.cluster;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by GuoYi on 3/12/18
 */
@RestResponse(allTo = "results")
public class APIUpdateClusterOSEvent extends APIEvent {
    // key:   hostUuid
    // value: cluster os update result
    private Map<String, String> results;

    public Map<String, String> getResults() {
        return results;
    }

    public void setResults(Map<String, String> results) {
        this.results = results;
    }

    public APIUpdateClusterOSEvent() {
    }

    public APIUpdateClusterOSEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateClusterOSEvent __example__() {
        APIUpdateClusterOSEvent event = new APIUpdateClusterOSEvent();
        Map<String, String> results = new HashMap<>();
        results.put(uuid(), "success");
        event.setResults(results);
        return event;
    }
}
