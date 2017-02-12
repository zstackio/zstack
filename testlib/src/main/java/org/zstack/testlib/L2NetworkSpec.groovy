package org.zstack.testlib

import org.zstack.sdk.L2NetworkInventory

/**
 * Created by xing5 on 2017/2/15.
 */
abstract class L2NetworkSpec implements Spec {
    String name
    String description
    String physicalInterface

    L2NetworkInventory inventory

    L3NetworkSpec l3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = L3NetworkSpec.class) Closure c) {
        def l3 = new L3NetworkSpec()
        c.delegate = l3
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(l3)

        return l3
    }
}
