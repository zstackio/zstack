package org.zstack.test.integration.kvm.scheduler

import org.quartz.JobDetail
import org.quartz.JobKey
import org.quartz.Trigger
import org.quartz.TriggerKey
import org.quartz.impl.matchers.GroupMatcher
import org.springframework.http.HttpEntity
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.core.scheduler.SchedulerStateEvent
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.scheduler.SchedulerConstant
import org.zstack.scheduler.SchedulerFacadeImpl
import org.zstack.header.core.scheduler.SchedulerJobSchedulerTriggerRefVO
import org.zstack.header.core.scheduler.SchedulerJobSchedulerTriggerRefVO_
import org.zstack.header.core.scheduler.SchedulerJobVO
import org.zstack.header.core.scheduler.SchedulerState
import org.zstack.header.core.scheduler.SchedulerTriggerVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.scheduler.SchedulerType
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.AddSchedulerJobToSchedulerTriggerAction
import org.zstack.sdk.CreateSchedulerTriggerAction
import org.zstack.sdk.SchedulerJobInventory
import org.zstack.sdk.SchedulerTriggerInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.sql.Timestamp

/**
 * Created by AlanJager on 2017/6/7.
 */
class SchedulerCase extends SubCase {
    EnvSpec env
    SchedulerFacadeImpl scheduler

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            scheduler = bean(SchedulerFacadeImpl.class)
            testSchedulerJobAPI()
            testSchedulerTriggerAPI()
            testSchedulerTriggerApiMessageInterceptor()
            testAddAndRemoveSchedulerJobToOrFromTriggerAPI()
            testAddSchedulerJobToTriggerInterceptor()
            testPauseResumeDeleteSchedulerTask()
            testTriggerStopTimeCalculation()
            testDeleteSchedulerJobWillRemoveJobTriggerRefFirst()
            testVmStateChangeCauseSchedulerPause()
        }
    }

    void testSchedulerJobAPI() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        // test create scheduler job
        SchedulerJobInventory startInv = createSchedulerJob {
            targetResourceUuid = vm.uuid
            description = "a job to start a vm"
            name = "start"
            type = SchedulerType.START_VM
        } as SchedulerJobInventory
        SchedulerJobVO startVO = dbFindByUuid(startInv.getUuid(), SchedulerJobVO.class)
        assert startVO.name == startInv.name

        SchedulerJobInventory rebootInv = createSchedulerJob {
            targetResourceUuid = vm.uuid
            name = "reboot"
            type = SchedulerType.REBOOT_VM
        } as SchedulerJobInventory
        SchedulerJobVO rebootVO = dbFindByUuid(rebootInv.getUuid(), SchedulerJobVO.class)
        assert rebootVO.name == rebootInv.name


        SchedulerJobInventory stopInv = createSchedulerJob {
            targetResourceUuid = vm.uuid
            name = "stop"
            type = SchedulerType.STOP_VM
        } as SchedulerJobInventory
        SchedulerJobVO stopVO = dbFindByUuid(stopInv.getUuid(), SchedulerJobVO.class)
        assert stopVO.name == stopInv.name

        Map<String, String> ps = new HashMap<>()
        ps.put("snapshotName", "test")
        ps.put("snapshotDescription", "test")
        SchedulerJobInventory snapshotSchedulerInv = createSchedulerJob {
            name = "snapshot"
            targetResourceUuid = vm.getRootVolumeUuid()
            type = SchedulerType.VOLUME_SNAPSHOT
            parameters = ps
        } as SchedulerJobInventory
        SchedulerJobVO createVolumeSnapshotSchedulerVO = dbFindByUuid(snapshotSchedulerInv.getUuid(), SchedulerJobVO.class)
        assert createVolumeSnapshotSchedulerVO.name == snapshotSchedulerInv.name

        // test update scheduler api
        updateSchedulerJob {
            uuid = startInv.uuid
            name = "new name"
        }
        startVO = dbFindByUuid(startInv.getUuid(), SchedulerJobVO.class)
        assert startVO.name == "new name"

        // test delete scheduler api
        deleteSchedulerJob {
            uuid = stopInv.uuid
        }
        retryInSecs {
            stopVO = dbFindByUuid(stopInv.getUuid(), SchedulerJobVO.class)
            assert stopVO == null
        }
    }

    void testSchedulerTriggerAPI() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        // test create scheduler trigger
        SchedulerTriggerInventory inv = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            schedulerInterval = 12222
            repeatCount = 22222
            startTime = (new Date(0)).getTime()
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        }
        SchedulerTriggerVO vo = dbFindByUuid(inv.uuid, SchedulerTriggerVO.class)
        assert vo.name == inv.name

        // test update scheduler trigger
        updateSchedulerTrigger {
            uuid = inv.uuid
            name = "new trigger"
            description = "this is a new trigger desc"
        }
        vo = dbFindByUuid(inv.uuid, SchedulerTriggerVO.class)
        assert vo.name == "new trigger"

        // test delete scheduler trigger
        deleteSchedulerTrigger {
            uuid = inv.uuid
        }
        vo = dbFindByUuid(inv.uuid, SchedulerTriggerVO.class)
        assert vo == null
    }

    void testSchedulerTriggerApiMessageInterceptor() {
        CreateSchedulerTriggerAction action = new CreateSchedulerTriggerAction()
        action.name = "trigger"
        action.description = "this is a trigger"
        action.schedulerInterval = Integer.MAX_VALUE
        action.repeatCount = 1000
        action.sessionId = adminSession()
        action.startTime = 3600
        action.schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        CreateSchedulerTriggerAction.Result ret = action.call()
        assert ret.error != null


        CreateSchedulerTriggerAction action2 = new CreateSchedulerTriggerAction()
        action2.name = "trigger"
        action2.description = "this is a trigger"
        action2.schedulerInterval = Integer.MAX_VALUE
        action2.repeatCount = Integer.MAX_VALUE
        action2.sessionId = adminSession()
        action2.startTime = 2147454847 - 1
        action2.schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        CreateSchedulerTriggerAction.Result ret2 = action2.call()
        assert ret2.error != null


        CreateSchedulerTriggerAction action3 = new CreateSchedulerTriggerAction()
        action3.name = "trigger"
        action3.description = "this is a trigger"
        action3.sessionId = adminSession()
        action3.startTime = 3600
        action3.schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        CreateSchedulerTriggerAction.Result ret3 = action3.call()
        assert ret3.error != null


        CreateSchedulerTriggerAction action4 = new CreateSchedulerTriggerAction()
        action4.name = "trigger"
        action4.description = "this is a trigger"
        action4.sessionId = adminSession()
        action4.repeatCount = 2
        action4.startTime = 3600
        action4.schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        CreateSchedulerTriggerAction.Result ret4 = action4.call()
        assert ret4.error != null


        CreateSchedulerTriggerAction action5 = new CreateSchedulerTriggerAction()
        action5.name = "trigger"
        action5.description = "this is a trigger"
        action5.sessionId = adminSession()
        action5.schedulerInterval = -1
        action5.repeatCount = 2
        action5.startTime = 3600
        action5.schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        CreateSchedulerTriggerAction.Result ret5 = action5.call()
        assert ret5.error != null

        CreateSchedulerTriggerAction action6 = new CreateSchedulerTriggerAction()
        action6.name = "trigger"
        action6.description = "this is a trigger"
        action6.sessionId = adminSession()
        action6.schedulerInterval = 2
        action6.repeatCount = -1
        action6.startTime = 3600
        action6.schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        CreateSchedulerTriggerAction.Result ret6 = action6.call()
        assert ret6.error != null

        CreateSchedulerTriggerAction action7 = new CreateSchedulerTriggerAction()
        action7.name = "trigger"
        action7.description = "this is a trigger"
        action7.sessionId = adminSession()
        action7.schedulerInterval = 2
        action7.repeatCount = 2
        action7.schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        CreateSchedulerTriggerAction.Result ret7 = action7.call()
        assert ret7.error != null


        CreateSchedulerTriggerAction action8 = new CreateSchedulerTriggerAction()
        action8.name = "trigger"
        action8.description = "this is a trigger"
        action8.sessionId = adminSession()
        action8.schedulerInterval = 2
        action8.repeatCount = 2
        action8.startTime = -1
        action8.schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        CreateSchedulerTriggerAction.Result ret8 = action8.call()
        assert ret8.error != null

        // start time out of range
        CreateSchedulerTriggerAction action9 = new CreateSchedulerTriggerAction()
        action9.name = "trigger"
        action9.description = "this is a trigger"
        action9.sessionId = adminSession()
        action9.schedulerInterval = 2
        action9.repeatCount = 2
        action9.startTime = 2147454847L + 1
        action9.schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        CreateSchedulerTriggerAction.Result ret9 = action9.call()
        assert ret9.error != null

        // forever trigger
        CreateSchedulerTriggerAction action10 = new CreateSchedulerTriggerAction()
        action10.name = "trigger"
        action10.description = "this is a trigger"
        action10.sessionId = adminSession()
        action10.schedulerInterval = 2
        action10.startTime = 2037454847
        action10.schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        CreateSchedulerTriggerAction.Result ret10 = action10.call()
        assert ret10.error == null
    }

    void testAddSchedulerJobToTriggerInterceptor() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        SchedulerJobInventory job = createSchedulerJob {
            targetResourceUuid = vm.uuid
            name = "start"
            type = SchedulerType.START_VM
        } as SchedulerJobInventory

        // test create scheduler trigger
        SchedulerTriggerInventory trigger = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            repeatCount = 1
            startTime = 0
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        addSchedulerJobToSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }

        AddSchedulerJobToSchedulerTriggerAction action = new AddSchedulerJobToSchedulerTriggerAction()
        action.schedulerJobUuid = job.getUuid()
        action.schedulerTriggerUuid = trigger.getUuid()
        action.sessionId = adminSession()
        AddSchedulerJobToSchedulerTriggerAction.Result ret = action.call()
        assert ret.error != null

        removeSchedulerJobFromSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }
    }

    void testAddAndRemoveSchedulerJobToOrFromTriggerAPI() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        stopVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Stopped

        // test start vm job
        KVMAgentCommands.StartVmCmd startVmCmd = null
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> e ->
            startVmCmd = json(e.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }

        SchedulerJobInventory job = createSchedulerJob {
            targetResourceUuid = vm.uuid
            name = "start"
            type = SchedulerType.START_VM
        } as SchedulerJobInventory

        // test create scheduler trigger
        SchedulerTriggerInventory trigger = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            repeatCount = 1
            startTime = 0
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        addSchedulerJobToSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }

        retryInSecs {
            vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert vo.state == VmInstanceState.Running
            assert startVmCmd != null
        }

        removeSchedulerJobFromSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }

        //  create another scheduler trigger
        trigger = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            repeatCount = 2222
            schedulerInterval = 222222
            startTime = (new Date(0)).getTime()
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        addSchedulerJobToSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }

        SchedulerJobSchedulerTriggerRefVO ref = Q.New(SchedulerJobSchedulerTriggerRefVO.class)
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, job.getUuid())
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid, trigger.getUuid())
                .find()
        assert ref != null

        Set<JobKey> ss = scheduler.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(trigger.getUuid()))
        Set<TriggerKey> ts = scheduler.getScheduler().getTriggerKeys(GroupMatcher.triggerGroupEquals(trigger.getUuid() + "." + job.getUuid()))
        assert ss.size() == 1
        assert ts.size() == 1

        // test cancel a scheduler job
        removeSchedulerJobFromSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }
        ref = Q.New(SchedulerJobSchedulerTriggerRefVO.class)
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, job.getUuid())
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid, trigger.getUuid())
                .find()
        assert ref == null

        // after unscheduled task equal to zero
        ss = scheduler.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(trigger.getUuid()))
        ts = scheduler.getScheduler().getTriggerKeys(GroupMatcher.triggerGroupEquals(trigger.getUuid() + "." + job.getUuid()))
        assert ss.size() == 0
        assert ts.size() == 0


        // test stop vm job
        KVMAgentCommands.StopVmCmd stopVmCmd = null
        env.afterSimulator(KVMConstant.KVM_STOP_VM_PATH) { rsp, HttpEntity<String> e ->
            stopVmCmd = json(e.body, KVMAgentCommands.StopVmCmd.class)
            return rsp
        }

        vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
        job = createSchedulerJob {
            targetResourceUuid = vm.uuid
            name = "stop"
            type = SchedulerType.STOP_VM
        } as SchedulerJobInventory

        trigger = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            repeatCount = 1
            startTime = (new Date(0)).getTime()
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        addSchedulerJobToSchedulerTrigger {
            schedulerJobUuid = job.getUuid()
            schedulerTriggerUuid = trigger.getUuid()
        }

        retryInSecs {
            vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert vo.state == VmInstanceState.Stopped
            assert stopVmCmd != null
        }

        removeSchedulerJobFromSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }

        startVmInstance {
            uuid = vm.uuid
        }

        retryInSecs {
            vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert vo.state == VmInstanceState.Running
        }


        // test reboot vm job
        startVmCmd = null
        startVmCmd = null

        job = createSchedulerJob {
            targetResourceUuid = vm.uuid
            name = "reboot"
            type = SchedulerType.REBOOT_VM
        } as SchedulerJobInventory

        trigger = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            repeatCount = 1
            startTime = (new Date(0)).getTime()
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        addSchedulerJobToSchedulerTrigger {
            schedulerJobUuid = job.getUuid()
            schedulerTriggerUuid = trigger.getUuid()
        }

        retryInSecs {
            assert startVmCmd != null
            assert stopVmCmd != null
        }

        removeSchedulerJobFromSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }

        // test create volume snapshot job
        KVMAgentCommands.TakeSnapshotCmd takeSnapshotCmd = null
        env.afterSimulator(KVMConstant.KVM_TAKE_VOLUME_SNAPSHOT_PATH) { rsp, HttpEntity<String> e ->
            takeSnapshotCmd = json(e.body, KVMAgentCommands.TakeSnapshotCmd.class)
            return rsp
        }

        Map<String, String> ps = new HashMap<>()
        ps.put("snapshotName", "test")
        ps.put("snapshotDescription", "test")
        job = createSchedulerJob {
            targetResourceUuid = vm.getRootVolumeUuid()
            name = "snapshot"
            type = SchedulerType.VOLUME_SNAPSHOT
            parameters = ps
        } as SchedulerJobInventory

        trigger = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            repeatCount = 1
            startTime = (new Date(0)).getTime()
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        addSchedulerJobToSchedulerTrigger {
            schedulerJobUuid = job.getUuid()
            schedulerTriggerUuid = trigger.getUuid()
        }

        retryInSecs {
            assert takeSnapshotCmd != null
        }

        removeSchedulerJobFromSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }
    }

    void testPauseResumeDeleteSchedulerTask() {
        VmInstanceInventory vm = env.inventoryByName("vm")
        SchedulerJobInventory job = createSchedulerJob {
            targetResourceUuid = vm.uuid
            name = "start"
            type = SchedulerType.START_VM
        } as SchedulerJobInventory

        SchedulerTriggerInventory trigger = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            repeatCount = 2222
            schedulerInterval = 222222
            startTime = (new Date(0)).getTime()
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        addSchedulerJobToSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }

        scheduler.pauseSchedulerJob(job.uuid)
        SchedulerJobVO jobVO = dbFindByUuid(job.uuid, SchedulerJobVO.class)
        assert jobVO.state == SchedulerState.Disabled.toString()

        scheduler.resumeSchedulerJob(job.uuid)
        jobVO = dbFindByUuid(job.uuid, SchedulerJobVO.class)
        assert jobVO.state == SchedulerState.Enabled.toString()

        removeSchedulerJobFromSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }
    }

    void testTriggerStopTimeCalculation() {
        SchedulerTriggerInventory trigger = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            repeatCount = 2222
            schedulerInterval = 222222
            startTime = (new Date(0)).getTime() + 1
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        assert trigger.stopTime.getTime() == new Timestamp(1000L * 2222L * 222222L).getTime() + 1L * 1000L

        SchedulerTriggerInventory trigger1 = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            schedulerInterval = 222222
            startTime = (new Date(0)).getTime() + 1
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        assert trigger1.getStopTime() == null

        SchedulerTriggerInventory trigger2 = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            startTime = 0
            repeatCount = 2222
            schedulerInterval = 222222
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        assert trigger2.getStartTime() == null
    }

    void testVmStateChangeCauseSchedulerPause() {
        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false)
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1)
        VmInstanceInventory vm = env.inventoryByName("vm")


        env.simulator(KVMSecurityGroupBackend.SECURITY_GROUP_REFRESH_RULE_ON_HOST_PATH) {
            return new KVMAgentCommands.RefreshAllRulesOnHostResponse()
        }

        SchedulerJobInventory job = createSchedulerJob {
            targetResourceUuid = vm.uuid
            name = "start"
            type = SchedulerType.START_VM
        } as SchedulerJobInventory

        SchedulerTriggerInventory trigger = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            repeatCount = 2222
            schedulerInterval = 222222
            startTime = (new Date(0)).getTime()
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        addSchedulerJobToSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }

        env.afterSimulator(KVMConstant.KVM_PING_PATH) { KVMAgentCommands.PingResponse rsp, HttpEntity<String> e ->
            KVMAgentCommands.PingCmd pingCmd = JSONObjectUtil.toObject(e.body,  KVMAgentCommands.PingCmd.class)
            if (pingCmd.hostUuid == vm.hostUuid) {
                rsp.success = false
                rsp.setError("on purpose")
            } else {
                rsp.success = true
            }
            rsp.hostUuid = pingCmd.hostUuid
            return rsp
        }

        VmInstanceVO vo
        retryInSecs {
            vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert vo.state == VmInstanceState.Unknown
        }

        Trigger.TriggerState state = scheduler.getScheduler().getTriggerState(TriggerKey.triggerKey(trigger.getUuid(), trigger.getUuid() + "." + job.getUuid()))
        assert state == Trigger.TriggerState.PAUSED


        env.afterSimulator(KVMConstant.KVM_PING_PATH) { KVMAgentCommands.PingResponse rsp, HttpEntity<String> e ->
            KVMAgentCommands.PingCmd pingCmd = JSONObjectUtil.toObject(e.body,  KVMAgentCommands.PingCmd.class)
            rsp.success = true
            rsp.hostUuid = pingCmd.hostUuid
            return rsp
        }

        reconnectHost {
            uuid = vm.getHostUuid()
        }
        retryInSecs {
            vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
            assert vo.state == VmInstanceState.Running
        }

        state = scheduler.getScheduler().getTriggerState(TriggerKey.triggerKey(trigger.getUuid(), trigger.getUuid() + "." + job.getUuid()))
        assert state == Trigger.TriggerState.NORMAL

        AccountInventory account = createAccount {
            name = "test"
            password = "password"
        } as AccountInventory

        changeResourceOwner {
            accountUuid = account.getUuid()
            resourceUuid = vm.uuid
        }
        state = scheduler.getScheduler().getTriggerState(TriggerKey.triggerKey(trigger.getUuid(), trigger.getUuid() + "." + job.getUuid()))
        assert state == Trigger.TriggerState.PAUSED

        changeSchedulerState {
            uuid = job.getUuid()
            stateEvent = SchedulerStateEvent.enable.toString()
        }
        state = scheduler.getScheduler().getTriggerState(TriggerKey.triggerKey(trigger.getUuid(), trigger.getUuid() + "." + job.getUuid()))
        assert state == Trigger.TriggerState.NORMAL


        destroyVmInstance {
            uuid = vm.uuid
        }
        state = scheduler.getScheduler().getTriggerState(TriggerKey.triggerKey(trigger.getUuid(), trigger.getUuid() + "." + job.getUuid()))
        assert state == Trigger.TriggerState.PAUSED

        expungeVmInstance {
            uuid = vm.uuid
        }

        removeSchedulerJobFromSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }
    }

    void testDeleteSchedulerJobWillRemoveJobTriggerRefFirst() {
        VmInstanceInventory vm = env.inventoryByName("vm")

        SchedulerJobInventory job = createSchedulerJob {
            targetResourceUuid = vm.uuid
            name = "start"
            type = SchedulerType.START_VM
        } as SchedulerJobInventory

        SchedulerTriggerInventory trigger = createSchedulerTrigger {
            name = "trigger"
            description = "this is a trigger"
            repeatCount = 2222
            schedulerInterval = 222222
            startTime = (new Date(0)).getTime()
            schedulerType = SchedulerConstant.SIMPLE_TYPE_STRING.toString()
        } as SchedulerTriggerInventory

        addSchedulerJobToSchedulerTrigger {
            schedulerJobUuid = job.uuid
            schedulerTriggerUuid = trigger.uuid
        }

        JobDetail jobDetail = scheduler.getScheduler().getJobDetail(JobKey.jobKey(job.uuid, trigger.uuid))
        String triggerName = trigger.uuid
        String triggerGroup = trigger.getUuid() + "." + job.getUuid()
        Trigger trigger1 = scheduler.getScheduler().getTrigger(TriggerKey.triggerKey(triggerName, triggerGroup))
        assert jobDetail != null
        assert trigger1 != null
        deleteSchedulerJob {
            uuid = job.uuid
        }

        // due to the job is deleted job detail will be null
        jobDetail = scheduler.getScheduler().getJobDetail(JobKey.jobKey(job.uuid, trigger.uuid))
        trigger1 = scheduler.getScheduler().getTrigger(TriggerKey.triggerKey(triggerName, triggerGroup))
        assert jobDetail == null
        assert trigger1 == null
        assert !Q.New(SchedulerJobSchedulerTriggerRefVO.class).eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, job.uuid).isExists()
    }
}
