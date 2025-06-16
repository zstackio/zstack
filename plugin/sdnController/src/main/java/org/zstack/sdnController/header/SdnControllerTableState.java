package org.zstack.sdnController.header;

public enum SdnControllerTableState {
    Enabled("Enabled"),
    Disabled("Disabled");

    public final String value;
    private SdnControllerTableState(String value) {
        this.value = value;
    }


    @Override
    public String toString() {
        return value;
    }
}
