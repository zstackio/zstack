package org.zstack.header.identity.rbac.condition

class Condition {
    private enum Operator {
        and,
        or
    }

    private class Expression {
        Operator operator
        Closure<Boolean> expression
    }

    private List<Expression> expressions = []

    void and(Closure<Boolean> c) {
        expressions.add(new Expression(operator: Operator.and, expression: c))
    }

    void or(Closure<Boolean> c) {
        expressions.add(new Expression(operator: Operator.or, expression: c))
    }

    boolean eval() {
        boolean ret = true

        for (Expression expr : expressions) {
            if (expr.operator == Operator.and) {
                ret = ret && expr.expression()
                if (!ret) {
                    return false
                }
            }

            if (expr.operator == Operator.or) {
                ret = ret || expr.expression()
                if (ret) {
                    return ret
                }
            }
        }

        return ret
    }
}
