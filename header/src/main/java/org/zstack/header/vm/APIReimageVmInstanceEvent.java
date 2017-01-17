package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;

/**
 * Created by miao on 11/3/16.
 */
public class APIReimageVmInstanceEvent extends APIEvent {
    public APIReimageVmInstanceEvent(String apiId) {
        super(apiId);
    }

    public APIReimageVmInstanceEvent() {
        super(null);
    }
 
    public static APIReimageVmInstanceEvent __example__() {
        APIReimageVmInstanceEvent event = new APIReimageVmInstanceEvent();


        return event;
    }

}
