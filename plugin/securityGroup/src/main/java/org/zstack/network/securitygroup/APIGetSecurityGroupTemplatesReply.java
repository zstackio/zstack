package org.zstack.network.securitygroup;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "templates")
public class APIGetSecurityGroupTemplatesReply extends APIReply {
    private List<SecurityGroupTemplateInventory> templates;

    public List<SecurityGroupTemplateInventory> getTemplates() {
        return templates;
    }

    public void setTemplates(List<SecurityGroupTemplateInventory> templates) {
        this.templates = templates;
    }

    public static APIGetSecurityGroupTemplatesReply __example__() {
        APIGetSecurityGroupTemplatesReply reply = new APIGetSecurityGroupTemplatesReply();
        SecurityGroupTemplateInventory template = new SecurityGroupTemplateInventory();
        template.setId("Custom");
        template.setName("Custom");
        template.setDescription("Create a security group with custom rules");
        template.setDefaultSelected(true);
        template.setRules(Collections.emptyList());
        reply.setTemplates(Collections.singletonList(template));
        return reply;
    }
}
