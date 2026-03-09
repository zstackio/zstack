package org.zstack.zql.ast.visitors;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.search.SearchFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ast.visitors.result.SearchResult;

/**
 * Hibernate Search 5 removed — incompatible with Jakarta namespace.
 * Full-text ZQL search is disabled until upgrade to Hibernate Search 7.x.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SearchVisitor implements ASTVisitor<SearchResult, ASTNode.Search> {
    private static final CLogger logger = Utils.getLogger(SearchVisitor.class);

    @Autowired
    SearchFacade sf;

    @Override
    public SearchResult visit(ASTNode.Search node) {
        throw new CloudRuntimeException("Full-text search is disabled (Hibernate Search pending upgrade to 7.x)");
    }
}
