package org.zstack.storage.addon.primary;

public class ExternalPrimaryStorageCanonicalEvent {
    public static final String ADDON_INFO_CHANGED_PATH = "/externalPrimaryStorage/addonInfo/changed";

    public static class AddonInfoChangedData {
        private String uuid;
        private String addonInfo;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getAddonInfo() {
            return addonInfo;
        }

        public void setAddonInfo(String addonInfo) {
            this.addonInfo = addonInfo;
        }
    }
}
