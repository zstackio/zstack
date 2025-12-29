package org.zstack.header.storage.addon.primary;

public class PingResult {
    private AddonInfo addonInfo;
    private boolean success = true;
    private String error;

    public PingResult(AddonInfo addonInfo, String error) {
        this.addonInfo = addonInfo;
        this.success = false;
        this.error = error;
    }

    public PingResult(AddonInfo addonInfo) {
        this.addonInfo = addonInfo;
    }

    public AddonInfo getAddonInfo() {
        return addonInfo;
    }

    public void setAddonInfo(AddonInfo addonInfo) {
        this.addonInfo = addonInfo;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
