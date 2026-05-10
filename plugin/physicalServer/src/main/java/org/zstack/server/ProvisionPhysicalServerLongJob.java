package org.zstack.server;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.server.APIProvisionPhysicalServerEvent;
import org.zstack.header.server.APIProvisionPhysicalServerMsg;
import org.zstack.header.server.PhysicalServerVO;
import org.zstack.header.server.ProvisionPhase;
import org.zstack.header.server.ProvisionResult;
import org.zstack.longjob.LongJobUtils;
import org.zstack.utils.gson.JSONObjectUtil;

@LongJobFor(APIProvisionPhysicalServerMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ProvisionPhysicalServerLongJob implements LongJob {
    @Autowired
    private PhysicalServerProvisionService provisionService;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        String jobData = job.getJobData();
        APIProvisionPhysicalServerMsg msg = JSONObjectUtil.toObject(jobData, APIProvisionPhysicalServerMsg.class);
        ProvisionPhase startPhase = parsePhase(jobData);

        provisionService.startProvisioning(msg, job.getAccountUuid(), job.getUuid(), startPhase,
                new ReturnValueCompletion<ProvisionResult>(completion) {
            @Override
            public void success(ProvisionResult result) {
                LongJobVO updated = LongJobUtils.setJobResult(job.getUuid(), result);
                APIProvisionPhysicalServerEvent event = new APIProvisionPhysicalServerEvent(job.getApiId());
                event.setInventory(LongJobInventory.valueOf(updated));
                completion.success(event);
            }

            @Override
            public void fail(org.zstack.header.errorcode.ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public Class getAuditType() {
        return PhysicalServerVO.class;
    }

    private ProvisionPhase parsePhase(String jobData) {
        if (jobData == null || jobData.isEmpty()) {
            return ProvisionPhase.NotStarted;
        }
        JsonObject obj = new JsonParser().parse(jobData).getAsJsonObject();
        if (obj.has("phase") && !obj.get("phase").isJsonNull()) {
            return ProvisionPhase.valueOf(obj.get("phase").getAsString());
        }
        return ProvisionPhase.NotStarted;
    }

}
