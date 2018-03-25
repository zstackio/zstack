package org.zstack.zql.ast.visitors

import org.zstack.zql.ast.ASTNode

interface ASTVisitor<T, K extends ASTNode> {
    T visit(K node)
}
