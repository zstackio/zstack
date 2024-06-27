package org.zstack.xinfini;

public enum XInfiniApiCategory {
    AFA("afa"),
    SDDC("sddc");

    XInfiniApiCategory(String category) {
        this.category = category;
    }

    private String category;

    @Override
    public String toString() {
        return category;
    }
}
