package org.zstack.test.integration.kvm.nic

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.Q
import org.zstack.header.tag.AbstractSystemTagLifeCycleListener
import org.zstack.header.tag.SystemTagInventory
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.*
import org.zstack.tag.SystemTag
import org.zstack.utils.gson.JSONObjectUtil

/** Real create/update APIs, plus failure injection at the tag transaction boundary. */
class VmNicDriverLifecycleCase extends UpdateVmNicDriverCase {
    @Override
    void test() {
        super.test()
        checkCreationDefaultsAndOverrides()
        checkTagTransactionRollback()
        checkTagCreationRollback()
        checkPluginPriorityAndConflicts()
        checkInternalTagCreationDuringStopAndReboot()
        checkSystemTagRename()
    }

    void checkSystemTagRename() {
        assert (queryVmInstance { conditions = ["uuid=${vm.uuid}".toString()] })[0].state == 'Stopped'
        def hostname = createSystemTag {
            resourceUuid = vm.uuid
            resourceType = 'VmInstanceVO'
            tag = 'hostname::nic-policy-a'
        }
        updateSystemTag { uuid = hostname.uuid; tag = 'hostname::nic-policy-b' }
        assert Q.New(SystemTagVO).eq(SystemTagVO_.uuid, hostname.uuid)
                .select(SystemTagVO_.tag).findValue() == 'hostname::nic-policy-b'
        String virtioUuid = Q.New(SystemTagVO).eq(SystemTagVO_.resourceUuid, vm.uuid)
                .eq(SystemTagVO_.tag, VmSystemTags.VIRTIO.tagFormat).select(SystemTagVO_.uuid).findValue()
        assert virtioUuid != null
        def drivers = [driverOf(nicUuid), driverOf(secondNicUuid)]
        [[hostname.uuid, VmSystemTags.VIRTIO.tagFormat], [virtioUuid, 'hostname::nic-policy-c']].each { change ->
            def result = new UpdateSystemTagAction(uuid: change[0], tag: change[1], sessionId: env.session.uuid).call()
            assert result.error != null
            assert JSONObjectUtil.toJsonString(result.error).contains('ORG_ZSTACK_TAG_10005')
            assert Q.New(SystemTagVO).eq(SystemTagVO_.uuid, hostname.uuid)
                    .select(SystemTagVO_.tag).findValue() == 'hostname::nic-policy-b'
            assert Q.New(SystemTagVO).eq(SystemTagVO_.uuid, virtioUuid)
                    .select(SystemTagVO_.tag).findValue() == VmSystemTags.VIRTIO.tagFormat
            assert [driverOf(nicUuid), driverOf(secondNicUuid)] == drivers
        }
        deleteTag { uuid = hostname.uuid }
    }

