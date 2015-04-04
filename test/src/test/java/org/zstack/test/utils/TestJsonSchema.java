package org.zstack.test.utils;

import org.junit.Test;
import org.mvel2.MVEL;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.MessageJsonSchemaBuilder;
import org.zstack.header.apimediator.APIIsReadyToGoMsg;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedJsonSchema;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.storage.backup.sftp.SftpBackupStorageInventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.io.Serializable;
import java.util.*;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 */
public class TestJsonSchema {

    public static class A {
        public ZoneInventory zone;

        public Map<String, HostInventory> hosts;

        public List<ClusterInventory> clusters;

        public B b;

        @NeedJsonSchema
        public String string;

    }

    public static class B {
        VmInstanceInventory vm;

        List<BackupStorageInventory> sftps = new ArrayList<BackupStorageInventory>();

        public VmInstanceInventory getVm() {
            return vm;
        }

        public void setVm(VmInstanceInventory vm) {
            this.vm = vm;
        }

        public List<BackupStorageInventory> getSftps() {
            return sftps;
        }

        public void setSftps(List<BackupStorageInventory> sftps) {
            this.sftps = sftps;
        }
    }

    @Test
    public void test() {
        A a = new A();
        a.zone = new ZoneInventory();
        a.zone.setName("zone1");
        a.hosts = new HashMap<String, HostInventory>();
        a.hosts.put("host1", new HostInventory());
        a.clusters = new ArrayList<ClusterInventory>();
        a.clusters.add(new ClusterInventory());
        a.b = new B();
        a.b.vm = new VmInstanceInventory();
        VmNicInventory nic1 = new VmNicInventory();
        nic1.setInternalName("nic1Name");
        a.b.vm.setVmNics(Arrays.asList(nic1));
        a.b.sftps.add(new SftpBackupStorageInventory());
        a.b.sftps.add(new SftpBackupStorageInventory());
        a.b.sftps.add(new BackupStorageInventory());
        a.string = "a string";

        /*
        Map<String, String> ret = MessageJsonSchemaBuilder.buildSchema(a);
        for (Map.Entry<String, String> e : ret.entrySet()) {
            System.out.println(String.mediaType("%s: %s", e.getKey(), e.value()));
        }

        HashMap m = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(a), HashMap.class);
        String name = (String) MVEL.eval("b.vm.vmNics[0].internalName", m);
        System.out.println(name);
        */

        APIIsReadyToGoMsg msg = new APIIsReadyToGoMsg();
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setManagementNodeId(Platform.getManagementServerId());
        MessageJsonSchemaBuilder.buildSchema(msg);

        Map vars = map(e("nicName", "nic2"));
        nic1 = (VmNicInventory) MVEL.eval("b.vm.vmNics[0]", a);
        System.out.println(nic1.getInternalName());
        MVEL.eval("b.vm.vmNics[0].internalName = nicName", a, vars);
        System.out.println(nic1.getInternalName());
        VmNicInventory nic2 = new VmNicInventory();
        nic2.setInternalName("this is a niw nic");
        vars.put("newNic", nic2);
        Serializable compiled = MVEL.compileExpression("b.vm.vmNics[0] = newNic");
        MVEL.executeExpression(compiled, a, vars);
        nic1 = (VmNicInventory) MVEL.eval("b.vm.vmNics[0]", a);
        System.out.println(nic1.getInternalName());
    }
}
