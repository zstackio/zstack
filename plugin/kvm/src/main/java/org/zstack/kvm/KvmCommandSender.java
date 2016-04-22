package org.zstack.kvm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by xing5 on 2016/4/19.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class KvmCommandSender {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ApiTimeoutManager timeoutManager;
    @Autowired
    private ErrorFacade errf;

    private List<String> hostUuids;
    private Iterator<String> it;
    private List<ErrorCode> errors = new ArrayList<ErrorCode>();
    private boolean noStatusCheck;

    public KvmCommandSender(List<String> hostUuids, boolean noStatusCheck) {
        this.hostUuids = hostUuids;
        it = hostUuids.iterator();
        this.noStatusCheck = noStatusCheck;
    }

    public KvmCommandSender(List<String> hostUuids) {
        this(hostUuids, false);
    }

    public KvmCommandSender(String hostUuid) {
        this(hostUuid, false);
    }

    public KvmCommandSender(String hostUuid, boolean noStatusCheck) {
        this(list(hostUuid), noStatusCheck);
        DebugUtils.Assert(hostUuid != null, "hostUuid cannot be null");
    }

    public void send(final Object cmd, final String path, final KvmCommandFailureChecker checker, final ReturnValueCompletion<Object> completion) {
        send(cmd, path, checker, TimeUnit.MINUTES.toMillis(5), completion);
    }

    public void send(final Object cmd, final String path, final KvmCommandFailureChecker checker , final long defaulTimeout, final ReturnValueCompletion<Object> completion) {
        if (!it.hasNext()) {
            ErrorCode err = errors.size() == 1 ? errors.get(0) : errf.stringToOperationError(String.format("failed to execute" +
                    " the command[%s] on the kvm host%s", cmd.getClass(), hostUuids), errors);
            completion.fail(err);
            return;
        }

        String huuid = it.next();
        KVMHostAsyncHttpCallMsg msg = new KVMHostAsyncHttpCallMsg();
        msg.setCommand(cmd);
        msg.setCommandTimeout(timeoutManager.getTimeout(cmd.getClass(), defaulTimeout));
        msg.setHostUuid(huuid);
        msg.setPath(path);
        msg.setNoStatusCheck(noStatusCheck);
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, huuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    errors.add(reply.getError());
                    send(cmd, path, checker, defaulTimeout, completion);
                    return;
                }

                KVMHostAsyncHttpCallReply ar = reply.castReply();
                ErrorCode err = checker.getError(new KvmResponseWrapper(ar.getResponse()));
                if (err != null) {
                    errors.add(err);
                    send(cmd, path, checker, defaulTimeout, completion);
                    return;
                }

                completion.success(ar.getResponse());
            }
        });
    }
}
