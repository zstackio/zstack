package org.zstack.externalStorage.primary;

import groovy.lang.Tuple;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmAllocateHostAndPrimaryStorageFlow;
import org.zstack.compute.vm.VmAllocateHostFlow;
import org.zstack.core.db.Q;
import org.zstack.externalStorage.primary.kvm.ExternalPrimaryStorageKvmFactory;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.storage.addon.primary.PrimaryStorageControllerSvc;
import org.zstack.header.storage.addon.primary.StorageCapabilities;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.vm.MarshalVmOperationFlowExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageFactory;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageSpaceHelper;
import org.zstack.storage.addon.primary.ExternalPrimaryStorageSystemTags;
import org.zstack.storage.primary.ImageCacheUtil;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class ExternalPrimaryStorageAllocator implements MarshalVmOperationFlowExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ExternalPrimaryStorageAllocator.class);

    @Autowired
    private ExternalPrimaryStorageFactory extPsFactory;

    @Override
    public Flow marshalVmOperationFlow(String previousFlowName, String nextFlowName, FlowChain chain, VmInstanceSpec spec) {
        if (VmAllocateHostAndPrimaryStorageFlow.class.getName().equals(nextFlowName)) {
            if (spec.getCurrentVmOperation() != VmInstanceConstant.VmOperation.NewCreate || !spec.getImageSpec().relyOnImageCache()) {
                return null;
            }

            if (extPsFactory.getAllControllerSvcs().isEmpty()) {
                return null;
            }

            List<String> candidatePsUuid = spec.getCandidatePrimaryStorageUuidsForRootVolume();
            List<Tuple> ts = null;
            if (candidatePsUuid == null || candidatePsUuid.isEmpty()) {
                ts = Q.New(ImageCacheVO.class).select(ImageCacheVO_.primaryStorageUuid, ImageCacheVO_.installUrl)
                        .eq(ImageCacheVO_.imageUuid, spec.getImageSpec().getInventory().getUuid())
                        .listValues();
                candidatePsUuid = ts.stream().map(it -> (String) it.get(0)).distinct().collect(Collectors.toList());
            }

            // TODO only consider the situation that there is only one candidate primary storage such as fast create
            if (candidatePsUuid.size() != 1) {
                logger.warn(String.format("ExternalPrimaryStorageAllocator only supports one candidate primary storage, found candidates: %s", candidatePsUuid));
                return null;
            }

            String requiredPsUuid = candidatePsUuid.get(0);
            PrimaryStorageControllerSvc controller = extPsFactory.getControllerSvc(requiredPsUuid);
            if (controller == null) {
                return null;
            }
            StorageCapabilities cap = controller.reportCapabilities();
            if (!cap.isSupportMultiSpace() || cap.isSupportCloneFromAnotherSpace()) {
                return null;
            }

            List<String> rootSysTags = spec.getRootVolumeSystemTags();
            List<String> cacheInstallUrls = new ArrayList<>();
            if (ts != null) {
                for (Tuple t : ts) {
                    if (requiredPsUuid.equals(t.get(0))) {
                        cacheInstallUrls.add(ImageCacheUtil.getImageCachePath((String) t.get(1)));
                    }
                }
            } else {
                cacheInstallUrls = Q.New(ImageCacheVO.class).select(ImageCacheVO_.installUrl)
                        .eq(ImageCacheVO_.imageUuid, spec.getImageSpec().getInventory().getUuid())
                        .eq(ImageCacheVO_.primaryStorageUuid, requiredPsUuid)
                        .listValues().stream().map(it -> ImageCacheUtil.getImageCachePath((String) it)).collect(Collectors.toList());
            }
            if (cacheInstallUrls.isEmpty()) {
                logger.warn(String.format("no candidate URLs found for image cache[uuid:%s] on primary storage[uuid:%s]",
                        spec.getImageSpec().getInventory().getUuid(), requiredPsUuid));
                return null;
            }

            ExternalPrimaryStorageSpaceHelper helper = new ExternalPrimaryStorageSpaceHelper(requiredPsUuid, controller.getIdentity());
            List<String> candidateUrls = cacheInstallUrls.stream()
                    .map(helper::getLocationSpaceUrl)
                    .collect(Collectors.toList());

            String requiredUrlTag = rootSysTags == null ? null :
                    rootSysTags.stream().filter(ExternalPrimaryStorageSystemTags.REQUIRED_INSTALL_URL::isMatch).findFirst().orElse(null);
            if (requiredUrlTag != null) {
                String requiredUrl = ExternalPrimaryStorageSystemTags.REQUIRED_INSTALL_URL.getTokenByTag(
                        requiredUrlTag, ExternalPrimaryStorageSystemTags.REQUIRED_INSTALL_URL_TOKEN).replaceAll("/$", "");
                if (candidateUrls.contains(requiredUrl)) {
                    return null;
                } else {
                    // DEBT: NoRollbackFlow — reason TBD
                    return new NoRollbackFlow() {
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            trigger.fail(operr(ORG_ZSTACK_EXTERNALSTORAGE_PRIMARY_10000, "creation relies on image cache[uuid:%s, locate urls: [%s]], cannot create other places.", spec.getImageSpec().getInventory().getUuid(), candidateUrls));

                        }
                    };
                }
            } else {
                spec.addRootVolumeSystemTag(ExternalPrimaryStorageSystemTags.REQUIRED_INSTALL_URL.instantiateTag(
                        Collections.singletonMap(ExternalPrimaryStorageSystemTags.REQUIRED_INSTALL_URL_TOKEN, candidateUrls.get(0)))
                );
            }
        }

        return null;
    }
}
