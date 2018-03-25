package org.zstack.zql.ast

import org.zstack.zql.ast.visitors.ASTVisitor

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

    interface ReturnWithExpr {

    }

    static class QueryTarget extends ASTNode {
         String entity
         List<String> fields
    }

    static class ComplexValue implements Value {
         SubQuery subQuery
    }

    static class PlainValue implements Value {
         String text
         transient Class type
         String ctype
    }

    static class ListValue implements Value {
         List<Value> values
    }

    static class Expr extends ASTNode implements Condition {
         String operator
         List<String> left
         Value right
    }

    static class LogicalOperator extends ASTNode implements Condition {
         String operator
         Condition left
         Condition right
    }

    static class OrderBy extends ASTNode {
         String field
         String direction
    }

    static class Limit extends ASTNode {
         int limit
    }

    static class Offset extends ASTNode {
         int offset
    }

    static class RestrictExpr extends ASTNode {
         String entity
         String field
         String operator
         Value value
    }

    static class RestrictBy extends ASTNode {
        List<RestrictExpr> exprs
    }

    static class ReturnWithIDExpr extends ASTNode implements ReturnWithExpr {
        List<String> names
    }

    static class ReturnWithBlockExpr extends ASTNode implements ReturnWithExpr {
        String content
    }

    static class ReturnWith extends ASTNode {
         List<ReturnWithExpr> exprs
    }

    static class Query extends ASTNode {
         QueryTarget target
         List<Condition> conditions
         RestrictBy restrictBy
         ReturnWith returnWith
         FilterBy filterBy
         OrderBy orderBy
         Limit limit
         Offset offset
    }

    static class SubQueryTarget extends ASTNode {
         String entity
         List<String> fields
    }

    static class SubQuery extends ASTNode {
         SubQueryTarget target
         List<Condition> conditions
    }

    static class FilterByExpr extends ASTNode {
         String filterName
         String content
    }

    static class FilterBy extends ASTNode {
         List<FilterByExpr> exprs
    }
}
