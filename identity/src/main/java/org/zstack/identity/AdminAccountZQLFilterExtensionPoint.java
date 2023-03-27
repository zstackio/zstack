package org.zstack.identity;

import org.zstack.header.zql.ZQLExtensionContext;
import org.zstack.zql.ast.ZQLMetadata;

/**
 * @author Xingwei Yu
 * @date 2023/9/22 9:59
 */
public interface AdminAccountZQLFilterExtensionPoint {
    void filterSanyuanSystemSystemPredefinedVirtualUuid(ZQLExtensionContext context);
    void filterPhysicalResourceInventoriesMetadataByName(ZQLMetadata.InventoryMetadata src);
}
