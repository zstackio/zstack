package org.zstack.zql.ast

interface ASTVisitor<T, K extends ASTNode> {
    T visit(K node)

    static class Result {
    }

    static class StringResult extends Result {
        String value
    }

    static class ReturnWithResult extends Result {
        List<String> values
    }

    static class FilterByResult extends Result {
        String filterName
        String content
    }

    static class QueryResult {
        String sql
        ReturnWithResult returnWith
        List<FilterByResult> filterBy
    }

    static class ConditionVisitor implements ASTVisitor<String, ASTNode> {
        @Override
        String visit(ASTNode node) {
            assert node instanceof ASTNode.Condition

            return null
        }
    }

    static class QueryVisitor implements ASTVisitor<QueryResult, ASTNode.Query> {
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
}
