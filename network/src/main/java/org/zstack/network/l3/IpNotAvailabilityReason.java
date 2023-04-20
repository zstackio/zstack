package org.zstack.network.l3;

import static org.zstack.core.Platform.i18n;

public enum IpNotAvailabilityReason {
    GATEWAY("it is gateway"),
    NO_IN_RANGE("it is not in this range"),
    USED("it is used");

    String type;

    IpNotAvailabilityReason(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public String toi18nString() {
        if (this.equals(GATEWAY)) {
            return i18n("it is gateway");
        } else if (this.equals(NO_IN_RANGE)) {
            return i18n("it is not in this range");
        } else if (this.equals(USED)) {
            return i18n("it is used");
        } else {
            return this.toString();
        }
    }
}
