package org.zstack.core.config;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(fieldsTo = { "all" })
public class APIQueryGlobalConfigValidValuesEvent extends APIEvent {
    private List<String> validValues;

    public APIQueryGlobalConfigValidValuesEvent(String apiId) {
        super(apiId);
    }

    public APIQueryGlobalConfigValidValuesEvent() {
        super(null);
    }

    public List<String> getValidValues() {
        return validValues;
    }

    public void setValidValues(List<String> validValues) {
        this.validValues = validValues;
    }
}