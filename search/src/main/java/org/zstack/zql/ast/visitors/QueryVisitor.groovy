package org.zstack.zql.ast.visitors

import org.zstack.core.db.SQLBatchWithReturn
import org.zstack.zql.ZQLContext
import org.zstack.zql.ast.ASTNode
import org.zstack.zql.ast.ZQLMetadata
import org.zstack.zql.ast.visitors.result.QueryResult

import javax.persistence.EntityManager
import javax.persistence.Query

class QueryVisitor implements ASTVisitor<QueryResult, ASTNode.Query> {
    def ret = new QueryResult()



    private String makeConditions(ASTNode.Query node) {
        if (node.conditions?.isEmpty()) {
            return ""
        }

        List<String> conds = node.conditions.collect { (it as ASTNode).accept(new ConditionVisitor()) } as List<String>
        return conds.join(" ")
    }

    private class SQLText {
        String sql
        // JPQL doesn't not support limit and offset clause
        String jpql
        Integer limit
        Integer offset
    }

    private SQLText makeSQL(ASTNode.Query node) {
        SQLText st = new SQLText()

        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.target.entity)
        ret.inventoryMetadata = inventory
        ZQLContext.pushQueryTargetInventoryName(inventory.fullInventoryName())

        String fieldName = node.target.fields == null || node.target.fields.isEmpty() ? "" : node.target.fields[0]
        if (fieldName != "") {
            inventory.errorIfNoField(fieldName)
        }

        String entityAlias = inventory.simpleInventoryName()
        String queryTarget = fieldName == "" ? entityAlias : "${inventory.simpleInventoryName()}.${fieldName}"
        String entityVOName = inventory.inventoryAnnotation.mappingVOClass().simpleName

        List<String> sqlClauses = []
        List<String> jpqlClauses = []

        sqlClauses.add("SELECT ${queryTarget} FROM ${entityVOName} ${entityAlias}")
        String condition = makeConditions(node)
        String restrictBy = node.restrictBy?.accept(new RestrictByVisitor())
        if (condition != "" || restrictBy != null) {
            sqlClauses.add("WHERE")
        }

        List<String> conditionClauses = []
        if (condition != "") {
            conditionClauses.add(condition)
        }
        if (restrictBy != null) {
            conditionClauses.add(restrictBy)
        }

        if (!conditionClauses.isEmpty()) {
            sqlClauses.add(conditionClauses.join(" AND "))
        }

        if (node.orderBy != null) {
            sqlClauses.add(node.orderBy.accept(new OrderByVisitor()) as String)
        }

        jpqlClauses.addAll(sqlClauses)

        if (node.limit != null) {
            def v = new LimitVisitor()
            sqlClauses.add(node.limit.accept(v) as String)
            assert v.limit
            st.limit = v.limit
        }

        if (node.offset != null) {
            def v = new OffsetVisitor()
            sqlClauses.add(node.offset.accept(v) as String)
            assert v.offset
            st.offset = v.offset
        }

        ZQLContext.popQueryTargetInventoryName()


        st.sql = sqlClauses.join(" ")
        st.jpql = jpqlClauses.join(" ")
        return st
    }

    QueryResult visit(ASTNode.Query node) {
        SQLText st = makeSQL(node)
        ret.sql = st.sql
        ret.createJPAQuery = { EntityManager emgr ->
            Query q = emgr.createQuery(st.jpql)
            if (st.limit) {
                q.setMaxResults(st.limit)
            }
            if (st.offset) {
                q.setFirstResult(st.offset)
            }

            return q
        }

        return ret
    }
}
