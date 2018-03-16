package org.zstack.test.integration.identity

import org.zstack.header.identity.AccountConstant
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class RBACPermissionCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {}
    }

    void testAccountPermission() {
        def e = senv {
            identities {
                admin {
                    policy {
                        name = "admin-policy"
                        statement {
                            effect = AccountConstant.StatementEffect.Allow
                            action(".*")
                        }
                    }

                    role {
                        name = "admin-role"
                        usePolicy("admin-policy")
                    }
                }

                account {
                    name = "account1"
                    password = "password"
                    useRole("admin-role")
                }

                account {
                    name = "test"
                    password = "password"

                    policy {
                        name = "normal-policy"

                        statement {
                            effect = AccountConstant.StatementEffect.Allow
                            action(".*")
                        }
                    }

                    user {
                        name = "user1"
                        password = "password"

                        usePolicy("normal-policy")
                        useRole("role1")
                    }

                    role {
                        name = "role1"
                        usePolicy("normal-policy")
                    }

                    group {
                        name = "group1"
                        addUser("user1")
                        usePolicy("normal-policy")
                        useRole("role1")
                    }
                }
            }
        }

        e.delete()
    }

    @Override
    void test() {
        env.create {
            testAccountPermission()
        }
    }
}
