package org.zstack.tag;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.tag.*;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SystemTag {
    private static final CLogger logger = Utils.getLogger(SystemTag.class);

    @Autowired
    protected DatabaseFacade dbf;

    // TagManager must be explicitly set. use @Autowried will cause circular dependency
    protected TagManager tagMgr;

    protected String tagFormat;
    protected Class resourceClass;
    protected List<SystemTagValidator> validators = new ArrayList<SystemTagValidator>();
    protected List<SystemTagLifeCycleListener> lifeCycleListeners = new ArrayList<SystemTagLifeCycleListener>();

    public SystemTag(String tagFormat, Class resourceClass) {
        this.tagFormat = tagFormat;
        this.resourceClass = resourceClass;
    }

    public void installLifeCycleListener(SystemTagLifeCycleListener listener) {
        lifeCycleListeners.add(listener);
    }

    void fireLifeCycleListener(SystemTagInventory tag, boolean isDeleted) {
        for (SystemTagLifeCycleListener ext : lifeCycleListeners) {
            try {
                if (isDeleted) {
                    ext.tagDeleted(tag);
                } else {
                    ext.tagCreated(tag);
                }
            } catch (Exception e) {
                logger.warn(String.format("unhandled exception when calling %s", ext.getClass()), e);
            }
        }
    }

    protected String useTagFormat() {
        return tagFormat;
    }

    protected Op useOp() {
        return Op.EQ;
    }

    public boolean hasTag(String resourceUuid) {
        return hasTag(resourceUuid, resourceClass);
    }

    public boolean hasTag(String resourceUuid, Class resourceClass) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.add(SystemTagVO_.resourceType, Op.EQ, resourceClass.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, resourceUuid);
        q.add(SystemTagVO_.tag, useOp(), useTagFormat());
        return q.isExists();
    }

    public List<String> getTags(String resourceUuid, Class resourceClass) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.tag);
        q.add(SystemTagVO_.resourceType, Op.EQ, resourceClass.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.EQ, resourceUuid);
        q.add(SystemTagVO_.tag, useOp(), useTagFormat());
        return q.listValue();
    }

    public List<String> getTags(String resourceUuid) {
        return getTags(resourceUuid, resourceClass);
    }

    public String getTag(String resourceUuid, Class resourceClass) {
        List<String> tags = getTags(resourceUuid, resourceClass);
        if (tags.isEmpty()) {
            return null;
        }
        return tags.get(0);
    }

    public String getTag(String resourceUuid) {
        return getTag(resourceUuid, resourceClass);
    }

    public Map<String, List<String>> getTags(List<String> resourceUuids, Class resourceClass) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.tag, SystemTagVO_.resourceUuid);
        q.add(SystemTagVO_.resourceType, Op.EQ, resourceClass.getSimpleName());
        q.add(SystemTagVO_.resourceUuid, Op.IN, resourceUuids);
        q.add(SystemTagVO_.tag, useOp(), useTagFormat());
        List<Tuple> ts = q.listTuple();
        Map<String, List<String>> ret = new HashMap<String, List<String>>();
        for (Tuple t : ts) {
            String uuid = t.get(1, String.class);
            List<String> tags = ret.get(uuid);
            if (tags == null) {
                tags = new ArrayList<String>();
                ret.put(uuid, tags);
            }
            tags.add(t.get(0, String.class));
        }
        return ret;
    }

    public Map<String, List<String>> getTags(List<String> resourceUuids) {
        DebugUtils.Assert(!resourceUuids.isEmpty(), "how can you pass an empty resourceUuids");
        return getTags(resourceUuids, resourceClass);
    }

    public String getTagFormat() {
        return tagFormat;
    }

    public Class getResourceClass() {
        return resourceClass;
    }


    public void delete(String resourceUuid) {
        tagMgr.deleteSystemTag(tagFormat, resourceUuid, resourceClass.getSimpleName(), false);
    }

    public void deleteInherentTag(String resourceUuid) {
        tagMgr.deleteSystemTag(tagFormat, resourceUuid, resourceClass.getSimpleName(), true);
    }

    private SystemTagInventory createTag(String resourceUuid, Class resourceClass, boolean inherent, boolean recreate) {
        if (recreate) {
            tagMgr.deleteSystemTag(tagFormat, resourceUuid, resourceClass.getSimpleName(), inherent);
        }

        if (inherent) {
            return (SystemTagInventory) tagMgr.createSysTag(resourceUuid, tagFormat, resourceClass.getSimpleName());
        } else {
            return tagMgr.createNonInherentSystemTag(resourceUuid, tagFormat, resourceClass.getSimpleName());
        }
    }

    public SystemTagInventory createTag(String resourceUuid, Class resourceClass) {
        return createTag(resourceUuid, resourceClass, false, false);
    }

    public SystemTagInventory createTag(String resourceUuid) {
        return createTag(resourceUuid, resourceClass, false, false);
    }

    public SystemTagInventory createInherentTag(String resourceUuid, Class resourceClass) {
        return createTag(resourceUuid, resourceClass, true, false);
    }

    public SystemTagInventory createInherentTag(String resourceUuid) {
        return createTag(resourceUuid, resourceClass, true, false);
    }

    public SystemTagInventory reCreateTag(String resourceUuid, Class resourceClass) {
        return createTag(resourceUuid, resourceClass, false, true);
    }

    public SystemTagInventory reCreateTag(String resourceUuid) {
        return createTag(resourceUuid, resourceClass, false, true);
    }

    public SystemTagInventory reCreateInherentTag(String resourceUuid, Class resourceClass) {
        return createTag(resourceUuid, resourceClass, true, true);
    }

    public SystemTagInventory reCreateInherentTag(String resourceUuid) {
        return createTag(resourceUuid, resourceClass, true, true);
    }

    public void installValidator(SystemTagValidator validator) {
        validators.add(validator);
    }

    public boolean isMatch(String tag) {
        return tagFormat.equals(tag);
    }

    void validate(String resourceUuid, Class resourceType, String tag) {
        for (SystemTagValidator validator : validators) {
            validator.validateSystemTag(resourceUuid, resourceType, tag);
        }
    }

    void setTagMgr(TagManager tagMgr) {
        this.tagMgr = tagMgr;
    }

    public List<SystemTagValidator> getValidators() {
        return validators;
    }

    public void setValidators(List<SystemTagValidator> validators) {
        this.validators = validators;
    }
}
