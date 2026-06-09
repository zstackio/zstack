package org.zstack.testlib.check;

import java.util.Arrays;
import java.util.List;

public class MetadataImpactCheckerStub {
    public static final List<String> NONE_IMPACT_APIS = Arrays.asList(
        // core
        "org.zstack.core.captcha.APIRefreshCaptchaMsg"
        ,"org.zstack.core.config.APIGetGlobalConfigOptionsMsg"
        ,"org.zstack.core.config.APIQueryGlobalConfigMsg"
        ,"org.zstack.core.config.APIResetGlobalConfigMsg"
        ,"org.zstack.core.config.APIUpdateGlobalConfigMsg"
        ,"org.zstack.core.debug.APICleanQueueMsg"
        ,"org.zstack.core.debug.APIDebugSignalMsg"
        ,"org.zstack.core.debug.APIGetDebugSignalMsg"
        ,"org.zstack.core.errorcode.APICheckElaborationContentMsg"
        ,"org.zstack.core.errorcode.APIGetElaborationCategoriesMsg"
        ,"org.zstack.core.errorcode.APIGetElaborationsMsg"
        ,"org.zstack.core.errorcode.APIReloadElaborationMsg"
        ,"org.zstack.core.eventlog.APIQueryEventLogMsg"
        ,"org.zstack.core.gc.APIDeleteGCJobMsg"
        ,"org.zstack.core.gc.APIQueryGCJobMsg"
        ,"org.zstack.core.gc.APITriggerGCJobMsg"

        // directory
        ,"org.zstack.directory.APIAddResourcesToDirectoryMsg"
        ,"org.zstack.directory.APICreateDirectoryMsg"
        ,"org.zstack.directory.APIDeleteDirectoryMsg"
        ,"org.zstack.directory.APIMoveDirectoryMsg"
        ,"org.zstack.directory.APIMoveResourcesToDirectoryMsg"
        ,"org.zstack.directory.APIQueryDirectoryMsg"
        ,"org.zstack.directory.APIRemoveResourcesFromDirectoryMsg"
        ,"org.zstack.directory.APIUpdateDirectoryMsg"

        // header
        ,"org.zstack.header.APIIsOpensourceVersionMsg"
        ,"org.zstack.header.allocator.APIGetCpuMemoryCapacityMsg"
        ,"org.zstack.header.allocator.APIGetHostAllocatorStrategiesMsg"
        ,"org.zstack.header.apimediator.APIIsReadyToGoMsg"

        // acl
        ,"org.zstack.header.acl.APIAddAccessControlListEntryMsg"
        ,"org.zstack.header.acl.APIAddAccessControlListRedirectRuleMsg"
        ,"org.zstack.header.acl.APIChangeAccessControlListRedirectRuleMsg"
        ,"org.zstack.header.acl.APICreateAccessControlListMsg"
        ,"org.zstack.header.acl.APIDeleteAccessControlListMsg"
        ,"org.zstack.header.acl.APIQueryAccessControlListMsg"
        ,"org.zstack.header.acl.APIRemoveAccessControlListEntryMsg"
    );
}
