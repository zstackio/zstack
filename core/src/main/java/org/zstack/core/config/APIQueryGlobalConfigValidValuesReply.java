package org.zstack.core.config;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(fieldsTo = { "validValues" })
public class APIQueryGlobalConfigValidValuesReply extends APIReply {
    private List<String> validValues;

    public List<String> getValidValues() {
        return validValues;
    }

    public void setValidValues(List<String> validValues) {
        this.validValues = validValues;
    }
}