    void checkCreationDefaultsAndOverrides() {
        def img = env.inventoryByName("image1") as ImageInventory
        def l3 = env.inventoryByName("pubL3") as L3NetworkInventory
        def offering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        KVMAgentCommands.StartVmCmd command
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> entity ->
            command = JSONObjectUtil.toObject(entity.body, KVMAgentCommands.StartVmCmd.class)
            return rsp
        }
        ["Windows", "WindowsVirtio", "Linux", "Other", "Paravirtualization"].each { os ->
            [false, true].each { enabled ->
                updateImage { uuid = img.uuid; virtio = enabled; platform = os }
                (enabled ? [false] : [false, true]).each { requestVirtio ->
                    [null, "virtio", "e1000"].each { model ->
                        command = null
                        def created = createVmInstance {
                            name = "model-${os}-${enabled}-${model}"
                            imageUuid = img.uuid
                            instanceOfferingUuid = offering.uuid
                            l3NetworkUuids = [l3.uuid]
                            platform = os
                            if (os == "Other") { guestOsType = "FreeBSD" }
                            if (requestVirtio) { systemTags = [VmSystemTags.VIRTIO.tagFormat] }
                            if (model != null) {
                                vmNicParams = JSONObjectUtil.toJsonString([[l3NetworkUuid: l3.uuid, driverType: model]])
                            }
                        } as VmInstanceInventory
                        String expected = model ?: ((enabled || requestVirtio || os in ["Linux", "WindowsVirtio", "Paravirtualization"]) ? "virtio" : "e1000")
                        assert created.vmNics[0].driverType == expected
                        assert driverOf(created.vmNics[0].uuid) == expected
                        assert VmSystemTags.VIRTIO.hasTag(created.uuid) == (enabled || requestVirtio)
                        assert command.vmInstanceUuid == created.uuid
                        assert command.nics[0].driverType == expected
                        assert command.rootVolume.useVirtio == (enabled || requestVirtio)
                        destroyVmInstance { uuid = created.uuid }
                    }
                }
            }
        }
    }

    void checkTagTransactionRollback() {
        def tagUuid = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, vm.uuid)
                .eq(SystemTagVO_.tag, VmSystemTags.VIRTIO.tagFormat).select(SystemTagVO_.uuid).findValue() as String
        def before = [driverOf(nicUuid), driverOf(secondNicUuid)]
        def addresses = bean(org.zstack.header.vm.devices.VmInstanceDeviceManager)
        addresses.createOrUpdateVmDeviceAddress(nicUuid, null, vm.uuid, null, null)
        long addressCount = Q.New(org.zstack.header.vm.devices.VmInstanceDeviceAddressVO)
                .eq(org.zstack.header.vm.devices.VmInstanceDeviceAddressVO_.vmInstanceUuid, vm.uuid).count()
        assert addressCount > 0
        def listener = new AbstractSystemTagLifeCycleListener() {
            @Override
            void tagDeleted(SystemTagInventory tag) {
                if (tag.resourceUuid == vm.uuid) { throw new IllegalStateException("injected after NIC reset") }
            }
        }
        VmSystemTags.VIRTIO.installLifeCycleListener(listener)
        try {
            def result = new DeleteTagAction(uuid: tagUuid, sessionId: env.session.uuid).call()
            assert result.error != null
            assert VmSystemTags.VIRTIO.hasTag(vm.uuid)
            assert [driverOf(nicUuid), driverOf(secondNicUuid)] == before
            assert Q.New(org.zstack.header.vm.devices.VmInstanceDeviceAddressVO)
                    .eq(org.zstack.header.vm.devices.VmInstanceDeviceAddressVO_.vmInstanceUuid, vm.uuid).count() == addressCount
        } finally {
            def field = SystemTag.class.getDeclaredField("lifeCycleListeners")
            field.accessible = true
            (field.get(VmSystemTags.VIRTIO) as List).remove(listener)
        }
        deleteTag { uuid = tagUuid }
        assert !VmSystemTags.VIRTIO.hasTag(vm.uuid)
        assert driverOf(nicUuid) == "e1000"
        assert driverOf(secondNicUuid) == "e1000"
    }
    void checkTagCreationRollback() {
        def listener = new AbstractSystemTagLifeCycleListener() {
            @Override
            void tagCreated(SystemTagInventory tag) {
                if (tag.resourceUuid == vm.uuid) {
                    throw new org.zstack.header.apimediator.ApiMessageInterceptionException(
                            new org.zstack.header.errorcode.ErrorCode("SYS.1000", "injected after NIC reset"))
                }
            }
        }
        VmSystemTags.VIRTIO.installLifeCycleListener(listener)
        try {
            def result = new CreateSystemTagAction(resourceUuid: vm.uuid, resourceType: "VmInstanceVO",
                    tag: VmSystemTags.VIRTIO.tagFormat, sessionId: env.session.uuid).call()
            assert result.error != null
            assert !VmSystemTags.VIRTIO.hasTag(vm.uuid)
            assert [driverOf(nicUuid), driverOf(secondNicUuid)] == ["e1000", "e1000"]
            expect(org.zstack.header.errorcode.OperationFailureException) {
                bean(org.zstack.tag.TagManager).createInherentSystemTag(vm.uuid,
                        VmSystemTags.VIRTIO.tagFormat, "VmInstanceVO")
            }
            assert !VmSystemTags.VIRTIO.hasTag(vm.uuid)
            assert [driverOf(nicUuid), driverOf(secondNicUuid)] == ["e1000", "e1000"]
        } finally {
            def field = SystemTag.class.getDeclaredField("lifeCycleListeners")
            field.accessible = true
            (field.get(VmSystemTags.VIRTIO) as List).remove(listener)
        }
    }

    void checkPluginPriorityAndConflicts() {
        def registry = bean(org.zstack.core.componentloader.PluginRegistry)
        def manager = bean(org.zstack.compute.vm.VmNicManager)
        def extensions = registry.getExtensionList(org.zstack.header.vm.VmNicSetDriverExtensionPoint)
        def definitions = registry.getExtensionByInterfaceName(org.zstack.header.vm.VmNicSetDriverExtensionPoint.name)
        def a = { ignored -> "virtio" } as org.zstack.header.vm.VmNicSetDriverExtensionPoint
        def b = { ignored -> "e1000" } as org.zstack.header.vm.VmNicSetDriverExtensionPoint
        def highPriority = new org.zstack.core.componentloader.PluginExtension()
        highPriority.instance = a
        highPriority.order = 100
        def inventory = org.zstack.header.vm.VmInstanceInventory.valueOf(
                Q.New(org.zstack.header.vm.VmInstanceVO).eq(org.zstack.header.vm.VmInstanceVO_.uuid, vm.uuid).find())
        extensions.addAll([a, b])
        try {
            expect(org.zstack.header.errorcode.OperationFailureException) {
                manager.getDefaultNicDriver(inventory, false)
            }
            long before = Q.New(org.zstack.header.vm.VmNicVO).count()
            def result = new CreateVmInstanceAction(sessionId: env.session.uuid, name: "conflicting-defaults",
                    imageUuid: env.inventoryByName("image1").uuid,
                    instanceOfferingUuid: env.inventoryByName("instanceOffering").uuid,
                    l3NetworkUuids: [env.inventoryByName("pubL3").uuid]).call()
            assert result.error != null
            assert JSONObjectUtil.toJsonString(result.error).contains("ORG_ZSTACK_COMPUTE_VM_10347")
            assert Q.New(org.zstack.header.vm.VmNicVO).count() == before
            definitions.add(highPriority)
            assert manager.getDefaultNicDriver(inventory, false) == "virtio"
        } finally {
            definitions.remove(highPriority)
            extensions.remove(a)
            extensions.remove(b)
        }
    }

    void checkInternalTagCreationDuringStopAndReboot() {
        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> entity ->
            def command = JSONObjectUtil.toObject(entity.body, KVMAgentCommands.StartVmCmd.class)
            if (command.vmInstanceUuid == vm.uuid) { startCmd = command }
            return rsp
        }
        def registry = bean(org.zstack.core.componentloader.PluginRegistry)
        def states = []
        def extension = { spec, message ->
            if (spec.vmInventory.uuid != vm.uuid) { return }
            def state = Q.New(org.zstack.header.vm.VmInstanceVO)
                    .eq(org.zstack.header.vm.VmInstanceVO_.uuid, vm.uuid)
                    .select(org.zstack.header.vm.VmInstanceVO_.state).findValue()
            states.add(state.toString())
            // Same internal tag creator used by GuestTools; the API validator
            // must not reject this lifecycle operation.
            def creator = VmSystemTags.VIRTIO.newSystemTagCreator(vm.uuid)
            creator.inherent = false
            creator.recreate = true
            creator.create()
        } as org.zstack.compute.vm.BeforeStopVmOnHypervisorExtensionPoint
        registry.defineDynamicExtension(org.zstack.compute.vm.BeforeStopVmOnHypervisorExtensionPoint, extension)
        try {
            ['stop', 'reboot'].each { operation ->
                startVmInstance { uuid = vm.uuid }
                def tagUuid = Q.New(SystemTagVO).eq(SystemTagVO_.resourceUuid, vm.uuid)
                        .eq(SystemTagVO_.tag, VmSystemTags.VIRTIO.tagFormat)
                        .select(SystemTagVO_.uuid).findValue() as String
                if (tagUuid != null) { deleteTag { uuid = tagUuid } }
                assert !VmSystemTags.VIRTIO.hasTag(vm.uuid)
                assert [driverOf(nicUuid), driverOf(secondNicUuid)] == ['e1000', 'e1000']
                def result = new CreateSystemTagAction(resourceUuid: vm.uuid, resourceType: 'VmInstanceVO',
                        tag: VmSystemTags.VIRTIO.tagFormat, sessionId: env.session.uuid).call()
                assert result.error != null
                assert !VmSystemTags.VIRTIO.hasTag(vm.uuid)
                if (operation == 'stop') {
                    stopVmInstance { uuid = vm.uuid }
                } else {
                    startCmd = null
                    rebootVmInstance { uuid = vm.uuid }
                    assertStartCommand([(nicUuid): 'virtio', (secondNicUuid): 'virtio'], true)
                }
                assert VmSystemTags.VIRTIO.hasTag(vm.uuid)
                assert [driverOf(nicUuid), driverOf(secondNicUuid)] == ['virtio', 'virtio']
            }
            assert states == ['Stopping', 'Rebooting']
        } finally {
            registry.getExtensionList(org.zstack.compute.vm.BeforeStopVmOnHypervisorExtensionPoint).remove(extension)
        }
        stopVmInstance { uuid = vm.uuid }
    }

}
