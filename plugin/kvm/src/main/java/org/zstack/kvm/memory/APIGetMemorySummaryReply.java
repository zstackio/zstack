package org.zstack.kvm.memory;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = {"summary"})
public class APIGetMemorySummaryReply extends APIReply {
    private MemorySummaryInventory summary;
    public MemorySummaryInventory getSummary() { return summary; }
    public void setSummary(MemorySummaryInventory value) { summary = value; }

    public static APIGetMemorySummaryReply __example__() {
        APIGetMemorySummaryReply reply = new APIGetMemorySummaryReply();
        reply.setSummary(MemorySummaryInventory.__example__());
        return reply;
    }
}
