package org.zstack.test.integration.other.apicost.analyzer

import org.zstack.core.apicost.analyzer.ApiSpendTimePredictor
import org.zstack.core.apicost.analyzer.zstack.Predictor
import org.zstack.core.apicost.analyzer.zstack.PredictResult
import org.zstack.testlib.SubCase
import org.zstack.utils.ShellUtils
import org.zstack.utils.Utils
import java.lang.System

/**
 * Created by huaxin on 2021/7/22.
 */
class PredictorCase extends SubCase {

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        String sql2CreateStepGraph = "SET NAMES utf8mb4;\n" +
                "SET FOREIGN_KEY_CHECKS = 0;\n" +
                "DROP TABLE IF EXISTS `MsgLogVO`;\n" +
                "CREATE TABLE `MsgLogVO`  (\n" +
                "  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,\n" +
                "  `uuid` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,\n" +
                "  `msgId` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,\n" +
                "  `msgName` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,\n" +
                "  `taskName` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,\n" +
                "  `apiId` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,\n" +
                "  `startTime` bigint(20) NULL DEFAULT NULL,\n" +
                "  `replyTime` bigint(20) NULL DEFAULT NULL,\n" +
                "  `wait` decimal(7, 2) NULL DEFAULT NULL,\n" +
                "  `status` int(1) NOT NULL,\n" +
                "  PRIMARY KEY (`id`) USING BTREE,\n" +
                "  UNIQUE INDEX `id`(`id`) USING BTREE\n" +
                ") ENGINE = InnoDB AUTO_INCREMENT = 77 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;\n" +
                "INSERT INTO `MsgLogVO` VALUES (1, '97e8ede291574be1ad3284fae2730c9f', '28d4bd5ed6a2406991698185816644e0', 'org.zstack.header.storage.backup.ConnectBackupStorageMsg', 'org.zstack.storage.backup.sftp.APIAddSftpBackupStorageMsg', 'cdca968b6929442cbdba232e01c849c8', 1627807481309, 1627807481760, 0.45, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (2, '1cc9fbcb51d647d5a2610e4e44050628', 'eeca3e6cb2e7483fa813c9cc708be751', 'org.zstack.header.storage.backup.DownloadImageMsg', 'org.zstack.header.image.APIAddImageMsg', '499549ed768e4a85b48cbed4c41308e2', 1627807481951, 1627807482128, 0.18, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (3, '6fbf0212788b45b5b215e3f259a1fc0f', '935440d7aa83485dbb717ae7aba4d112', 'org.zstack.header.storage.backup.DownloadImageMsg', 'org.zstack.header.image.APIAddImageMsg', 'ade074daf26d40c4a7bdb08dec2902a6', 1627807482437, 1627807482463, 0.03, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (4, '9501b90aeb2d4ab2938e3e83d22be4d6', 'c33d0cfd66a542498f114599bec1f7b2', 'org.zstack.header.storage.primary.ConnectPrimaryStorageMsg', 'org.zstack.storage.primary.local.APIAddLocalPrimaryStorageMsg', '93c286c6a04e4f968e772f9c68c971f4', 1627807482836, 1627807483072, 0.24, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (5, '8947d8406f77450e983e2fd56c17a0d3', 'eac9b3ca30eb418eb31899dfdc775aae', 'org.zstack.storage.primary.local.InitPrimaryStorageOnHostConnectedMsg', 'org.zstack.kvm.APIAddKVMHostMsg', 'ca5850ada93946099435a85a49bcd1f6', 1627807484080, 1627807484410, 0.33, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (6, 'e9f7c6c93c1b42b28fd142fdd5c42566', 'a528e06fc2c64b68be5d6c24a5fac7b3', 'org.zstack.header.cluster.ReportHostCapacityMessage', 'org.zstack.kvm.APIAddKVMHostMsg', 'ca5850ada93946099435a85a49bcd1f6', 1627807484514, 1627807484529, 0.02, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (7, '0dc559ea129d4a0db2eceeaedc24b157', 'fb2e89a13055458fad0990bea0d83da5', 'org.zstack.header.host.CheckHostCapacityMsg', 'org.zstack.kvm.APIAddKVMHostMsg', 'ca5850ada93946099435a85a49bcd1f6', 1627807484422, 1627807484533, 0.11, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (8, 'f7e43425345f44e3922a2d7667802b85', 'b29cabcdf6aa4d82819fc0bc3d7549f4', 'org.zstack.header.network.l2.BatchCheckNetworkPhysicalInterfaceMsg', 'org.zstack.kvm.APIAddKVMHostMsg', 'ca5850ada93946099435a85a49bcd1f6', 1627807484677, 1627807484707, 0.03, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (9, 'c5699d1f75fa4afc80cc0d894b4511f8', 'e3b9750d3c1941408e1a1d0e607a893d', 'org.zstack.header.host.ConnectHostMsg', 'org.zstack.kvm.APIAddKVMHostMsg', 'ca5850ada93946099435a85a49bcd1f6', 1627807483739, 1627807484863, 1.12, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (10, '84910f58112140c2b16b631194d7ac09', '22c80926d23547eb90c09908acbe7295', 'org.zstack.network.l3.AttachNetworkServiceToL3Msg', 'org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkMsg', 'a3231863fc3d4a8088ff0ef8963ec67c', 1627807485307, 1627807485319, 0.01, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (11, 'ccba9948db7940f296835453af6a7225', 'e95371ed2ef34b5ea721e9181d062247', 'org.zstack.network.l3.AttachNetworkServiceToL3Msg', 'org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkMsg', '457e69c31c614547a919a0f45b84d564', 1627807485422, 1627807485429, 0.01, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (12, '9064e42b73e0412893e8812fab279434', '227bc99486b7411397b39155fdb9e955', 'org.zstack.header.allocator.DesignatedAllocateHostMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807486493, 1627807486761, 0.27, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (13, 'dd477c98a32c48419268f75dd52f2fbc', 'f6d65e7b8efc4c18a62a7eb53a5327c7', 'org.zstack.header.storage.primary.AllocatePrimaryStorageMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807486791, 1627807487164, 0.37, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (14, '9a64566e29a945bab0e6392775fa41e9', '8a5517018d0942d3af20e4863446ff2a', 'org.zstack.header.volume.CreateVolumeMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807487177, 1627807487238, 0.06, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (15, 'ef259be58d7d459abacbddc55884e6c3', '2bad0cf35c664f14ae7bb220c86ae305', 'org.zstack.header.network.l3.AllocateIpMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807487271, 1627807487293, 0.02, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (16, '2102a960cacb41d4a66c67e52866e781', '2bbe6e171685400da0ab13bf657d2f40', 'org.zstack.header.storage.primary.AllocatePrimaryStorageMsg', 'create a volume[%s] on the local storage', 'dddb16981c944ddf9ef6798cfc34531c', 1627807487526, 1627807487543, 0.02, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (17, 'bf1085e0d88f450abbba4bbd85f6ea99', 'c0b5cd1b06ce457f995700b21e57f96a', 'org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg', 'create a volume[%s] on the local storage', 'dddb16981c944ddf9ef6798cfc34531c', 1627807487552, 1627807487564, 0.01, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (18, '355e2ea4fca9416bb8faef460848a34b', 'c266a1e053fb4fc4afea37d29ddc55ee', 'org.zstack.header.storage.primary.DownloadVolumeTemplateToPrimaryStorageMsg', 'create a volume[%s] on the local storage', 'dddb16981c944ddf9ef6798cfc34531c', 1627807487487, 1627807487618, 0.13, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (19, '8fb9edf72c7f4a649987dbbc4b8140d4', 'f2b841fc24414a5aa3b043ba58ae6f5b', 'org.zstack.header.storage.primary.InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807487437, 1627807487690, 0.25, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (20, 'f4e965cfabe944039025c15ef32dda2a', 'c3794cc1eecf4175ad38ef8e5d6b38c0', 'org.zstack.header.volume.InstantiateRootVolumeMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807487338, 1627807487717, 0.38, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (21, '54ffed8ca3b24c58a1db511e2d7c2d1b', '30c3f56555a144d6b96e9deb514f6235', 'org.zstack.header.allocator.DesignatedAllocateHostMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488070, 1627807488120, 0.05, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (22, '84bdc8602d9f4633858ebf8410320ef2', '0cb7ff2b377f4a20a9c929567a7f4252', 'org.zstack.header.storage.primary.AllocatePrimaryStorageMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488132, 1627807488270, 0.14, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (23, '858c2de12c2a435cb0dd337f8d09b757', '604f3b840df94cb0bfd6d82d1724bfaf', 'org.zstack.header.volume.CreateVolumeMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488277, 1627807488335, 0.06, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (24, '8380dba945b846eca144fa52ee74f0fd', 'ea8cb3a702f546f08ad8dea0e2c76317', 'org.zstack.header.network.l3.AllocateIpMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488347, 1627807488361, 0.01, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (25, 'd8e7ae60520d4c88b0edfb2dea4b4bfa', '94bce96f1c94429fad45ad43fc88653c', 'org.zstack.header.network.l3.AllocateIpMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488370, 1627807488382, 0.01, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (26, '3a100e98de74403492c4238f0a80237d', 'dafc208bfb7a4b5e9c91f7f042bb1f42', 'org.zstack.header.storage.primary.AllocatePrimaryStorageMsg', 'create a volume[%s] on the local storage', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488434, 1627807488449, 0.02, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (27, 'c7c7c60ec3134029b82b0da1cbe93252', '0701bd2037a442f6b816ed502a709aa4', 'org.zstack.storage.backup.sftp.GetSftpBackupStorageDownloadCredentialMsg', 'create a volume[%s] on the local storage', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488453, 1627807488457, 0.00, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (28, '9eeb2b76335749919558ed7fa5056e60', '4286e4aa755f4b99ab6ddaa42d091a79', 'org.zstack.header.storage.primary.DownloadVolumeTemplateToPrimaryStorageMsg', 'create a volume[%s] on the local storage', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488421, 1627807488506, 0.09, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (29, '0a1c78d884cc443780c07ede8ce61766', 'e28236e576ff419ba997f59fbbce51a3', 'org.zstack.header.storage.primary.InstantiateRootVolumeFromTemplateOnPrimaryStorageMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488408, 1627807488550, 0.14, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (30, 'e9869c9352e745138a00b8130db6546c', '561125fe8b774b03bd30207c4e65fbdc', 'org.zstack.header.volume.InstantiateRootVolumeMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488399, 1627807488572, 0.17, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (31, 'ae4815b4c2534c5a863f7675e105209a', 'f40d749322e64f69b558307c529f1eb5', 'org.zstack.header.vm.CreateVmOnHypervisorMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488587, 1627807488767, 0.18, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (32, 'cbe772eda8174a1cbf8372b4d05c9877', '2fd9fe04c13f466c934e2e6aa1598601', 'org.zstack.appliancevm.ApplianceVmRefreshFirewallMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807488886, 1627807488966, 0.08, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (33, '3d41b1c0b3d84444a680052889e687e4', '23be484682ab430b9e29df5c466fabb6', 'org.zstack.appliancevm.ApplianceVmRefreshFirewallMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807489000, 1627807489032, 0.03, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (34, 'c8b71dc15cfa41f5a1a50cee9ff6fd3e', '6021a6d9c00746b59d8805b327c8718e', 'org.zstack.network.service.vip.CreateVipMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807489119, 1627807489188, 0.07, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (35, '3987732599d64320b882dd7634b24d68', 'd508b884db2d499bae90f8394173c7b8', 'org.zstack.network.service.vip.AcquireVipMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807489246, 1627807489309, 0.06, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (36, '22998bb8b16b417c8fcdb4910138c723', '883d45dc5437424e88f6158943a188b5', 'org.zstack.appliancevm.StartNewCreatedApplianceVmMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807487937, 1627807489399, 1.46, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (37, 'a6565567b2fd49bb81ec161583854bcd', '2beb255f2a1f46388d9fbff0830ebd50', 'org.zstack.network.service.virtualrouter.CreateVirtualRouterVmMsg', 'create a virtual router vm', 'dddb16981c944ddf9ef6798cfc34531c', 1627807487822, 1627807489422, 1.60, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (38, '0fac7b4ce1194777b80911c88761427b', '44b296033887417d9f02f0d974e97da3', 'org.zstack.header.vm.CreateVmOnHypervisorMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807489516, 1627807489627, 0.11, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (39, '38c414313530402e9e97e7174e517704', '2800afa5ff4845a7bd71286a79c8e74d', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807489634, 1627807489653, 0.02, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (40, 'e8bf5be528c2418faa99e274f498ecfd', '825f6bb71c58409784c4cb3a393591d8', 'org.zstack.header.vm.InstantiateNewCreatedVmInstanceMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807486101, 1627807489702, 3.60, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (41, '23ee1fc5c1344138a52f49e13a4936e9', 'ee18b9e4252240869515271494695a8a', 'org.zstack.header.vm.StopVmOnHypervisorMsg', 'org.zstack.header.vm.APIStopVmInstanceMsg', '0da80d524dfa4cbc847068e635c83efb', 1627807490687, 1627807490731, 0.04, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (42, 'caa9a942366f453d9aa1e45d81a48fed', 'ca419fdfa8814c2da00c1b9da1e77f5c', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIStopVmInstanceMsg', '0da80d524dfa4cbc847068e635c83efb', 1627807490918, 1627807490936, 0.02, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (43, '439fc27850ed431898d6bd4ed3640a24', 'aec7d4602d6847129164bdeebfe1ed21', 'org.zstack.header.allocator.DesignatedAllocateHostMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'e249192a29b74bc192c49f067f18db6a', 1627807491110, 1627807491163, 0.05, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (44, '90703d0f08224c5ba887ff94d7ddb3b0', '18d3a72710d94fca9d641bc977682680', 'org.zstack.header.vm.StartVmOnHypervisorMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'e249192a29b74bc192c49f067f18db6a', 1627807491238, 1627807491304, 0.07, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (45, 'cbad3fa470b04c9d939ae06c1f1a250c', '7e00e7aaaec844db96fc22fab2f096ca', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'e249192a29b74bc192c49f067f18db6a', 1627807491318, 1627807491321, 0.00, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (46, 'ee91bbff56a3416892d1f83e631edd6e', 'e9807ac80ba4403581818e53a46fedfb', 'org.zstack.header.vm.StopVmOnHypervisorMsg', 'org.zstack.header.vm.APIRebootVmInstanceMsg', '8d36fbdf7e0f4067b80fe912e9fbd639', 1627807491554, 1627807491570, 0.02, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (47, '1846a1684674430cada01b5cacb24fb7', 'cd8e913bf2c740c8a9037cbc17bca4f3', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIRebootVmInstanceMsg', '8d36fbdf7e0f4067b80fe912e9fbd639', 1627807491618, 1627807491622, 0.00, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (48, '45c264685e034055970a589c9f6d605b', '0fb03e9b9a73469291ded3387c839741', 'org.zstack.header.vm.StartVmOnHypervisorMsg', 'org.zstack.header.vm.APIRebootVmInstanceMsg', '8d36fbdf7e0f4067b80fe912e9fbd639', 1627807491703, 1627807491795, 0.09, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (49, '4918a426d9dd449f9523c0eb4016735e', '14f5c43fc81645eeb7d4965257a3e70a', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIRebootVmInstanceMsg', '8d36fbdf7e0f4067b80fe912e9fbd639', 1627807491800, 1627807491803, 0.00, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (50, 'd195310005bf48a3aa85a52436a21abe', '9c4102bf0c1d41a1b1e61adffca06388', 'org.zstack.header.vm.RebootVmInstanceMsg', 'org.zstack.header.vm.APIRebootVmInstanceMsg', '8d36fbdf7e0f4067b80fe912e9fbd639', 1627807491478, 1627807491837, 0.36, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (51, '4bf6e2126da04c0d82a65aabacf26873', '25c4965557934a4987bb0c314a79285d', 'org.zstack.header.vm.DestroyVmOnHypervisorMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', 'dffd072cdf684411936fb868da6827db', 1627807492000, 1627807492034, 0.03, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (52, 'dae6e57a57dd43f9bfb44479c94b05aa', 'aea889825cd44e02aa36a2e5eba1d8f0', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', 'dffd072cdf684411936fb868da6827db', 1627807492088, 1627807492092, 0.00, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (53, '7a0b3f163d56400aadba530791549617', '5211a1868f2b40bcae87d388a9416266', 'org.zstack.header.volume.VolumeDeletionMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', 'dffd072cdf684411936fb868da6827db', 1627807492145, 1627807492184, 0.04, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (54, '0af20a37c6c142158bfdf8135f2ed6a0', 'e8d26515b37a4162a6ca5cdf76f7897f', 'org.zstack.header.vm.VmInstanceDeletionMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', 'dffd072cdf684411936fb868da6827db', 1627807491952, 1627807492331, 0.38, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (55, '42c5e73f257e41aa8c8e221f8d7cdf76', '8717f4627bef496496d8aa1bdead7ac3', 'org.zstack.header.volume.RecoverVolumeMsg', 'org.zstack.header.vm.APIRecoverVmInstanceMsg', 'f35f076e38d04d07bce9f29f0e9d58bc', 1627807492435, 1627807492463, 0.03, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (56, '17446ad54e3447359fc81e1b8c4473b9', '74ee8a72c5f04264b72e00e79c01e7b4', 'org.zstack.header.allocator.DesignatedAllocateHostMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'ccea688526194ca7a2623eb4e085aecf', 1627807492573, 1627807492610, 0.04, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (57, 'b54b961db9954b1f94bbd92b1f683be1', '8876ce05a14945d9a7437de4160c8719', 'org.zstack.header.network.l3.AllocateIpMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'ccea688526194ca7a2623eb4e085aecf', 1627807492619, 1627807492635, 0.02, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (58, '271c6727991a4831925c528d79a70575', '421dc6fe8064409a80ae745cf847c8ab', 'org.zstack.header.vm.StartVmOnHypervisorMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'ccea688526194ca7a2623eb4e085aecf', 1627807492705, 1627807492788, 0.08, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (59, 'f117dc3b23604193a871ed4e810ea253', 'a4708e31e2194acdbfeef69c88a4983c', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'ccea688526194ca7a2623eb4e085aecf', 1627807492795, 1627807492798, 0.00, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (60, 'a5f105464f034a1ba8d8bf85764b1636', '1015f031be0c4d6fbf186c6f63410f5b', 'org.zstack.header.vm.DestroyVmOnHypervisorMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', '71dc2654a14049a99a7d8bd3db4e758a', 1627807492933, 1627807492960, 0.03, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (61, '678bf7123ecd497fb66f3e7a56962e39', 'ff1ceb8bc64b47ad96355f6aa8955787', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', '71dc2654a14049a99a7d8bd3db4e758a', 1627807493008, 1627807493013, 0.01, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (62, '5d54bf7d97704fc09edcc733d211d210', '8cd8f4cf769543a495011f15c26240ad', 'org.zstack.header.volume.VolumeDeletionMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', '71dc2654a14049a99a7d8bd3db4e758a', 1627807493039, 1627807493053, 0.01, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (63, 'f7502582398c4b918ea54df8beb13c52', 'efb223002ceb47b9baed3aa7ae1357e5', 'org.zstack.header.vm.VmInstanceDeletionMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', '71dc2654a14049a99a7d8bd3db4e758a', 1627807492906, 1627807493072, 0.17, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (64, '5176fcc1cccc439fa57b6446d0a18b96', '19f38138f25d4a9fb5dbc2285468f247', 'org.zstack.header.storage.primary.DeleteVolumeOnPrimaryStorageMsg', 'org.zstack.header.vm.APIExpungeVmInstanceMsg', 'fb1e61de7a9649d29fbefc35c3b44f98', 1627807493205, 1627807493287, 0.08, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (65, '9a97dd50be4941b391d716881a699acf', '7a5a2506411e4b1c9a461ecc3e107f2b', 'org.zstack.header.volume.ExpungeVolumeMsg', 'org.zstack.header.vm.APIExpungeVmInstanceMsg', 'fb1e61de7a9649d29fbefc35c3b44f98', 1627807493174, 1627807493321, 0.15, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (66, '201cc0a7784d4088a5eaa6748da66206', '9fa0edfa895c4d31bc5c82725fa9886b', 'org.zstack.header.vm.VmInstanceDeletionMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', 'e9d4887577b84cf4bc5635fba7079e37', 1627807493678, 1627807493719, 0.04, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (67, '28c6f50b683b4ae0aa7dca65699361ec', 'f2b3e5d2ff6f44d5bce720c6bf333907', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.zone.APIChangeZoneStateMsg', 'af2a6fb076f44504a94a061a50c97a96', 1627807493841, 1627807493862, 0.02, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (68, '3e540b13f7364780bf6cf111cafcf71d', 'b0daf2708eff4268be4379594d01a344', 'org.zstack.header.cluster.ChangeClusterStateMsg', 'org.zstack.header.zone.APIChangeZoneStateMsg', 'af2a6fb076f44504a94a061a50c97a96', 1627807493809, 1627807493875, 0.07, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (69, 'cfbe2af2567b46b5a5f9c308e1541a7d', '5a508422f42445c19e5a05e61eaaabb5', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.zone.APIChangeZoneStateMsg', '51135c3785de4d0abdef58ab8dd97c8f', 1627807494009, 1627807494016, 0.01, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (70, '09391fa7154a492983c04c76b79c46de', '089ed87730aa4486964d1ccf95ec763e', 'org.zstack.header.cluster.ChangeClusterStateMsg', 'org.zstack.header.zone.APIChangeZoneStateMsg', '51135c3785de4d0abdef58ab8dd97c8f', 1627807494005, 1627807494022, 0.02, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (71, '7ca511d0cc2e40dfb2c0cdcc7edfc6ec', '786720a85fcf4ea78fb2ca32188d4083', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.cluster.APIChangeClusterStateMsg', '95984512ef8846139cb9043a3c5cadb6', 1627807494086, 1627807494101, 0.02, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (72, 'ebf1612fe78247f28ebce80b03bd85d5', 'b4f69acc4a464999b5f4ad124f92817e', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.cluster.APIChangeClusterStateMsg', 'b7b6662afb3242c0aa6645f46b9508e2', 1627807494194, 1627807494202, 0.01, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (73, '4866309491504f7fa307831cd6e713f1', 'f3d69a75843945b9a7873387222ca31d', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.host.APIChangeHostStateMsg', '0fd332cbd9f2415d85260a4776f13548', 1627807494267, 1627807494279, 0.01, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (74, '31249a9730384c19ad1bcb57a6396bf5', '17de2c5677a54f6b8ad2ac0741dbca47', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.host.APIChangeHostStateMsg', 'cb8848110f1944e7a9b4151193e2c283', 1627807494379, 1627807494389, 0.01, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (75, '11040e5b33fb4fa7b081c4a1f5835edd', 'd240e88311594ca689bd500120cafa2a', 'org.zstack.header.storage.backup.DownloadImageMsg', 'org.zstack.header.image.APIAddImageMsg', 'fd776540ccfd4eb1a274d5be89561920', 1627807494844, 1627807494870, 0.03, 0);\n" +
                "INSERT INTO `MsgLogVO` VALUES (76, '9c161698cc214604982b6e3cb2ed9526', 'f28eca787da64d95a39edb0bdacbb933', 'org.zstack.header.storage.backup.DownloadImageMsg', 'org.zstack.header.image.APIAddImageMsg', '7e8f173fd2f54f4990158878c3ccaa19', 1627807494968, 1627807494990, 0.02, 0);\n" +
                "SET FOREIGN_KEY_CHECKS = 1;"
        String sql2TestApi = "SET NAMES utf8mb4;\n" +
                "SET FOREIGN_KEY_CHECKS = 0;\n" +
                "DROP TABLE IF EXISTS `MsgLogVO`;\n" +
                "CREATE TABLE `MsgLogVO`  (\n" +
                "  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,\n" +
                "  `uuid` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,\n" +
                "  `msgId` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,\n" +
                "  `msgName` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,\n" +
                "  `taskName` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,\n" +
                "  `apiId` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,\n" +
                "  `startTime` bigint(20) NULL DEFAULT NULL,\n" +
                "  `replyTime` bigint(20) NULL DEFAULT NULL,\n" +
                "  `wait` decimal(7, 2) NULL DEFAULT NULL,\n" +
                "  `status` int(1) NOT NULL,\n" +
                "  PRIMARY KEY (`id`) USING BTREE,\n" +
                "  UNIQUE INDEX `id`(`id`) USING BTREE\n" +
                ") ENGINE = InnoDB AUTO_INCREMENT = 77 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = COMPACT;\n" +
                "INSERT INTO `MsgLogVO` VALUES (1, '97e8ede291574be1ad3284fae2730c9f', '28d4bd5ed6a2406991698185816644e0', 'org.zstack.header.storage.backup.ConnectBackupStorageMsg', 'org.zstack.storage.backup.sftp.APIAddSftpBackupStorageMsg', 'cdca968b6929442cbdba232e01c849c8', 1627807481309, 1627807481760, 0.45, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (2, '1cc9fbcb51d647d5a2610e4e44050628', 'eeca3e6cb2e7483fa813c9cc708be751', 'org.zstack.header.storage.backup.DownloadImageMsg', 'org.zstack.header.image.APIAddImageMsg', '499549ed768e4a85b48cbed4c41308e2', 1627807481951, 1627807482128, 0.18, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (3, '6fbf0212788b45b5b215e3f259a1fc0f', '935440d7aa83485dbb717ae7aba4d112', 'org.zstack.header.storage.backup.DownloadImageMsg', 'org.zstack.header.image.APIAddImageMsg', 'ade074daf26d40c4a7bdb08dec2902a6', 1627807482437, 1627807482463, 0.03, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (4, '9501b90aeb2d4ab2938e3e83d22be4d6', 'c33d0cfd66a542498f114599bec1f7b2', 'org.zstack.header.storage.primary.ConnectPrimaryStorageMsg', 'org.zstack.storage.primary.local.APIAddLocalPrimaryStorageMsg', '93c286c6a04e4f968e772f9c68c971f4', 1627807482836, 1627807483072, 0.24, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (5, '8947d8406f77450e983e2fd56c17a0d3', 'eac9b3ca30eb418eb31899dfdc775aae', 'org.zstack.storage.primary.local.InitPrimaryStorageOnHostConnectedMsg', 'org.zstack.kvm.APIAddKVMHostMsg', 'ca5850ada93946099435a85a49bcd1f6', 1627807484080, 1627807484410, 0.33, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (6, 'e9f7c6c93c1b42b28fd142fdd5c42566', 'a528e06fc2c64b68be5d6c24a5fac7b3', 'org.zstack.header.cluster.ReportHostCapacityMessage', 'org.zstack.kvm.APIAddKVMHostMsg', 'ca5850ada93946099435a85a49bcd1f6', 1627807484514, 1627807484529, 0.02, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (7, '0dc559ea129d4a0db2eceeaedc24b157', 'fb2e89a13055458fad0990bea0d83da5', 'org.zstack.header.host.CheckHostCapacityMsg', 'org.zstack.kvm.APIAddKVMHostMsg', 'ca5850ada93946099435a85a49bcd1f6', 1627807484422, 1627807484533, 0.09, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (8, 'f7e43425345f44e3922a2d7667802b85', 'b29cabcdf6aa4d82819fc0bc3d7549f4', 'org.zstack.header.network.l2.BatchCheckNetworkPhysicalInterfaceMsg', 'org.zstack.kvm.APIAddKVMHostMsg', 'ca5850ada93946099435a85a49bcd1f6', 1627807484677, 1627807484707, 0.03, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (9, 'c5699d1f75fa4afc80cc0d894b4511f8', 'e3b9750d3c1941408e1a1d0e607a893d', 'org.zstack.header.host.ConnectHostMsg', 'org.zstack.kvm.APIAddKVMHostMsg', 'ca5850ada93946099435a85a49bcd1f6', 1627807483739, 1627807484863, 0.65, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (10, '84910f58112140c2b16b631194d7ac09', '22c80926d23547eb90c09908acbe7295', 'org.zstack.network.l3.AttachNetworkServiceToL3Msg', 'org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkMsg', 'a3231863fc3d4a8088ff0ef8963ec67c', 1627807485307, 1627807485319, 0.01, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (11, 'ccba9948db7940f296835453af6a7225', 'e95371ed2ef34b5ea721e9181d062247', 'org.zstack.network.l3.AttachNetworkServiceToL3Msg', 'org.zstack.header.network.service.APIAttachNetworkServiceToL3NetworkMsg', '457e69c31c614547a919a0f45b84d564', 1627807485422, 1627807485429, 0.01, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (12, '9064e42b73e0412893e8812fab279434', '227bc99486b7411397b39155fdb9e955', 'org.zstack.header.allocator.DesignatedAllocateHostMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807486493, 1627807486761, 0.27, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (13, 'dd477c98a32c48419268f75dd52f2fbc', 'f6d65e7b8efc4c18a62a7eb53a5327c7', 'org.zstack.header.storage.primary.AllocatePrimaryStorageMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807486791, 1627807487164, 0.37, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (14, '9a64566e29a945bab0e6392775fa41e9', '8a5517018d0942d3af20e4863446ff2a', 'org.zstack.header.volume.CreateVolumeMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807487177, 1627807487238, 0.06, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (15, 'ef259be58d7d459abacbddc55884e6c3', '2bad0cf35c664f14ae7bb220c86ae305', 'org.zstack.header.network.l3.AllocateIpMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807487271, 1627807487293, 0.02, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (40, 'e8bf5be528c2418faa99e274f498ecfd', '825f6bb71c58409784c4cb3a393591d8', 'org.zstack.header.vm.InstantiateNewCreatedVmInstanceMsg', 'org.zstack.header.vm.APICreateVmInstanceMsg', 'dddb16981c944ddf9ef6798cfc34531c', 1627807486101, 1627807489702, 0.77, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (41, '23ee1fc5c1344138a52f49e13a4936e9', 'ee18b9e4252240869515271494695a8a', 'org.zstack.header.vm.StopVmOnHypervisorMsg', 'org.zstack.header.vm.APIStopVmInstanceMsg', '0da80d524dfa4cbc847068e635c83efb', 1627807490687, 1627807490731, 0.04, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (42, 'caa9a942366f453d9aa1e45d81a48fed', 'ca419fdfa8814c2da00c1b9da1e77f5c', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIStopVmInstanceMsg', '0da80d524dfa4cbc847068e635c83efb', 1627807490918, 1627807490936, 0.02, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (43, '439fc27850ed431898d6bd4ed3640a24', 'aec7d4602d6847129164bdeebfe1ed21', 'org.zstack.header.allocator.DesignatedAllocateHostMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'e249192a29b74bc192c49f067f18db6a', 1627807491110, 1627807491163, 0.05, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (44, '90703d0f08224c5ba887ff94d7ddb3b0', '18d3a72710d94fca9d641bc977682680', 'org.zstack.header.vm.StartVmOnHypervisorMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'e249192a29b74bc192c49f067f18db6a', 1627807491238, 1627807491304, 0.07, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (45, 'cbad3fa470b04c9d939ae06c1f1a250c', '7e00e7aaaec844db96fc22fab2f096ca', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'e249192a29b74bc192c49f067f18db6a', 1627807491318, 1627807491321, 0.00, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (46, 'ee91bbff56a3416892d1f83e631edd6e', 'e9807ac80ba4403581818e53a46fedfb', 'org.zstack.header.vm.StopVmOnHypervisorMsg', 'org.zstack.header.vm.APIRebootVmInstanceMsg', '8d36fbdf7e0f4067b80fe912e9fbd639', 1627807491554, 1627807491570, 0.02, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (47, '1846a1684674430cada01b5cacb24fb7', 'cd8e913bf2c740c8a9037cbc17bca4f3', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIRebootVmInstanceMsg', '8d36fbdf7e0f4067b80fe912e9fbd639', 1627807491618, 1627807491622, 0.00, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (48, '45c264685e034055970a589c9f6d605b', '0fb03e9b9a73469291ded3387c839741', 'org.zstack.header.vm.StartVmOnHypervisorMsg', 'org.zstack.header.vm.APIRebootVmInstanceMsg', '8d36fbdf7e0f4067b80fe912e9fbd639', 1627807491703, 1627807491795, 0.09, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (49, '4918a426d9dd449f9523c0eb4016735e', '14f5c43fc81645eeb7d4965257a3e70a', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIRebootVmInstanceMsg', '8d36fbdf7e0f4067b80fe912e9fbd639', 1627807491800, 1627807491803, 0.00, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (50, 'd195310005bf48a3aa85a52436a21abe', '9c4102bf0c1d41a1b1e61adffca06388', 'org.zstack.header.vm.RebootVmInstanceMsg', 'org.zstack.header.vm.APIRebootVmInstanceMsg', '8d36fbdf7e0f4067b80fe912e9fbd639', 1627807491478, 1627807491837, 0.25, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (51, '4bf6e2126da04c0d82a65aabacf26873', '25c4965557934a4987bb0c314a79285d', 'org.zstack.header.vm.DestroyVmOnHypervisorMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', 'dffd072cdf684411936fb868da6827db', 1627807492000, 1627807492034, 0.03, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (52, 'dae6e57a57dd43f9bfb44479c94b05aa', 'aea889825cd44e02aa36a2e5eba1d8f0', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', 'dffd072cdf684411936fb868da6827db', 1627807492088, 1627807492092, 0.00, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (53, '7a0b3f163d56400aadba530791549617', '5211a1868f2b40bcae87d388a9416266', 'org.zstack.header.volume.VolumeDeletionMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', 'dffd072cdf684411936fb868da6827db', 1627807492145, 1627807492184, 0.04, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (54, '0af20a37c6c142158bfdf8135f2ed6a0', 'e8d26515b37a4162a6ca5cdf76f7897f', 'org.zstack.header.vm.VmInstanceDeletionMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', 'dffd072cdf684411936fb868da6827db', 1627807491952, 1627807492331, 0.31, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (55, '42c5e73f257e41aa8c8e221f8d7cdf76', '8717f4627bef496496d8aa1bdead7ac3', 'org.zstack.header.volume.RecoverVolumeMsg', 'org.zstack.header.vm.APIRecoverVmInstanceMsg', 'f35f076e38d04d07bce9f29f0e9d58bc', 1627807492435, 1627807492463, 0.03, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (56, '17446ad54e3447359fc81e1b8c4473b9', '74ee8a72c5f04264b72e00e79c01e7b4', 'org.zstack.header.allocator.DesignatedAllocateHostMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'ccea688526194ca7a2623eb4e085aecf', 1627807492573, 1627807492610, 0.04, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (57, 'b54b961db9954b1f94bbd92b1f683be1', '8876ce05a14945d9a7437de4160c8719', 'org.zstack.header.network.l3.AllocateIpMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'ccea688526194ca7a2623eb4e085aecf', 1627807492619, 1627807492635, 0.02, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (58, '271c6727991a4831925c528d79a70575', '421dc6fe8064409a80ae745cf847c8ab', 'org.zstack.header.vm.StartVmOnHypervisorMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'ccea688526194ca7a2623eb4e085aecf', 1627807492705, 1627807492788, 0.08, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (59, 'f117dc3b23604193a871ed4e810ea253', 'a4708e31e2194acdbfeef69c88a4983c', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIStartVmInstanceMsg', 'ccea688526194ca7a2623eb4e085aecf', 1627807492795, 1627807492798, 0.00, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (60, 'a5f105464f034a1ba8d8bf85764b1636', '1015f031be0c4d6fbf186c6f63410f5b', 'org.zstack.header.vm.DestroyVmOnHypervisorMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', '71dc2654a14049a99a7d8bd3db4e758a', 1627807492933, 1627807492960, 0.03, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (61, '678bf7123ecd497fb66f3e7a56962e39', 'ff1ceb8bc64b47ad96355f6aa8955787', 'org.zstack.network.securitygroup.RefreshSecurityGroupRulesOnVmMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', '71dc2654a14049a99a7d8bd3db4e758a', 1627807493008, 1627807493013, 0.01, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (62, '5d54bf7d97704fc09edcc733d211d210', '8cd8f4cf769543a495011f15c26240ad', 'org.zstack.header.volume.VolumeDeletionMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', '71dc2654a14049a99a7d8bd3db4e758a', 1627807493039, 1627807493053, 0.01, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (63, 'f7502582398c4b918ea54df8beb13c52', 'efb223002ceb47b9baed3aa7ae1357e5', 'org.zstack.header.vm.VmInstanceDeletionMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', '71dc2654a14049a99a7d8bd3db4e758a', 1627807492906, 1627807493072, 0.12, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (64, '5176fcc1cccc439fa57b6446d0a18b96', '19f38138f25d4a9fb5dbc2285468f247', 'org.zstack.header.storage.primary.DeleteVolumeOnPrimaryStorageMsg', 'org.zstack.header.vm.APIExpungeVmInstanceMsg', 'fb1e61de7a9649d29fbefc35c3b44f98', 1627807493205, 1627807493287, 0.08, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (65, '9a97dd50be4941b391d716881a699acf', '7a5a2506411e4b1c9a461ecc3e107f2b', 'org.zstack.header.volume.ExpungeVolumeMsg', 'org.zstack.header.vm.APIExpungeVmInstanceMsg', 'fb1e61de7a9649d29fbefc35c3b44f98', 1627807493174, 1627807493321, 0.07, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (66, '201cc0a7784d4088a5eaa6748da66206', '9fa0edfa895c4d31bc5c82725fa9886b', 'org.zstack.header.vm.VmInstanceDeletionMsg', 'org.zstack.header.vm.APIDestroyVmInstanceMsg', 'e9d4887577b84cf4bc5635fba7079e37', 1627807493678, 1627807493719, 0.04, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (67, '28c6f50b683b4ae0aa7dca65699361ec', 'f2b3e5d2ff6f44d5bce720c6bf333907', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.zone.APIChangeZoneStateMsg', 'af2a6fb076f44504a94a061a50c97a96', 1627807493841, 1627807493862, 0.02, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (68, '3e540b13f7364780bf6cf111cafcf71d', 'b0daf2708eff4268be4379594d01a344', 'org.zstack.header.cluster.ChangeClusterStateMsg', 'org.zstack.header.zone.APIChangeZoneStateMsg', 'af2a6fb076f44504a94a061a50c97a96', 1627807493809, 1627807493875, 0.05, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (69, 'cfbe2af2567b46b5a5f9c308e1541a7d', '5a508422f42445c19e5a05e61eaaabb5', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.zone.APIChangeZoneStateMsg', '51135c3785de4d0abdef58ab8dd97c8f', 1627807494009, 1627807494016, 0.01, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (70, '09391fa7154a492983c04c76b79c46de', '089ed87730aa4486964d1ccf95ec763e', 'org.zstack.header.cluster.ChangeClusterStateMsg', 'org.zstack.header.zone.APIChangeZoneStateMsg', '51135c3785de4d0abdef58ab8dd97c8f', 1627807494005, 1627807494022, 0.01, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (71, '7ca511d0cc2e40dfb2c0cdcc7edfc6ec', '786720a85fcf4ea78fb2ca32188d4083', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.cluster.APIChangeClusterStateMsg', '95984512ef8846139cb9043a3c5cadb6', 1627807494086, 1627807494101, 0.02, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (72, 'ebf1612fe78247f28ebce80b03bd85d5', 'b4f69acc4a464999b5f4ad124f92817e', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.cluster.APIChangeClusterStateMsg', 'b7b6662afb3242c0aa6645f46b9508e2', 1627807494194, 1627807494202, 0.01, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (73, '4866309491504f7fa307831cd6e713f1', 'f3d69a75843945b9a7873387222ca31d', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.host.APIChangeHostStateMsg', '0fd332cbd9f2415d85260a4776f13548', 1627807494267, 1627807494279, 0.01, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (74, '31249a9730384c19ad1bcb57a6396bf5', '17de2c5677a54f6b8ad2ac0741dbca47', 'org.zstack.header.host.ChangeHostStateMsg', 'org.zstack.header.host.APIChangeHostStateMsg', 'cb8848110f1944e7a9b4151193e2c283', 1627807494379, 1627807494389, 0.01, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (75, '11040e5b33fb4fa7b081c4a1f5835edd', 'd240e88311594ca689bd500120cafa2a', 'org.zstack.header.storage.backup.DownloadImageMsg', 'org.zstack.header.image.APIAddImageMsg', 'fd776540ccfd4eb1a274d5be89561920', 1627807494844, 1627807494870, 0.03, 1);\n" +
                "INSERT INTO `MsgLogVO` VALUES (76, '9c161698cc214604982b6e3cb2ed9526', 'f28eca787da64d95a39edb0bdacbb933', 'org.zstack.header.storage.backup.DownloadImageMsg', 'org.zstack.header.image.APIAddImageMsg', '7e8f173fd2f54f4990158878c3ccaa19', 1627807494968, 1627807494990, 0.02, 1);\n" +
                "SET FOREIGN_KEY_CHECKS = 1;"
        // 导入MsgLog数据，用于创建步骤图
        insertSql(sql2CreateStepGraph)
        // 初始化预测引擎
        new Predictor()

        // 导入MsgLog数据，用于测试api
        insertSql(sql2TestApi)
        // 预测耗时
        PredictResult predictResult = ApiSpendTimePredictor.predictByMaxPath("dddb16981c944ddf9ef6798cfc34531c")
        assert predictResult.getSpendTime().compareTo(new BigDecimal("2.040")) == 0
    }

    // 插入数据到数据库
    void insertSql(String sql) {
        String home = System.getProperty("user.dir")
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("MsgLogVO.sql"))
            out.write(sql)
            out.close()
            String path = Utils.getPathUtil().join(home, "MsgLogVO.sql")
            ShellUtils.run(String.format("mysql -uroot zstack -e \"source %s\"", path), false)
            ShellUtils.run(String.format("rm -f %s", path), false)
        } catch (IOException e) {
            String path = Utils.getPathUtil().join(home, "MsgLogVO.sql")
            ShellUtils.run(String.format("rm -f %s", path), false)
            e.printStackTrace()
        }
    }

    @Override
    void clean() {
    }
}
