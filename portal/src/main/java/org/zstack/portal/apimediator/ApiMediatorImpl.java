package org.zstack.portal.apimediator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.managementnode.*;
import org.zstack.header.message.*;
import org.zstack.header.rest.RestAPIExtensionPoint;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.VersionComparator;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.*;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;


public class ApiMediatorImpl extends AbstractService implements ApiMediator, GlobalApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(ApiMediator.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    private List<RestAPIExtensionPoint> apiExts = new ArrayList<>();


    private ApiMessageProcessor processor;

    private List<String> serviceConfigFolders;
    private int apiWorkerNum = 5;

    private void dispatchMessage(APIMessage msg) {
        ApiMessageDescriptor desc = processor.getApiMessageDescriptor(msg);
        if (desc == null) {
            Map message = map(e(msg.getClass().getName(), msg));
            ErrorCode err = err(PortalErrors.NO_SERVICE_FOR_MESSAGE, "no service configuration file declares message: %s", JSONObjectUtil.toJsonString(message));
            logger.warn(err.getDetails());
            bus.replyErrorByMessageType(msg, err);
            return;
        }


        try {
            msg.setServiceId(null);
            msg = processor.process(msg);
        } catch (ApiMessageInterceptionException ie) {
            logger.debug(ie.getError().toString(), ie);
            bus.replyErrorByMessageType(msg, ie.getError());
            return;
        } catch (StopRoutingException e) {
            return;
        }

        if (msg.getServiceId() == null && desc.getServiceId() != null) {
            bus.makeLocalServiceId(msg, desc.getServiceId());
        }

        if (msg.getServiceId() == null) {
            ErrorCode err = inerr("No service id found for API message[%s], message dump: %s", msg.getMessageName(), JSONObjectUtil.toJsonString(msg));
            logger.warn(err.getDetails());
            bus.replyErrorByMessageType(msg, err);
            return;
        }

        if (!msg.hasSystemTag(PortalSystemTags.VALIDATION_ONLY.getTagFormat())) {
            bus.route(msg);
            return;
        }

        // this call is only for validate the API parameters
        if (msg instanceof APISyncCallMessage) {
            APIReply reply = new APIReply();
            bus.reply(msg, reply);
        } else {
            APIEvent evt = new APIEvent(msg.getId());
            bus.publish(evt);
        }
    }


    @Override
    public void handleMessage(final Message msg) {
        apiExts.forEach(e -> e.afterAPIRequest(msg));
        thdf.syncSubmit(new SyncTask<Object>() {
            @Override
            public String getSyncSignature() {
                return "api.worker";
            }

            @Override
            public int getSyncLevel() {
                return apiWorkerNum;
            }

            @Override
            public String getName() {
                return "api.worker";
            }

            @MessageSafe
            void handleMessage(Message msg) {
                if (msg instanceof APIIsReadyToGoMsg) {
                    handle((APIIsReadyToGoMsg) msg);
                } else if (msg instanceof APIGetVersionMsg) {
                    handle((APIGetVersionMsg) msg);
                } else if (msg instanceof APIGetCurrentTimeMsg) {
                    handle((APIGetCurrentTimeMsg) msg);
                } else if (msg instanceof APIMessage) {
                    dispatchMessage((APIMessage) msg);
                } else {
                    logger.debug("Not an APIMessage.Message ID is " + msg.getId());
                }
            }

            @Override
            public Object call() {
                handleMessage(msg);
                return null;
            }
        });
    }

    @Transactional(readOnly = true)
    private String getMnVersion() {
        String sql = "select v.version from schema_version v order by installed_rank desc";
        Query q = dbf.getEntityManager().createNativeQuery(sql);
        q.setMaxResults(5);

        @SuppressWarnings("unchecked")
        List<String> versions = q.getResultList();
        Optional<String> ver = versions.stream()
                .map(VersionComparator::new)
                .max(VersionComparator::compare)
                .map(VersionComparator::toString);
        return ver.orElse("unknown");
    }

    private void handle(APIGetVersionMsg msg) {
        APIGetVersionReply reply = new APIGetVersionReply();
        reply.setVersion(getMnVersion());
        bus.reply(msg, reply);
    }

    private void handle(APIGetCurrentTimeMsg msg) {
        Map<String, Long> ret = new HashMap<>();
        long currentTimeMillis = System.currentTimeMillis();
        long currentTimeSeconds = System.currentTimeMillis()/1000;
        ret.put("MillionSeconds", currentTimeMillis);
        ret.put("Seconds", currentTimeSeconds);
        APIGetCurrentTimeReply reply = new APIGetCurrentTimeReply();
        reply.setCurrentTime(ret);
        bus.reply(msg, reply);
    }

    private void handle(final APIIsReadyToGoMsg msg) {
        final APIIsReadyToGoReply areply = new APIIsReadyToGoReply();

        IsManagementNodeReadyMsg imsg = new IsManagementNodeReadyMsg();
        String nodeId = msg.getManagementNodeId();
        if (nodeId == null) {
            bus.makeLocalServiceId(imsg, ManagementNodeConstant.SERVICE_ID);
            nodeId = Platform.getManagementServerId();
        } else {
            bus.makeServiceIdByManagementNodeId(imsg, ManagementNodeConstant.SERVICE_ID, msg.getManagementNodeId());
        }

        final String fnodeId = nodeId;
        areply.setManagementNodeId(nodeId);
        bus.send(imsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    areply.setError(reply.getError());
                } else {
                    IsManagementNodeReadyReply r = (IsManagementNodeReadyReply) reply;
                    if (!r.isReady()) {
                        areply.setError(err(SysErrors.NOT_READY_ERROR,
                                "management node[uuid:%s] is not ready yet", fnodeId));
                    }
                }
                bus.reply(msg, areply);
            }
        });
    }

    @Override
    public String getId() {
        return ApiMediatorConstant.SERVICE_ID;
    }

    @Override
    public boolean start() {
        Map<String, Object> config = new HashMap<>();
        config.put("serviceConfigFolders", serviceConfigFolders);
        processor = new ApiMessageProcessorImpl(config);
        bus.registerService(this);
        apiExts = pluginRgty.getExtensionList(RestAPIExtensionPoint.class);

        return true;
    }

    @Override
    public boolean stop() {
        bus.unregisterService(this);
        return true;
    }

    public void setServiceConfigFolders(List<String> serviceConfigFolders) {
        this.serviceConfigFolders = serviceConfigFolders;
    }

    public void setApiWorkerNum(int apiWorkerNum) {
        this.apiWorkerNum = apiWorkerNum;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> lst = new ArrayList<>();
        lst.add(APICreateMessage.class);
        return lst;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateMessage) {
            APICreateMessage cmsg = (APICreateMessage) msg;
            if (cmsg.getResourceUuid() != null) {
                if (!StringDSL.isZStackUuid(cmsg.getResourceUuid())) {
                    throw new ApiMessageInterceptionException(argerr("resourceUuid[%s] is not a valid uuid. A valid uuid is a UUID(v4 recommended) with '-' stripped. " +
                                    "see http://en.wikipedia.org/wiki/Universally_unique_identifier for format of UUID, the regular expression uses" +
                                    " to validate a UUID is '[0-9a-f]{8}[0-9a-f]{4}[1-5][0-9a-f]{3}[89ab][0-9a-f]{3}[0-9a-f]{12}'", cmsg.getResourceUuid()));
                }
            }
        }
        return msg;
    }

    @Override
    public ApiMessageProcessor getProcesser() {
        return processor;
    }
}
