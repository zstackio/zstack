package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode
import org.zstack.zql.ast.visitors.result.QueryResult

class QueryVisitor implements ASTVisitor<QueryResult, ASTNode.Query> {
    private String makeConditions(ASTNode.Query node) {
        if (node.conditions?.isEmpty()) {
            return ""
        }

        List<String> conds = node.conditions.collect { (it as ASTNode).accept(new ConditionVisitor()) }
        return conds.join(" ")
    }

    QueryResult visit(ASTNode.Query node) {
        def ret = new QueryResult()
        ret.sql = "SELECT entity${node.target.fields?.isEmpty() ? "" : "." + node.target.fields[0]} FROM ${node.target.entity}"
        return ret
    }
}
