package org.zstack.header.zql;

public interface RestrictByExprExtensionPoint {
    class RestrictByExpr {
        public String entity;
        public String field;
    }

    class SkipThisRestrictExprException extends RuntimeException {
    }

    String restrictByExpr(ZQLExtensionContext context, RestrictByExpr expr);
}
