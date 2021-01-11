package org.zstack.test.integration.network.l2network

import org.springframework.http.HttpEntity
import org.zstack.core.asyncbatch.While
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.core.FutureCompletion
import org.zstack.header.core.NoErrorCompletion
import org.zstack.header.core.workflow.WhileCompletion
import org.zstack.header.network.l2.L2NetworkClusterRefVO
import org.zstack.header.network.l2.L2NetworkClusterRefVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.network.l3.NetworkGlobalProperty
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.test.integration.network.NetworkTest;
import org.zstack.testlib.EnvSpec;
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit;

/**
 * Created by heathhose on 17-5-6.
 */
public class AttachL2NetworkCase extends SubCase{

    EnvSpec env
    DatabaseFacade dbf
    @Override
    public void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    public void environment() {
        env = env {
            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster1"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }
                }

                cluster {
                    name = "cluster2"
                    hypervisorType = "KVM"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth0"

                }

                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth0"
                }

                l2VlanNetwork {
                    name = "l2-vlan-1"
                    vlan = 3001
                    physicalInterface = "eth0"
                }

                l2VlanNetwork {
                    name = "l2-vlan-2"
                    vlan = 3001
                    physicalInterface = "eth0"
                }

                l2VlanNetwork {
                    name = "l2-vlan-3"
                    vlan = 3003
                    physicalInterface = "eth0"
                }

            }
        }
    }

    @Override
    public void test() {
        env.create {
            testAttachL2NoVlanNetwork()
            testAttachL2ValnNetwork()
            testAttachL2NoVlanNetworkSynchronously()
            testAttachL2VlanNetworkSynchronously()
            testAddHost()
        }

    }

    void testAttachL2NoVlanNetwork(){
        L2NetworkInventory l21 = env.inventoryByName("l2-1")
        L2NetworkInventory l22 = env.inventoryByName("l2-2")
        ClusterInventory cluster = env.inventoryByName("cluster1")

        NetworkGlobalProperty.BRIDGE_DISABLE_IPTABLES = true
        KVMAgentCommands.CreateBridgeCmd createCmd = null
        env.afterSimulator(KVMConstant.KVM_REALIZE_L2NOVLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            createCmd = json(e.body, KVMAgentCommands.CreateBridgeCmd.class)
            return rsp
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = l21.uuid
            clusterUuid = cluster.uuid
        }
        assert createCmd.disableIptables == true

        retryInSecs{
            L2NetworkClusterRefVO ref = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid,cluster.uuid).find();
            assert ref.l2NetworkUuid == l21.uuid
            assert Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid,cluster.uuid).count() == 1l
        }

        expect(AssertionError.class){
            attachL2NetworkToCluster {
                l2NetworkUuid = l22.uuid
                clusterUuid = cluster.uuid
            }
        }

    }

    void testAttachL2ValnNetwork(){
        L2NetworkInventory l21 = env.inventoryByName("l2-vlan-1")
        L2NetworkInventory l22 = env.inventoryByName("l2-vlan-2")
        ClusterInventory cluster = env.inventoryByName("cluster1")

        KVMAgentCommands.CreateVlanBridgeCmd createCmd = null
        env.afterSimulator(KVMConstant.KVM_REALIZE_L2VLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            createCmd = json(e.body, KVMAgentCommands.CreateVlanBridgeCmd.class)
            return rsp
        }

        attachL2NetworkToCluster {
            l2NetworkUuid = l21.uuid
            clusterUuid = cluster.uuid
        }
        assert createCmd.disableIptables == true

        retryInSecs{
            assert Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid,cluster.uuid).count() == 2l
        }

        expect(AssertionError.class){
            attachL2NetworkToCluster {
                l2NetworkUuid = l22.uuid
                clusterUuid = cluster.uuid
            }
        }
    }

    void testAttachL2NoVlanNetworkSynchronously(){
        L2NetworkInventory l21 = env.inventoryByName("l2-1")
        L2NetworkInventory l22 = env.inventoryByName("l2-2")
        ClusterInventory cluster = env.inventoryByName("cluster2")
        def list = [l21.uuid,l22.uuid]
        new While<>(list).all(new While.Do<String>() {

            @Override
            void accept(String item, WhileCompletion completion) {
                attachL2NetworkToCluster {
                    l2NetworkUuid = item
                    clusterUuid = cluster.uuid
                }
                completion.done()
            }
        }).run(new NoErrorCompletion(){

            @Override
            void done() {

            }
        })

        retryInSecs{
            return {
                L2NetworkClusterRefVO ref = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid,cluster.uuid).find();
                assert ref.l2NetworkUuid == l21.uuid
                assert Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid,cluster.uuid).count() == 1l
            }
        }
    }

    void testAttachL2VlanNetworkSynchronously(){
        L2NetworkInventory l23 = env.inventoryByName("l2-vlan-3")
        L2NetworkInventory l22 = env.inventoryByName("l2-vlan-2")
        ClusterInventory cluster = env.inventoryByName("cluster2")
        def list = [l23.uuid,l22.uuid]
        new While<>(list).all(new While.Do<String>() {

            @Override
            void accept(String item, WhileCompletion completion) {
                attachL2NetworkToCluster {
                    l2NetworkUuid = item
                    clusterUuid = cluster.uuid
                }
                completion.done()
            }
        }).run(new NoErrorCompletion(){

            @Override
            void done() {

            }
        })

        retryInSecs{
            return {
                assert Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.clusterUuid,cluster.uuid).count() == 2l
            }
        }
    }

    void testAddHost(){
        ClusterInventory cluster = env.inventoryByName("cluster1")

        KVMAgentCommands.CreateBridgeCmd createCmd = null
        env.afterSimulator(KVMConstant.KVM_REALIZE_L2NOVLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            createCmd = json(e.body, KVMAgentCommands.CreateBridgeCmd.class)
            return rsp
        }

        env.afterSimulator(KVMConstant.KVM_REALIZE_L2VLAN_NETWORK_PATH) { rsp, HttpEntity<String> e ->
            assert createCmd != null
            return rsp
        }

        addKVMHost {
            name = "test"
            managementIp = "127.0.0.2"
            username = "root"
            password = "password"
            clusterUuid = cluster.uuid
        }
    }

    @Override
    public void clean() {
        env.delete()
    }
}
