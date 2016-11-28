package org.zstack.test.multinodes;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.job.JobQueueFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.NopeReturnValueCompletion;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class SilentJobService extends AbstractService {
    CLogger logger = Utils.getLogger(SilentJobService.class);
    public static String SERVICE_ID = "SilentJobService";

    @Autowired
    private CloudBus bus;
    @Autowired
    private JobQueueFacade jobf;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof SilentJobMsg) {
            SilentJobMsg smsg = (SilentJobMsg) msg;
            List<String> uuids = new ArrayList<String>();
            for (int i = 0; i < smsg.getJobNum(); i++) {
                SilentJob job = null;
                if (smsg.isRestartable()) {
                    job = new RestartableSilentJob();
                } else {
                    job = new SilentJob();
                }
                final String uuid = Platform.getUuid();
                job.setUuid(uuid);
                uuids.add(uuid);
                jobf.execute("silent-job", getId(), job, new NopeReturnValueCompletion(), null);
            }

            SilentJobReply reply = new SilentJobReply();
            reply.setJobUuids(uuids);
            bus.reply(msg, reply);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    @Override
    public boolean start() {
        bus.registerService(this);
        return true;
    }

    @Override
    public boolean stop() {
        bus.unregisterService(this);
        return true;
    }
}
