package org.zstack.compute.vm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Constants;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.longjob.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.header.volume.GetVolumeTaskMsg;
import org.zstack.header.volume.GetVolumeTaskReply;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;


/**
 * Created by on camile 2018/3/7.
 */
@LongJobFor(APIMigrateVmMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MigrateVmLongJob implements LongJob {
    private static final Logger logger = LogManager.getLogger(MigrateVmLongJob.class);
    private static final int WAIT_CHAIN_TASK_EXIT_MAX_RETRIES = 30;
    private static final long WAIT_CHAIN_TASK_EXIT_INTERVAL_SECS = 1;

    @Autowired
    protected CloudBus bus;
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    protected String auditResourceUuid;

    @Override
    public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
        MigrateVmInnerMsg msg = JSONObjectUtil.toObject(job.getJobData(), MigrateVmInnerMsg.class);

        List<String> backupTaskLongJobUuids = getBackupTaskLongJobUuids(job.getJobData());
        if (backupTaskLongJobUuids != null && !backupTaskLongJobUuids.isEmpty()) {
            logger.info(String.format("migrate vm[uuid:%s] longjob has %d backup longjobs to cancel first",
                    msg.getVmInstanceUuid(), backupTaskLongJobUuids.size()));
            cancelBackupLongJobsThenMigrate(backupTaskLongJobUuids, msg, completion);
        } else {
            doMigrate(msg, completion);
        }
    }

    private List<String> getBackupTaskLongJobUuids(String jobData) {
        Map<String, Object> raw = JSONObjectUtil.toObject(jobData, LinkedHashMap.class);
        Object uuids = raw == null ? null : raw.get("backupTaskLongJobUuids");
        if (!(uuids instanceof List<?>)) {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) uuids) {
            if (item == null) {
                continue;
            }
            String uuid = String.valueOf(item).trim();
            if (!uuid.isEmpty()) {
                result.add(uuid);
            }
        }
        return result.isEmpty() ? null : result;
    }

    private void cancelBackupLongJobsThenMigrate(List<String> backupTaskLongJobUuids,
                                                  MigrateVmInnerMsg msg,
                                                  ReturnValueCompletion<APIEvent> completion) {
        cancelBackupLongJobs(backupTaskLongJobUuids.iterator(), new Completion(completion) {
            @Override
            public void success() {
                waitForVolumeChainTasksExit(msg.getVmInstanceUuid(), WAIT_CHAIN_TASK_EXIT_MAX_RETRIES,
                        new Completion(completion) {
                    @Override
                    public void success() {
                        doMigrate(msg, completion);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to cancel backup longjobs for vm[uuid:%s], " +
                        "attempting migration anyway: %s", msg.getVmInstanceUuid(), errorCode));
                doMigrate(msg, completion);
            }
        });
    }

    private void cancelBackupLongJobs(Iterator<String> it, Completion completion) {
        if (!it.hasNext()) {
            completion.success();
            return;
        }

        String longJobUuid = it.next();
        CancelLongJobMsg cmsg = new CancelLongJobMsg();
        cmsg.setUuid(longJobUuid);
        bus.makeLocalServiceId(cmsg, LongJobConstants.SERVICE_ID);
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to cancel backup longjob[uuid:%s]: %s",
                            longJobUuid, reply.getError()));
                }
                cancelBackupLongJobs(it, completion);
            }
        });
    }

    private void waitForVolumeChainTasksExit(String vmUuid, int retriesLeft, Completion completion) {
        List<String> volUuids = Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, vmUuid)
                .select(VolumeVO_.uuid)
                .listValues();

        if (volUuids.isEmpty()) {
            completion.success();
            return;
        }

        GetVolumeTaskMsg gmsg = new GetVolumeTaskMsg();
        gmsg.setVolumeUuids(volUuids);
        bus.makeLocalServiceId(gmsg, VolumeConstant.SERVICE_ID);
        bus.send(gmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                    return;
                }

                GetVolumeTaskReply gr = reply.castReply();
                boolean hasRunningTasks = gr.getResults().values().stream()
                        .anyMatch(info -> !info.getRunningTask().isEmpty());

                if (!hasRunningTasks) {
                    completion.success();
                    return;
                }

                if (retriesLeft <= 0) {
                    completion.fail(operr(
                            "timeout waiting for volume backup chain tasks to exit for vm[uuid:%s]", vmUuid));
                    return;
                }

                logger.debug(String.format(
                        "volumes of vm[uuid:%s] still have running tasks, retry in %ds (retries left: %d)",
                        vmUuid, WAIT_CHAIN_TASK_EXIT_INTERVAL_SECS, retriesLeft));
                thdf.submitTimeoutTask(
                        () -> waitForVolumeChainTasksExit(vmUuid, retriesLeft - 1, completion),
                        TimeUnit.SECONDS, WAIT_CHAIN_TASK_EXIT_INTERVAL_SECS);
            }
        });
    }

    private void doMigrate(MigrateVmInnerMsg msg, ReturnValueCompletion<APIEvent> completion) {
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, msg.getVmInstanceUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    MigrateVmInnerReply r = reply.castReply();
                    APIMigrateVmEvent evt = new APIMigrateVmEvent(ThreadContext.get(Constants.THREAD_CONTEXT_API));

                    auditResourceUuid = r.getInventory().getUuid();
                    evt.setInventory(r.getInventory());
                    completion.success(evt);
                } else {
                    auditResourceUuid = msg.getVmInstanceUuid();
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {
        MigrateVmInnerMsg msg = JSONObjectUtil.toObject(job.getJobData(), MigrateVmInnerMsg.class);
        CancelMigrateVmMsg cmsg = new CancelMigrateVmMsg();
        cmsg.setCancellationApiId(job.getApiId());
        cmsg.setUuid(msg.getVmInstanceUuid());
        bus.makeTargetServiceIdByResourceUuid(cmsg, VmInstanceConstant.SERVICE_ID, cmsg.getVmInstanceUuid());
        bus.send(cmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success(false);
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public Class getAuditType() {
        return VmInstanceVO.class;
    }

    @Override
    public String getAuditResourceUuid() {
        return auditResourceUuid;
    }
}
