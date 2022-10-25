/*
 * Copyright (c) 2013 Juniper Networks, Inc. All rights reserved.
 */
package org.zstack.sugonSdnController.controller.api;

import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public abstract class ApiObjectBase implements Serializable {
    private String name;
    private String uuid;
    private List<String> fq_name;
    private transient ApiObjectBase parent;
    private String parent_type;
    private String parent_uuid;

    public String getName() {
        if (name == null && fq_name != null) {
            name = fq_name.get(fq_name.size() - 1);
        }
        return name;
    }

    /**
     * Retrieves a parent object that may be cached in the object due to a setParent operation.
     * 
     * @return parent
     */
    public ApiObjectBase getParent() {
        return parent;
    }
    
    protected void setParent(ApiObjectBase parent) {
        this.parent = parent;
        this.fq_name = null;
        if (parent != null) {
            parent_type = parent.getObjectType();
            parent_uuid = parent.getUuid();
        } else {
            parent_type = null;
            parent_uuid = null;
        }
    }

    public void setName(String name) {
        this.name = name;
        if (fq_name != null) {
            fq_name.set(fq_name.size() - 1, name);
        }
    }

    public String getUuid() {
    return uuid;
    }
    public void setUuid(String uuid) {
    this.uuid = uuid;
    }

    public String getParentUuid() {
        return parent_uuid;
    }

    public String getParentType() {
        return parent_type;
    }

    protected void updateQualifiedName() {
        if (fq_name == null) {
            List<String> parent_qn;
            if (parent != null) {
                parent_qn = parent.getQualifiedName();
                parent_type = parent.getObjectType();
            } else {
                parent_qn = getDefaultParent();
                parent_type = getDefaultParentType();
                if (parent_qn == null)
                    throw new IllegalStateException("Parent of type " + getClass().getSimpleName() + " has to be specified explicitly.");
            }
            parent_qn.add(name);
            fq_name = parent_qn;
        }
    }

    public List<String> getQualifiedName() {
        if ("config-root".equals(getObjectType()))
            return new ArrayList<String>();

        updateQualifiedName();
        return new ArrayList<String>(fq_name);
    }

    public abstract String getObjectType();
    public abstract List<String> getDefaultParent();
    public abstract String getDefaultParentType();
}
