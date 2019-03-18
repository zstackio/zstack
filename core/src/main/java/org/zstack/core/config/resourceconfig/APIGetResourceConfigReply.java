package org.zstack.core.config.resourceconfig;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by MaJin on 2019/2/23.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetResourceConfigReply extends APIReply {
    private String value;
    private List<ResourceConfigInventory> effectiveConfigs;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<ResourceConfigInventory> getEffectiveConfigs() {
        return effectiveConfigs;
    }

    public void setEffectiveConfigs(List<ResourceConfigInventory> effectiveConfigs) {
        this.effectiveConfigs = effectiveConfigs;
    }
}
