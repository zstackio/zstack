package org.zstack.core.errorcode;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.string.ErrorCodeElaboration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mingjian.deng on 2018/12/1.
 */
@RestResponse(allTo = "errorCodes")
public class APIGetElaborationsReply extends APIReply {
    private List<ErrorCodeElaboration> errorCodes = new ArrayList<>();

    public List<ErrorCodeElaboration> getErrorCodes() {
        return errorCodes;
    }

    public void setErrorCodes(List<ErrorCodeElaboration> errorCodes) {
        this.errorCodes = errorCodes;
    }

    public static APIGetElaborationsReply __example__() {
        APIGetElaborationsReply reply = new APIGetElaborationsReply();
        List<ErrorCodeElaboration> e = new ArrayList<>();
        ErrorCodeElaboration elaboration = new ErrorCodeElaboration();

        e.add(elaboration);
        reply.setErrorCodes(e);
        return reply;
    }
}
