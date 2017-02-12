package org.zstack.testlib

import org.zstack.header.identity.AccountConstant
import org.zstack.sdk.AttachBackupStorageToZoneAction
import org.zstack.sdk.LogInByAccountAction
import org.zstack.sdk.SessionInventory
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/12.
 */
class EnvSpec implements Node {
    protected List<ZoneSpec> zones = []
    List<AccountSpec> accounts = []

    SessionInventory session

    Map specsByName = [:]
    Map specsByUuid = [:]

    void zone(String name, String description) {
        zones.add(new ZoneSpec(name, description))
    }

    ZoneSpec zone(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ZoneSpec.class) Closure c)  {
        def zspec = new ZoneSpec()
        c.delegate = zspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        zones.add(zspec)
        addChild(zspec)
        return zspec
    }

    AccountSpec account(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = AccountSpec.class) Closure c) {
        def aspec = new AccountSpec()
        c.delegate = aspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(aspec)
        accounts.add(aspec)
        return aspec
    }

    InstanceOfferingSpec instanceOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = InstanceOfferingSpec.class) Closure c) {
        def spec = new InstanceOfferingSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    BackupStorageSpec sftpBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SftpBackupStorageSpec.class) Closure c) {
        def spec = new SftpBackupStorageSpec()
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = spec
        c()
        addChild(spec)
        return spec
    }

    BackupStorageSpec cephBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = CephBackupStorageSpec.class) Closure c) {
        def spec = new CephBackupStorageSpec()
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = spec
        c()
        addChild(spec)
        return spec
    }

    DiskOfferingSpec diskOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = DiskOfferingSpec.class) Closure c) {
        def spec = new DiskOfferingSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    VmSpec vm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = VmSpec.class) Closure c) {
        def spec = new VmSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    void adminLogin() {
        session = login(AccountConstant.INITIAL_SYSTEM_ADMIN_NAME, AccountConstant.INITIAL_SYSTEM_ADMIN_PASSWORD)
    }

    SessionInventory login(String accountName, String password) {
        LogInByAccountAction a = new LogInByAccountAction()
        a.accountName = accountName
        a.password = password
        def res = a.call()
        assert res.error == null : "Login failure: ${JSONObjectUtil.toJsonString(res.error)}"
        return res.value.inventory
    }

    def specByUuid(String uuid) {
        return specsByUuid[uuid]
    }

    def specByName(String name) {
        return specsByName[name]
    }

    EnvSpec create() {
        adminLogin()
        deploy()

        return this
    }
}
