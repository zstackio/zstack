package org.zstack.header.core.external.plugin;

import org.zstack.header.identity.rbac.RBACDescription;

public class RBACInfo implements RBACDescription {

    @Override
    public void permissions() {
        permissionBuilder()
                .name("external-plugin")
                .adminOnlyAPIs(
                        APIQueryPluginDriversMsg.class,
                        APIRefreshPluginDriversMsg.class,
                        APIDeletePluginDriversMsg.class
                ).build();

        permissionBuilder()
                .name("feature-plugin")
                .normalAPIs(APIGetFeaturePluginsMsg.class.getName())
                .adminOnlyAPIs(
                        APIGetFeaturePluginPackagesMsg.class,
                        APIAddFeaturePluginPackageMsg.class,
                        APIInstallFeaturePluginMsg.class,
                        APIUninstallFeaturePluginMsg.class
                ).build();
    }

    @Override
    public void contributeToRoles() {
        roleContributorBuilder()
                .roleName("other")
                .actionsByPermissionName("external-plugin")
                .build();
    }

    @Override
    public void roles() {

    }

    @Override
    public void globalReadableResources() {

    }
}
