package org.zstack.sugonSdnController.controller.api;

import java.util.List;
import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("serial")
public class EnablePort  extends VRouterApiObjectBase  {

    private static final String NONE = "None";

    @SerializedName("id") private String id  = NONE; // VMI id
    private String display_name = NONE; // VM display name
    private String system_name = NONE; // tap interface name, required

    @Override
    public String getObjectType() {
        return "enable-port";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-domain", "default-project");
    }

    @Override
    public String getDefaultParentType() {
        return "project";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getSystem_name() {
        return system_name;
    }

    public void setSystem_name(String system_name) {
        this.system_name = system_name;
    }

}
