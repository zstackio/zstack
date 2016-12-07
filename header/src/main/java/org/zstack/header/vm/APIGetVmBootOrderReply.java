package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by frank on 11/22/2015.
 */
@RestResponse(fieldsTo = {"orders=order"})
public class APIGetVmBootOrderReply extends APIReply {
    private List<String> order;

    public List<String> getOrder() {
        return order;
    }

    public void setOrder(List<String> order) {
        this.order = order;
    }
}
