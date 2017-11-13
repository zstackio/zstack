package org.zstack.header.longjob;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

/**
 * Created by GuoYi on 11/13/17.
 */
@Action(category = LongJobConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/longjobs",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APISubmitLongJobEvent.class
)
public class APISubmitLongJobMsg extends APICreateMessage {
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(maxLength = 255)
    private String jobName;
    @APIParam
    private String jobData;
    @APIParam(maxLength = 32, required = false, resourceType = ResourceVO.class, checkAccount = true)
    private String targetResourceUuid;

    @APINoSee
    private String jobUuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public static APISubmitLongJobMsg __example__() {
        APISubmitLongJobMsg msg = new APISubmitLongJobMsg();
        msg.setName("migrate-volume");
        msg.setDescription("migrate volume to another Ceph primary storage");
        msg.setJobName("APIPrimaryStorageMigrateVolumeMsg");
        msg.setJobData("{\"volumeUuid\":\"45a53d3d93384433add8ead7616586cf\", \"dstPrimaryStorageUuid\":\"70a0618804864b3dabe8be9824c8028c\"}");
        msg.setTargetResourceUuid("45a53d3d93384433add8ead7616586cf");
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;
        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    String uuid = ((APISubmitLongJobEvent)evt).getInventory().getUuid();
                    ntfy("Submitted long job[uuid:%s] for %s", uuid, jobName)
                            .resource(uuid, LongJobVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}
