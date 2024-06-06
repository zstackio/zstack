package org.zstack.identity.imports.entity;

public enum SyncUpdateAccountStateStrategy {
    NoAction,
    KeepSameWithSource,
    EnableNeedChecking,
    ;

    public static SyncUpdateAccountStateStrategy from(SyncCreatedAccountStrategy createStrategy) {
        switch (createStrategy) {
        case NoAction:
            return NoAction;
        case CreateAccount: default:
            return KeepSameWithSource;
        }
    }
}
