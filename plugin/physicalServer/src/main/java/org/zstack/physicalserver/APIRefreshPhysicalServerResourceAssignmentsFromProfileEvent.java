package org.zstack.physicalserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"all"})
public class APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent extends APIEvent {
    private List<RefreshPhysicalServerResourceAssignmentResult> results = new ArrayList<>();

    public APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent() {
    }

    public APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent(String apiId) {
        super(apiId);
    }

    public List<RefreshPhysicalServerResourceAssignmentResult> getResults() {
        return results;
    }

    public void setResults(List<RefreshPhysicalServerResourceAssignmentResult> results) {
        this.results = results;
    }

    public static APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent __example__() {
        APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent event =
                new APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent();
        RefreshPhysicalServerResourceAssignmentResult result = new RefreshPhysicalServerResourceAssignmentResult();
        result.setServerUuid(uuid());
        event.setResults(Collections.singletonList(result));
        return event;
    }
}
