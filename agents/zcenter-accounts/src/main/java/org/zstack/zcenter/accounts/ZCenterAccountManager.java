package org.zstack.zcenter.accounts;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.AbstractService;
import org.zstack.header.identity.AccountSource;
import org.zstack.header.identity.AccountState;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.AccountVO_;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.Session;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zcenter.accounts.api.APICreateSessionForZCenterAccountEvent;
import org.zstack.zcenter.accounts.api.APICreateSessionForZCenterAccountMsg;

import static org.zstack.core.Platform.err;

public class ZCenterAccountManager extends AbstractService {
    private static final CLogger logger = Utils.getLogger(ZCenterAccountManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateSessionForZCenterAccountMsg) {
            handle((APICreateSessionForZCenterAccountMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APICreateSessionForZCenterAccountMsg msg) {
        APICreateSessionForZCenterAccountEvent event = new APICreateSessionForZCenterAccountEvent(msg.getId());

        AccountVO account = findAccount(msg);
        if (account == null) {
            event.setError(err(ZCenterAccountsErrors.ACCOUNT_NOT_FOUND,
                    "no account matched by [uuid:%s, name:%s, source:%s]",
                    msg.getAccountUuid(), msg.getAccountName(), msg.getSource()));
            bus.publish(event);
            return;
        }

        if (account.getState() == AccountState.Disabled) {
            event.setError(err(ZCenterAccountsErrors.ACCOUNT_DISABLED,
                    "account[uuid:%s, name:%s] is disabled, cannot create a session",
                    account.getUuid(), account.getName()));
            bus.publish(event);
            return;
        }

        SessionInventory session = Session.login(account.getUuid());
        logger.info(String.format("created a session[uuid:%s] for account[uuid:%s] requested by ZCenter",
                session.getUuid(), account.getUuid()));
        event.setInventory(session);
        bus.publish(event);
    }

    private AccountVO findAccount(APICreateSessionForZCenterAccountMsg msg) {
        if (msg.getAccountUuid() != null) {
            return dbf.findByUuid(msg.getAccountUuid(), AccountVO.class);
        }

        AccountSource source = msg.getSource() != null
                ? AccountSource.valueOf(msg.getSource())
                : AccountSource.Local;
        return Q.New(AccountVO.class)
                .eq(AccountVO_.name, msg.getAccountName())
                .eq(AccountVO_.source, source)
                .find();
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ZCenterAccountConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
