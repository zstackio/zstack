package org.zstack.zql.ast.visitors;

import org.zstack.core.Platform;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;

import java.util.Optional;

import static org.zstack.zql.ast.visitors.constants.MySqlKeyword.keywordMap;

/**
 * Created by MaJin on 2019/6/3.
 */
public class FunctionVisitor implements ASTVisitor<String, ASTNode.Function> {
    @Override
    public String visit(ASTNode.Function function) {
        if (function == null) {
            throw new OperationFailureException(Platform.operr("function cannot be null"));
        }

        Optional<String> result = keywordMap.keySet().stream()
                .filter(key -> key.equalsIgnoreCase(function.getFunctionName()))
                .findFirst();
        if (result.isPresent()) {
            return keywordMap.get(result.get());
        }
        return function.getFunctionName() + "(%s)";
    }
}
