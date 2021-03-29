package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by MaJin on 2021/3/29.
 */

@Action(category = ImageConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/images/upload-job/details/{imageId}",
        method = HttpMethod.GET,
        responseClass = APIGetUploadImageJobDetailsReply.class
)
public class APIGetUploadImageJobDetailsMsg extends APISyncCallMessage {
    @APIParam
    private String imageId;

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
}
