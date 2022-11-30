/*
 * Copyright (c) 2013 Juniper Networks, Inc. All rights reserved.
 */
package org.zstack.sugonSdnController.controller.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.Serializable;

public class ObjectReference<AttrType extends ApiPropertyBase> implements Serializable {
    private final List<String> to;
    private final AttrType attr;
    private final String href;
    private final String uuid;

    public ObjectReference(List<String> to, AttrType attr) {
        this(to, attr, null, null);
    }
    public ObjectReference(String uuid) {
        this(null, null, null, uuid);
    }
    public ObjectReference(List<String> to, AttrType attr, String href, String uuid) {
        this.to = Collections.unmodifiableList(new ArrayList<String>(to));
        this.attr = attr;
        this.href = href;
        this.uuid = uuid;
    }
    public String getHRef() {
        return href;
    }
    public String getUuid() {
        return uuid;
    }
    public List<String> getReferredName() {
        return to;
    }
    public AttrType getAttr() {
        return attr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectReference)) return false;

        ObjectReference<?> that = (ObjectReference<?>) o;

        if (to != null && that.to != null)
            return to.equals(that.to);
        return uuid != null ? uuid.equals(that.uuid) : that.uuid == null;
    }

    @Override
    public int hashCode() {
        int result = to != null ? to.hashCode() : 0;
        result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
        return result;
    }

    public static <T extends ApiPropertyBase> String getReferenceListUuid(List<ObjectReference<T>> reflist) {
        if (reflist != null && !reflist.isEmpty()) {
            ObjectReference<T> ref = reflist.get(0);
            return ref.getUuid();
        }
        return null;
    }
}
