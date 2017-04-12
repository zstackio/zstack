package org.zstack.test.integration.core.CloudBus

import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBusIN
import org.zstack.core.thread.AsyncThread
import org.zstack.header.AbstractService
import org.zstack.header.message.Message
import org.zstack.test.integration.core.cloudbus.HelloWorldMsgForCloudBusCase
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by heathhose on 17-3-22.
 */
public class CloudBusCase extends SubCase{

    CloudBusIN bus
    CountDownLatch latch = new CountDownLatch(1)
    boolean isSuccess = false
    String servId = "FakeServiceForCloudBusCase"
    CountDownLatch startLatch = new CountDownLatch(1)
    
    class FakeService extends AbstractService {
        @Override
        public boolean start() {
            bus.registerService(this)
            bus.activeService(this)
            startLatch.countDown()
            return true
        }

        @Override
        public boolean stop() {
            bus.deActiveService(this)
            bus.unregisterService(this)
            return true
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.getClass() == HelloWorldMsgForCloudBusCase.class) {
                isSuccess = true
            }
            latch.countDown()
        }

        @Override
        public String getId() {
            return bus.makeLocalServiceId(servId)
        }

    }

    @AsyncThread
    private void startFakeService() {
        FakeService fs = new FakeService()
        fs.start()
    }

    @Override
    public void setup() {
    }

    @Override
    public void environment() {
    }

    @Override
    public void test() {
        bus = bean(CloudBusIN.class)

        testCloudBusSharding()
    }

    void testCloudBusSharding(){
        HelloWorldMsgForCloudBusCase msg = new HelloWorldMsgForCloudBusCase() 
        startFakeService() 
        startLatch.await() 

        bus.makeTargetServiceIdByResourceUuid(msg, servId, Platform.getUuid()) 
        bus.send(msg) 
        latch.await(10, TimeUnit.SECONDS) 
        assert isSuccess
    }

    @Override
    public void clean() {
    }


}
