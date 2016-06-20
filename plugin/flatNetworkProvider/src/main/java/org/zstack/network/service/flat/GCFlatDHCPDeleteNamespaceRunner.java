package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.gc.GCCompletion;
import org.zstack.core.gc.GCContext;
import org.zstack.core.gc.GCRunner;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmResponseWrapper;
import org.zstack.network.service.flat.FlatDhcpBackend.DeleteNamespaceRsp;

/**
 * Created by xing5 on 2016/6/20.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GCFlatDHCPDeleteNamespaceRunner implements GCRunner {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public void run(GCContext context, GCCompletion completion) {
        GCFlatDHCPDeleteNamespaceContext ctx = (GCFlatDHCPDeleteNamespaceContext) context.getContext();

        if (!dbf.isExist(ctx.getHostUuid(), HostVO.class)) {
            // the host is deleted;
            completion.success();
            return;
        }

        new KvmCommandSender(ctx.getHostUuid()).send(ctx.getCommand(), FlatDhcpBackend.DHCP_DELETE_NAMESPACE_PATH,
                wrapper -> {
                    DeleteNamespaceRsp rsp = wrapper.getResponse(DeleteNamespaceRsp.class);
                    return rsp.isSuccess() ? null : errf.stringToOperationError(rsp.getError());
                }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
                    @Override
                    public void success(KvmResponseWrapper w) {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
    }
}
