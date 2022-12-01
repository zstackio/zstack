package org.zstack.header.identity;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Wenhao.Zhang on 22/11/29
 */
@RestResponse(allTo = "additions")
public class APIGetLoginProceduresReply extends APIReply {
    private List<Map<String, String>> additions;

    public List<Map<String, String>> getAdditions() {
        return additions;
    }

    public void setAdditions(List<Map<String, String>> additions) {
        this.additions = additions;
    }

    public static APIGetLoginProceduresReply __example__() {
        APIGetLoginProceduresReply reply = new APIGetLoginProceduresReply();

        Map<String, String> map = new HashMap<>();
        map.put("authentications", "InfoSec");
        map.put("credentials", "AAAAAAAAAA");

        List<Map<String, String>> list = new ArrayList<>();
        list.add(map);

        reply.setAdditions(list);
        return reply;
    }
}
