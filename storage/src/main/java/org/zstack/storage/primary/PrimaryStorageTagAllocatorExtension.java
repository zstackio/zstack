package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.DiskOfferingTagAllocatorExtensionPoint;
import org.zstack.header.allocator.HostAllocatorError;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.InstanceOfferingTagAllocatorExtensionPoint;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.PrimaryStorageTagAllocatorExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.TagInventory;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import static org.zstack.core.Platform.operr;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class PrimaryStorageTagAllocatorExtension implements InstanceOfferingTagAllocatorExtensionPoint,
        PrimaryStorageTagAllocatorExtensionPoint, DiskOfferingTagAllocatorExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Transactional(readOnly = true)
    private List<HostVO> uuidTagAllocateHost(List<HostVO> candidates, String psUuid) {
        List<String> hostUuids = CollectionUtils.transformToList(candidates, new Function<String, HostVO>() {
            @Override
            public String call(HostVO arg) {
                return arg.getUuid();
            }
        });

        String sql = "select h from HostVO h where h.clusterUuid in (select ref.clusterUuid from PrimaryStorageClusterRefVO ref where ref.primaryStorageUuid = :psUuid) and h.uuid in (:huuids)";
        TypedQuery<HostVO> q = dbf.getEntityManager().createQuery(sql, HostVO.class);
        q.setParameter("psUuid", psUuid);
        q.setParameter("huuids", hostUuids);
        candidates = q.getResultList();

        if (candidates.isEmpty()) {
            throw new OperationFailureException(errf.instantiateErrorCode(HostAllocatorError.NO_AVAILABLE_HOST,
                    String.format("cannot find host whose cluster has attached to primary storage[uuid:%s]. The primary storage uuid is specified in instance offering tag", psUuid)
            ));
        }

        return candidates;
    }

    @Override
    public List<HostVO> allocateHost(List<TagInventory> tags, List<HostVO> candidates, HostAllocatorSpec spec) {
        if (!VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            return candidates;
        }

        for (TagInventory tag : tags) {
            String uuid = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.getTokenByTag(tag.getTag(), "uuid");
            if (uuid != null) {
                return uuidTagAllocateHost(candidates, uuid);
            }

            String requiredUserTag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG_MANDATORY.getTokenByTag(tag.getTag(), "tag");
            if (requiredUserTag != null) {
                return userTagAllocateHost(candidates, requiredUserTag, true);
            }

            String userTag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG.getTokenByTag(tag.getTag(), "tag");
            if (userTag != null) {
                return userTagAllocateHost(candidates, userTag, false);
            }
        }

        return candidates;
    }

    @Transactional(readOnly = true)
    private List<HostVO> userTagAllocateHost(List<HostVO> candidates, String tag, boolean required) {
        List<String> hostUuids = CollectionUtils.transformToList(candidates, new Function<String, HostVO>() {
            @Override
            public String call(HostVO arg) {
                return arg.getUuid();
            }
        });

        String sql = "select h from HostVO h where h.clusterUuid in (select ref.clusterUuid from PrimaryStorageClusterRefVO ref where ref.primaryStorageUuid in (select t.resourceUuid from UserTagVO t where t.tag = :tag and t.resourceType = :resourceType)) and h.uuid in (:huuids)";
        TypedQuery<HostVO> q = dbf.getEntityManager().createQuery(sql, HostVO.class);
        q.setParameter("tag", tag);
        q.setParameter("resourceType", PrimaryStorageVO.class.getSimpleName());
        q.setParameter("huuids", hostUuids);
        List<HostVO> vos = q.getResultList();

        if (vos.isEmpty() && required) {
            throw new OperationFailureException(errf.instantiateErrorCode(HostAllocatorError.NO_AVAILABLE_HOST,
                    String.format("cannot find host whose cluster has attached to primary storage having user tag[%s]. The user tag is specified in instance offering tag", tag)
            ));
        } else if (vos.isEmpty()) {
            return candidates;
        } else {
            return vos;
        }
    }

    @Override
    public List<PrimaryStorageVO> allocatePrimaryStorage(List<SystemTagInventory> tags, List<PrimaryStorageVO> candidates) {
        for (SystemTagInventory tag : tags) {
            final String uuid = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.getTokenByTag(tag.getTag(), "uuid");
            if (uuid != null) {
                PrimaryStorageVO pvo = CollectionUtils.find(candidates, new Function<PrimaryStorageVO, PrimaryStorageVO>() {
                    @Override
                    public PrimaryStorageVO call(PrimaryStorageVO arg) {
                        return uuid.equals(arg.getUuid()) ? arg : null;
                    }
                });

                if (pvo == null) {
                    throw new OperationFailureException(operr("cannot find primary storage[uuid:%s], the uuid is specified in instance offering or disk offering", uuid));
                }

                List<PrimaryStorageVO> psvos = new ArrayList<PrimaryStorageVO>();
                psvos.add(pvo);
                return psvos;
            }

            String requiredUserTag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG_MANDATORY.getTokenByTag(tag.getTag(), "tag");
            if (requiredUserTag != null) {
                return allocatePrimaryStorageByUserTag(requiredUserTag, candidates, true);
            }

            String userTag = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_USERTAG_TAG.getTokenByTag(tag.getTag(), "tag");
            if (userTag != null) {
                return allocatePrimaryStorageByUserTag(userTag, candidates, false);
            }
        }

        return candidates;
    }


    @Transactional(readOnly = true)
    private List<PrimaryStorageVO> allocatePrimaryStorageByUserTag(String tag, List<PrimaryStorageVO> candidates, boolean required) {
        List<String> uuids = CollectionUtils.transformToList(candidates, new Function<String, PrimaryStorageVO>() {
            @Override
            public String call(PrimaryStorageVO arg) {
                return arg.getUuid();
            }
        });

        String sql = "select ps from PrimaryStorageVO ps where ps.uuid in (:uuids) and ps.uuid in (select t.resourceUuid from UserTagVO t where t.tag = :tag and t.resourceType = :resourceType)";
        TypedQuery<PrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, PrimaryStorageVO.class);
        q.setParameter("uuids", uuids);
        q.setParameter("tag", tag);
        q.setParameter("resourceType", PrimaryStorageVO.class.getSimpleName());
        List<PrimaryStorageVO> vos = q.getResultList();

        if (vos.isEmpty() && required) {
            throw new OperationFailureException(operr("cannot find primary storage having user tag[%s]. The user tag is specified in instance offering or disk offering", tag));
        } else if (vos.isEmpty()) {
            return candidates;
        } else {
            return vos;
        }
    }
}
