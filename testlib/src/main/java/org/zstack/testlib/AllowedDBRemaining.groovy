package org.zstack.testlib

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

class AllowedDBRemaining {
    CLogger logger = Utils.getLogger(AllowedDBRemaining.class)
    List<Table> tables = []

    private boolean resolved
    private Closure resolveFunction

    void resolve() {
        if (!resolved) {
            resolveFunction()
            resolved = true
        }
    }

    static AllowedDBRemaining New(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = AllowedDBRemaining.class) Closure c) {
        def a = new AllowedDBRemaining()
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = a
        a.resolveFunction = c
        return a
    }

    class DBUtil {
        def getRowVO(Class tableClz, String rowName) {
            Row row = null
            for (Table t : tables) {
                row = t.rows.find { it.name == rowName }
                if (row != null) {
                    break
                }
            }

            assert row != null : "row[name:${rowName}] not defined in table[${tableClz.simpleName}]"


            def conds = []
            row.IDColumns.each { k, v->
                if (v instanceof String) {
                    conds.add("${k}='${v}'")
                } else {
                    conds.add("${k}=${v}")
                }
            }

            assert conds : "row[name:${rowName}] defined in table[${tableClz.simpleName}] has no IDColumns defined"
            String sql = "SELECT a from ${tableClz.simpleName} a WHERE ${conds.join(" AND ")}"
            return SQL.New(sql, tableClz).find()
        }
    }

    class Row extends DBUtil {
        String name
        Map<String, Object> IDColumns = [:]

        void column(String columnName, Object value) {
            IDColumns[columnName] = value
        }
    }

    class Table extends DBUtil {
        Class tableVOClass
        boolean noLimitRows

        List<Row> rows = []

        void row(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Row.class) Closure c) {
            Row r = new Row()
            c.delegate = r
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()
            rows.add(r)
        }

        List check(List vos) {
            if (noLimitRows) {
                return []
            }

            List remain = []
            remain.addAll(vos)

            vos.each { vo ->
                assert rows.find {
                    for (e in it.IDColumns) {
                        if (vo[e.key] != e.value) {
                            return false
                        }
                    }

                    return true
                } != null : "unexpected row found in table[${tableVOClass.simpleName}], it must be cleaned up:\n ${vo.getProperties()}"

                remain.remove(vo)
            }

            return remain
        }
    }

    void table(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Table.class) Closure c) {
        def t = new Table()
        c.delegate = t
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        tables.add(t)
    }

    List check(String tblName, List vos) {
        resolve()

        Table t = tables.find { it.tableVOClass.simpleName == tblName }
        if (t != null) {
            return t.check(vos)
        }

        return vos
    }
}
