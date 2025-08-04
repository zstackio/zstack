package org.zstack.sdnController.header;

public enum SdnControllerTableState {
    ENABLED("Enabled"),
    DISABLED("Disabled");

    private String value;
    private SdnControllerTableState(String value) {
        this.value = value;
    }


    @Override
    public String toString() {
        return value;
    }
}
