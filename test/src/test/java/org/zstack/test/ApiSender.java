package org.zstack.test;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusEventListener;
import org.zstack.core.debug.APIDebugSignalMsg;
import org.zstack.core.debug.DebugSignal;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.progressbar.InProgressEvent;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApiSender {
    private APIEvent result;
    private volatile boolean isTimeout = true;
    private int timeout = 15;

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    public <T extends MessageReply> T call(APIMessage msg, Class clazz) throws ApiSenderException {
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        MessageReply r = bus.call(msg);
        if (!r.isSuccess()) {
            throw new ApiSenderException(r.getError());
        } else {
            return r.castReply();
        }
    }

    private <T extends APIEvent> T doSend(final APIMessage msg, Class<? extends APIEvent> clazz, boolean exceptionOnError) throws ApiSenderException {
        APIEvent resultEvent;
        try {
            resultEvent = clazz.newInstance();
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to create instance of " + clazz.getCanonicalName(), e);
        }

        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        final CountDownLatch count = new CountDownLatch(1);
        bus.subscribeEvent(new CloudBusEventListener() {
            @Override
            public boolean handleEvent(Event e) {
                APIEvent ae = (APIEvent) e;
                if (ae instanceof InProgressEvent) {
                    return false;
                }

                if (msg.getId().equals(ae.getApiId())) {
                    result = ae;
                    isTimeout = false;
                    count.countDown();
                    return true;
                }

                return false;
            }

        }, resultEvent);
        bus.send(msg);

        try {
            count.await(timeout, TimeUnit.SECONDS);
            if (isTimeout) {
                Api api = new Api();
                APIDebugSignalMsg dmsg = new APIDebugSignalMsg();
                dmsg.setServiceId(ApiMediatorConstant.SERVICE_ID);
                dmsg.setSignals(asList(DebugSignal.DumpTaskQueue.toString()));
                dmsg.setSession(api.loginAsAdmin());
                bus.send(dmsg);
                TimeUnit.SECONDS.sleep(2);

                String errStr = String.format("%s[uuid:%s] timeout after %s seconds", msg.getMessageName(), msg.getId(), timeout);
                throw new ApiSenderException(errf.stringToTimeoutError(errStr));
            }
        } catch (InterruptedException e1) {
            throw new CloudRuntimeException("", e1);
        }

        if (!result.isSuccess()) {
            if (exceptionOnError) {
                throw new ApiSenderException(result.getError());
            } else {
                return null;
            }
        } else {
            return (T) result;
        }
    }

    public <T extends APIEvent> T send(APIMessage msg, Class<? extends APIEvent> clazz) throws ApiSenderException {
        return doSend(msg, clazz, true);
    }

    public <T extends APIEvent> T send(APIMessage msg, Class<? extends APIEvent> clazz, boolean exceptionOnError) throws ApiSenderException {
        return doSend(msg, clazz, exceptionOnError);
    }

    public <T extends APIReply> T list(APIListMessage msg) throws ApiSenderException {
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        APIReply reply = (APIReply) bus.call(msg);
        if (!reply.isSuccess()) {
            throw new ApiSenderException(reply.getError());
        } else {
            return (T) reply;
        }
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
