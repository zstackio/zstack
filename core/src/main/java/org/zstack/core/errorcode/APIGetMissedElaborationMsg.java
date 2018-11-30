package org.zstack.core.errorcode;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/12/3.
 */
@RestRequest(
        path = "/errorcode/elaborations/missed",
        method = HttpMethod.GET,
        responseClass = APIGetMissedElaborationReply.class
)
public class APIGetMissedElaborationMsg extends APISyncCallMessage {
    // at least repeat times in ElaborationVO
    @APIParam(required = false, numberRange = {1, Long.MAX_VALUE})
    private Long repeats;
    @APIParam(required = false)
    private String from;

    public Long getRepeats() {
        return repeats;
    }

    public void setRepeats(Long repeats) {
        this.repeats = repeats;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public static APIGetMissedElaborationMsg __example__() {
        APIGetMissedElaborationMsg msg = new APIGetMissedElaborationMsg();
        msg.setRepeats(1L);
        msg.setFrom("2018-12-12 00:00:00");

        return msg;
    }
}
