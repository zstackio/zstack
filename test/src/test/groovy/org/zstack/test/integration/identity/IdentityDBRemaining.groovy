package org.zstack.test.integration.identity

import org.zstack.core.db.Q
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.QuotaVO
import org.zstack.header.identity.role.*
import org.zstack.testlib.AllowedDBRemaining

class IdentityDBRemaining extends AllowedDBRemaining {
    @Override
    void remaining() {
        table {
            tableVOClass = AccountResourceRefVO.class
            checker = { List<AccountResourceRefVO> vos ->
                return vos.findAll { it.resourceType != QuotaVO.class.simpleName }
            }
        }

        table {
            tableVOClass = RoleVO.class
            checker = { List<RoleVO> vos ->
                return vos.findAll { it.type != RoleType.Predefined }
            }
        }

        table {
            tableVOClass = SystemRoleVO.class
            noLimitRows = true
        }

        table {
            tableVOClass = RolePolicyStatementVO.class
            checker = { List<RolePolicyStatementVO> vos ->
                List<String> roleUuids = vos.collect { it.roleUuid }

                return Q.New(RoleVO.class).notEq(RoleVO_.type, RoleType.Predefined)
                        .in(RoleVO_.uuid, roleUuids).list()
            }
        }

        table {
            tableVOClass = AccountResourceRefVO.class

            checker = { List<AccountResourceRefVO> vos ->
                List<String> systemRoleUuids = Q.New(SystemRoleVO.class).select(SystemRoleVO_.uuid).listValues()
                return vos.findAll { !systemRoleUuids.contains(it.resourceUuid) }
            }
        }
    }
}
