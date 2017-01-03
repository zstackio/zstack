package org.zstack.core.progress;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.progress.ProgressCommands.ProgressReportCmd;
import org.zstack.header.AbstractService;
import org.zstack.header.core.progress.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.SyncHttpCallHandler;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;


/**
 * Created by mingjian.deng on 16/12/10.
 */
public class ProgressReportService extends AbstractService implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ProgressReportService.class);
    @Autowired
    private RESTFacade restf;

    @Autowired
    protected ErrorFacade errf;

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        restf.registerSyncHttpCallHandler(ProgressConstants.PROGRESS_START_PATH, ProgressReportCmd.class, new SyncHttpCallHandler<ProgressReportCmd>() {
            @Override
            public String handleSyncHttpCall(ProgressReportCmd cmd) {
                //TODO
                logger.debug(String.format("call PROGRESS_START_PATH by %s, uuid: %s", cmd.getProcessType(), cmd.getResourceUuid()));
                startProcess(cmd);
                return null;
            }
        });

        restf.registerSyncHttpCallHandler(ProgressConstants.PROGRESS_REPORT_PATH, ProgressReportCmd.class, new SyncHttpCallHandler<ProgressReportCmd>() {
            @Override
            public String handleSyncHttpCall(ProgressReportCmd cmd) {
                //TODO
                logger.debug(String.format("call PROGRESS_REPORT_PATH by %s, uuid: %s", cmd.getProcessType(), cmd.getResourceUuid()));
                process(cmd);
                return null;
            }
        });

        restf.registerSyncHttpCallHandler(ProgressConstants.PROGRESS_FINISH_PATH, ProgressReportCmd.class, new SyncHttpCallHandler<ProgressReportCmd>() {
            @Override
            public String handleSyncHttpCall(ProgressReportCmd cmd) {
                //TODO
                logger.debug(String.format("call PROGRESS_FINISH_PATH by %s, uuid: %s", cmd.getProcessType(), cmd.getResourceUuid()));
                finishProcess(cmd);
                return null;
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void validation(ProgressReportCmd cmd) {
        validationType(cmd.getProcessType());
        validationUuid(cmd.getResourceUuid());
    }

    private void validationType(String processType) {
        if (processType == null || ProgressConstants.ProgressType.valueOf(processType) == null) {
            logger.warn(String.format("not supported processType: %s", processType));
            throw new OperationFailureException(
                    errf.stringToOperationError(String.format("not supported processType: %s",
                            processType)));
        }
    }

    private void validationUuid(String uuid) {
        if (uuid == null) {
            logger.warn(String.format("not supported null uuid: %s", uuid));
            throw new OperationFailureException(
                    errf.stringToOperationError(String.format("not supported null uuid: %s",
                            uuid)));
        }
    }

    private void startProcess(ProgressReportCmd cmd) {
        validation(cmd);
        insertProgress(cmd);
    }

    private void process(ProgressReportCmd cmd) {
        validation(cmd);
        updateProgress(cmd);
    }

    private void finishProcess(ProgressReportCmd cmd) {
        validation(cmd);
        deleteProgress(cmd);
    }

    private void insertProgress(ProgressReportCmd cmd) {
        logger.debug(String.format("insert progress and it begins, processType is: %s", cmd.getProcessType()
        ));
        SimpleQuery<ProgressVO> q = dbf.createQuery(ProgressVO.class);
        // please notice if there are no conditions that result more than two vo found...
        q.add(ProgressVO_.processType, SimpleQuery.Op.EQ, cmd.getProcessType());
        q.add(ProgressVO_.resourceUuid, SimpleQuery.Op.EQ, cmd.getResourceUuid());
        if (q.isExists()) {
            logger.warn(String.format("delete records that shouldn't exist...: %s", q.count()));
            q.list().stream().forEach(p -> dbf.remove(p));
        }
        ProgressVO vo = new ProgressVO();
        vo.setProgress(cmd.getProgress() == null? "0":cmd.getProgress());
        vo.setProcessType(cmd.getProcessType());
        vo.setResourceUuid(cmd.getResourceUuid());
        dbf.persistAndRefresh(vo);
    }

    private void deleteProgress(ProgressReportCmd cmd) {
        logger.debug("delete progress and it's over");
        SimpleQuery<ProgressVO> q = dbf.createQuery(ProgressVO.class);
        // please notice if there are no conditions that result more than two vo found...
        q.add(ProgressVO_.processType, SimpleQuery.Op.EQ, cmd.getProcessType());
        q.add(ProgressVO_.resourceUuid, SimpleQuery.Op.EQ, cmd.getResourceUuid());
        if (q.isExists()) {
            q.list().stream().forEach(p -> dbf.remove(p));
        }
    }

    @Override
    public void managementNodeReady() {

    }

    private void updateProgress(ProgressReportCmd cmd) {
        logger.debug(String.format("update progress and during processing, progress is: %s", cmd.getProgress()));
        SimpleQuery<ProgressVO> q = dbf.createQuery(ProgressVO.class);
        q.add(ProgressVO_.processType, SimpleQuery.Op.EQ, cmd.getProcessType());
        q.add(ProgressVO_.resourceUuid, SimpleQuery.Op.EQ, cmd.getResourceUuid());
        if (q.isExists()) {
            assert q.list().size() == 1;
            ProgressVO vo = q.find();
            vo.setProgress(cmd.getProgress());
            dbf.updateAndRefresh(vo);
        } else {
            insertProgress(cmd);
        }
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    @Override
    public String getId() {
        return ProgressConstants.SERVICE_ID;
    }

    private void handleApiMessage(APIMessage msg) {
        try {
            if (msg instanceof APIGetTaskProgressMsg) {
                handle((APIGetTaskProgressMsg) msg);
            } else {
                bus.dealWithUnknownMessage(msg);
            }
        } catch (Exception e) {
            bus.logExceptionWithMessageDump(msg, e);
            bus.replyErrorByMessageType(msg, e);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handle(APIGetTaskProgressMsg msg) {
        APIGetTaskProgressReply reply = new APIGetTaskProgressReply();
        SimpleQuery<ProgressVO> q = dbf.createQuery(ProgressVO.class);
        q.add(ProgressVO_.resourceUuid, SimpleQuery.Op.EQ, msg.getResourceUuid());
        if (msg.getProcessType() != null) {
            q.add(ProgressVO_.processType, SimpleQuery.Op.EQ, msg.getProcessType());
        }
        q.orderBy(ProgressVO_.lastOpDate, SimpleQuery.Od.ASC);
        List<ProgressVO> vos = q.list();
        if (q.list().size() == 0) {
            reply.setError(errf.instantiateErrorCode(ProgressError.NO_SUCH_TASK_RUNNING,
                    "no such task running now"));
            reply.setSuccess(false);
        } else {
            vos.stream().forEach(p -> reply.getTaskProgress().add(new TaskProgress(p)));
        }
        bus.reply(msg, reply);
    }
}
