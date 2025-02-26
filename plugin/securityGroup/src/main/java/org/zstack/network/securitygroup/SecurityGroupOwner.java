package org.zstack.network.securitygroup;

public enum SecurityGroupOwner {
    USER("user"),
    IAM2("iam2");

    public final String name;

    SecurityGroupOwner(String owner) {
        name = owner;
    }
}
