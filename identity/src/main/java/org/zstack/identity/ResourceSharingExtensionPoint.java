package org.zstack.identity;

import org.zstack.identity.header.ShareResourceContext;

public interface ResourceSharingExtensionPoint {
    void beforeSharingResource(ShareResourceContext context);
}
