package org.zstack.header.storage.addon.primary;

import org.zstack.utils.gson.JSONObjectUtil;

import java.io.Serializable;

public interface AddonInfo extends Serializable {
    default boolean changed(String infoJson) {
        return !this.serialize().equals(infoJson);
    }

    default String serialize() {
        return JSONObjectUtil.toJsonString(this);
    }
}
