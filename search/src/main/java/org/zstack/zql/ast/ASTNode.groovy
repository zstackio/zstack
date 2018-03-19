package org.zstack.zql.ast

class ASTNode {
    private static final Map<Class, List<String>> childrenNames = [:]

    List<ASTNode> getChildren() {
        List<String> childNames
        synchronized (childrenNames) {
            childNames = childrenNames.get(getClass(), metaClass.getProperties().findAll { ASTNode.isAssignableFrom(it.type) }.collect { it.name })
        }

        return childNames.collect { owner[it] as ASTNode }
    }

    Object accept(ASTVisitor visitor) {
        return visitor.visit(this)
    }

    interface Value {
    }

    interface Condition {
    }

    static class QueryTarget extends ASTNode {
        public String entity
        public List<String> fields
    }

    static class ComplexValue implements Value {
        public SubQuery subQuery
    }

    static class PlainValue implements Value {
        public String text
        public transient Class type
        public String ctype
    }

    static class ListValue implements Value {
        public List<Value> values
    }

    static class Expr extends ASTNode implements Condition {
        public String operator
        public List<String> left
        public Value right
    }

    static class LogicalOperator extends ASTNode implements Condition {
        public String operator
        public Condition left
        public Condition right
    }

    static class OrderBy extends ASTNode {
        public String field
        public String direction
    }

    static class Limit extends ASTNode {
        public int limit
    }

    static class Offset extends ASTNode {
        public int offset
    }

    static class RestrictExpr extends ASTNode {
        public String entity
        public String field
        public String operator
        public Value value
    }

    static class RestrictBy extends ASTNode {
        public List<RestrictExpr> exprs
    }

    static class ReturnWithExpr extends ASTNode {
        public List<String> names
    }

    static class ReturnWith extends ASTNode {
        public List<ReturnWithExpr> exprs
    }

    static class Query extends ASTNode {
        public QueryTarget target
        public List<Condition> conditions
        public RestrictBy restrictBy
        public ReturnWith returnWith
        public FilterBy filterBy
        public OrderBy orderBy
        public Limit limit
        public Offset offset
    }

    static class SubQueryTarget extends ASTNode {
        public String entity
        public List<String> fields
    }

    static class SubQuery extends ASTNode {
        public SubQueryTarget target
        public List<Condition> conditions
    }

    static class FilterByExpr extends ASTNode {
        public String filterName
        public String content
    }

    static class FilterBy extends ASTNode {
        public List<FilterByExpr> exprs
    }
}
