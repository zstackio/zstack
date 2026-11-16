package org.zstack.test.unittest.compute

import org.junit.Before
import org.junit.Test
import org.zstack.compute.vm.VmInstanceManagerImpl
import org.zstack.compute.vm.VmQuotaConstant
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.identity.quota.QuotaMessageHandler
import org.zstack.header.image.ImageConstant
import org.zstack.header.vm.APICreateVmInstanceMsg
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.function.Function

import javax.persistence.EntityManager
import javax.persistence.Tuple
import javax.persistence.TypedQuery

import static org.mockito.Mockito.*

class VmDataVolumeTemplateQuotaCase {
    Function<Long, APICreateVmInstanceMsg> requiredCapacity
    TypedQuery<Long> templateQuery

    @Before
    void setup() {
        def dbf = mock(DatabaseFacade.class)
        def entityManager = mock(EntityManager.class)
        def rootQuery = mock(TypedQuery.class)
        def root = mock(Tuple.class)
        templateQuery = mock(TypedQuery.class)
        when(dbf.getEntityManager()).thenReturn(entityManager)
        when(entityManager.createQuery(
                'select img.size, img.mediaType from ImageVO img where img.uuid = :iuuid', Tuple.class))
                .thenReturn(rootQuery)
        when(rootQuery.getSingleResult()).thenReturn(root)
        when(root.get(0, Long.class)).thenReturn(SizeUnit.GIGABYTE.toByte(1))
        when(root.get(1, ImageConstant.ImageMediaType.class))
                .thenReturn(ImageConstant.ImageMediaType.RootVolumeTemplate)
        when(entityManager.createQuery('select size from ImageVO where uuid = :uuid', Long.class))
                .thenReturn(templateQuery)
        when(templateQuery.getSingleResult()).thenReturn(SizeUnit.GIGABYTE.toByte(5))

        def manager = new VmInstanceManagerImpl()
        manager.@dbf = dbf
        QuotaMessageHandler<APICreateVmInstanceMsg> handler = manager.reportQuota()[0]
                .quotaMessageCheckerList.find { it.messageClass == APICreateVmInstanceMsg.class }
        requiredCapacity = handler.functionalSizeRequiredRequests
                .find { it.quotaName == VmQuotaConstant.VOLUME_SIZE }.function
    }

    @Test
    void includesDataVolumeTemplateCapacity() {
        def msg = new APICreateVmInstanceMsg(imageUuid: 'root', diskAOs: [
                new APICreateVmInstanceMsg.DiskAO(templateUuid: 'data-template')])
        assert requiredCapacity.call(msg) == SizeUnit.GIGABYTE.toByte(6)
        verify(templateQuery).setParameter('uuid', 'data-template')
    }

    @Test
    void countsEachNewDiskWhenTemplateIsReused() {
        def msg = new APICreateVmInstanceMsg(imageUuid: 'root', diskAOs: [
                new APICreateVmInstanceMsg.DiskAO(templateUuid: 'data-template'),
                new APICreateVmInstanceMsg.DiskAO(templateUuid: 'data-template')])
        assert requiredCapacity.call(msg) == SizeUnit.GIGABYTE.toByte(11)
    }

    @Test
    void doesNotCountBootTemplateTwice() {
        def msg = new APICreateVmInstanceMsg(imageUuid: 'root', diskAOs: [
                new APICreateVmInstanceMsg.DiskAO(boot: true, templateUuid: 'root'),
                new APICreateVmInstanceMsg.DiskAO(templateUuid: 'data-template')])
        assert requiredCapacity.call(msg) == SizeUnit.GIGABYTE.toByte(6)
        verify(templateQuery, never()).setParameter('uuid', 'root')
    }

    @Test
    void doesNotChargeExistingVolumeAgain() {
        def msg = new APICreateVmInstanceMsg(imageUuid: 'root', diskAOs: [
                new APICreateVmInstanceMsg.DiskAO(sourceUuid: 'existing', sourceType: 'VolumeVO')])
        assert requiredCapacity.call(msg) == SizeUnit.GIGABYTE.toByte(1)
        verifyNoInteractions(templateQuery)
    }

    @Test
    void preservesRequestsWithoutDiskAOs() {
        assert requiredCapacity.call(new APICreateVmInstanceMsg(imageUuid: 'root')) == SizeUnit.GIGABYTE.toByte(1)
        verifyNoInteractions(templateQuery)
    }
}
