package org.zstack.test.integration.networkservice.provider.virtualrouter.vip

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.zstack.core.db.Q
import org.zstack.header.identity.IdentityErrors
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
import org.zstack.network.service.vip.VipNetworkServicesRefVO
import org.zstack.network.service.vip.VipNetworkServicesRefVO_
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.vip.VipVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.CreateEipAction
import org.zstack.sdk.CreateLoadBalancerAction
import org.zstack.sdk.CreatePortForwardingRuleAction
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.WebBeanConstructor
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class VipAccountIsolationAndSystemFieldCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.MEGABYTE.toByte(512)
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
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
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
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
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            testCrossAccountVipIsolation()
            testCreateVipIgnoresSystemFieldInRestPayload()
        }
    }

    void testCrossAccountVipIsolation() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory
        L3NetworkInventory l3 = env.inventoryByName("l3") as L3NetworkInventory
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        ImageInventory img = env.inventoryByName("image") as ImageInventory

        shareResource {
            resourceUuids = [pubL3.uuid, l3.uuid, offering.uuid, img.uuid]
            toPublic = true
        }

        AccountInventory accA = createAccount {
            name = "vip-owner-a"
            password = "password"
        } as AccountInventory
        AccountInventory accB = createAccount {
            name = "vip-tenant-b"
            password = "password"
        } as AccountInventory

        SessionInventory sessionA = logInByAccount {
            accountName = accA.name
            password = "password"
        } as SessionInventory
        SessionInventory sessionB = logInByAccount {
            accountName = accB.name
            password = "password"
        } as SessionInventory

        try {
            VipInventory vipA = createVip {
                name = "vip-a"
                l3NetworkUuid = pubL3.uuid
                sessionId = sessionA.uuid
            } as VipInventory

            VmInstanceInventory vmB = createVmInstance {
                name = "vm-b"
                instanceOfferingUuid = offering.uuid
                imageUuid = img.uuid
                l3NetworkUuids = [l3.uuid]
                sessionId = sessionB.uuid
            } as VmInstanceInventory

            List<VipInventory> visibleToB = queryVip {
                conditions = ["uuid=${vipA.uuid}".toString()]
                sessionId = sessionB.uuid
            } as List<VipInventory>
            assert visibleToB.isEmpty()

            CreatePortForwardingRuleAction pfAction = new CreatePortForwardingRuleAction(
                    name: "pf-cross-account",
                    vipUuid: vipA.uuid,
                    vipPortStart: 8080,
                    privatePortStart: 80,
                    protocolType: PortForwardingProtocolType.TCP.toString(),
                    vmNicUuid: vmB.vmNics[0].uuid,
                    sessionId: sessionB.uuid
            )
            assertPermissionDenied(pfAction.call().error)

            CreateEipAction eipAction = new CreateEipAction(
                    name: "eip-cross-account",
                    vipUuid: vipA.uuid,
                    vmNicUuid: vmB.vmNics[0].uuid,
                    sessionId: sessionB.uuid
            )
            assertPermissionDenied(eipAction.call().error)

            CreateLoadBalancerAction lbAction = new CreateLoadBalancerAction(
                    name: "lb-cross-account",
                    vipUuid: vipA.uuid,
                    sessionId: sessionB.uuid
            )
            assertPermissionDenied(lbAction.call().error)

            assert !Q.New(VipNetworkServicesRefVO.class).eq(VipNetworkServicesRefVO_.vipUuid, vipA.uuid).isExists()

            deleteVip {
                uuid = vipA.uuid
                sessionId = sessionA.uuid
            }

            VipInventory vipAdmin = createVip {
                name = "vip-admin-shared"
                l3NetworkUuid = pubL3.uuid
            } as VipInventory
            try {
                assert (queryVip {
                    conditions = ["uuid=${vipAdmin.uuid}".toString()]
                    sessionId = sessionB.uuid
                } as List<VipInventory>).isEmpty()

                shareResource {
                    resourceUuids = [vipAdmin.uuid]
                    accountUuids = [accB.uuid]
                }

                List<VipInventory> sharedVisibleToB = queryVip {
                    conditions = ["uuid=${vipAdmin.uuid}".toString()]
                    sessionId = sessionB.uuid
                } as List<VipInventory>
                assert sharedVisibleToB.size() == 1

                PortForwardingRuleInventory pfShared = createPortForwardingRule {
                    name = "pf-shared-vip"
                    vipUuid = vipAdmin.uuid
                    vipPortStart = 8081
                    privatePortStart = 81
                    protocolType = PortForwardingProtocolType.TCP.toString()
                    vmNicUuid = vmB.vmNics[0].uuid
                    sessionId = sessionB.uuid
                } as PortForwardingRuleInventory

                deletePortForwardingRule {
                    uuid = pfShared.uuid
                    sessionId = sessionB.uuid
                }
            } finally {
                deleteVip {
                    uuid = vipAdmin.uuid
                }
            }
        } finally {
            deleteAccount { uuid = accB.uuid }
            deleteAccount { uuid = accA.uuid }
        }
    }

    void testCreateVipIgnoresSystemFieldInRestPayload() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory
        String sessionUuid = adminSession()
        OkHttpClient client = new OkHttpClient()

        def postRawVip = { Map paramsMap ->
            String bodyJson = JSONObjectUtil.toJsonString([params: paramsMap])
            Request createReq = new Request.Builder()
                    .url("http://127.0.0.1:${WebBeanConstructor.port}/v1/vips")
                    .addHeader("Authorization", "OAuth ${sessionUuid}")
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), bodyJson))
                    .build()
            String jobUrl = null
            Response resp = client.newCall(createReq).execute()
            try {
                if (resp.code() != 202) {
                    return [code: resp.code(), body: JSONObjectUtil.toObject(resp.body().string(), LinkedHashMap.class)]
                }
                Map locBody = JSONObjectUtil.toObject(resp.body().string(), LinkedHashMap.class)
                String rawLocation = locBody.get("location") as String
                String suffix = rawLocation.contains("/v1/api-jobs")
                        ? rawLocation.substring(rawLocation.indexOf("/v1/api-jobs"))
                        : rawLocation
                jobUrl = suffix.startsWith("http") ? suffix : "http://127.0.0.1:${WebBeanConstructor.port}${suffix}"
            } finally {
                resp.close()
            }
            for (int i = 0; i < 100; i++) {
                Request pollReq = new Request.Builder()
                        .url(jobUrl)
                        .addHeader("Authorization", "OAuth ${sessionUuid}")
                        .get()
                        .build()
                Response pollResp = client.newCall(pollReq).execute()
                try {
                    if (pollResp.code() != 202) {
                        return [code: pollResp.code(), body: JSONObjectUtil.toObject(pollResp.body().string(), LinkedHashMap.class)]
                    }
                } finally {
                    pollResp.close()
                }
                Thread.sleep(100)
            }
            throw new IllegalStateException("timed out polling async createVip job: ${jobUrl}")
        }

        Map created = postRawVip([
                name         : "vip-raw-system",
                l3NetworkUuid: pubL3.uuid,
                system       : true
        ])
        assert created.code == 200 : "expected 200 for createVip, got ${created}"
        Map inv = (created.body as Map).get("inventory") as Map
        String vipUuid = inv.get("uuid") as String
        String vipIp = inv.get("ip") as String
        assert inv.get("system") == false
        assert Q.New(VipVO.class).eq(VipVO_.uuid, vipUuid).select(VipVO_.system).findValue() == false

        Map duplicateAttempt = postRawVip([
                name         : "vip-raw-dup-ip",
                l3NetworkUuid: pubL3.uuid,
                requiredIp   : vipIp,
                system       : true
        ])
        assert duplicateAttempt.code == 503 : "expected duplicate IP createVip with system:true to fail, got ${duplicateAttempt}"

        deleteVip {
            uuid = vipUuid
        }
        assert !Q.New(VipVO.class).eq(VipVO_.uuid, vipUuid).isExists()
    }

    private static void assertPermissionDenied(org.zstack.sdk.ErrorCode err) {
        assert err != null
        if (err.code == IdentityErrors.PERMISSION_DENIED.toString() || err.globalErrorCode?.startsWith("ORG_ZSTACK_IDENTITY")) {
            return
        }
        assert err.code == "SYS.1006" : "expected ID.1002 or RBAC SYS.1006, got ${err.code} (${err.globalErrorCode}): ${err.details}"
        String combined = "${err.details ?: ''} ${(err instanceof org.zstack.sdk.ErrorCodeList && err.causes) ? err.causes.collect { "${it.code}:${it.globalErrorCode}:${it.details}" }.join(' ') : ''}".toLowerCase()
        assert combined.contains("org_zstack_identity") || combined.contains("permission denied") || combined.contains("has no access") ||
                combined.contains("doesn't belong") || combined.contains("无法使用资源") || combined.contains("没有权限") ||
                combined.contains("不属于") || combined.contains("id.1002") :
                "unexpected SYS.1006 error details (${err.globalErrorCode}): ${combined}"
    }

    @Override
    void clean() {
        env.delete()
    }
}
