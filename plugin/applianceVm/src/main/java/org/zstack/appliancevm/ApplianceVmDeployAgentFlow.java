package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.appliancevm.ApplianceVmConstant.Params;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.ansible.AnsibleGlobalProperty;
import org.zstack.core.ansible.AnsibleRunner;
import org.zstack.core.ansible.SshFileMd5Checker;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowTrigger;
import org.zstack.core.workflow.NoRollbackFlow;
import org.zstack.header.configuration.ConfigurationConstant;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;
import org.zstack.utils.path.PathUtil;

import java.util.Map;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ApplianceVmDeployAgentFlow extends NoRollbackFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private GlobalConfigFacade gcf;
    @Autowired
    private RESTFacade restf;

    private void continueConnect(String echoUrl, final FlowTrigger trigger) {
        restf.echo(echoUrl, new Completion(trigger) {
            @Override
            public void success() {
                trigger.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        boolean isReconnect = Boolean.valueOf((String) data.get(Params.isReconnect.toString()));

        String mgmtNicIp;
        if (!isReconnect) {
            final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            VmNicInventory mgmtNic;
            if (spec.getCurrentVmOperation() == VmOperation.NewCreate) {
                final ApplianceVmSpec aspec = spec.getExtensionData(ApplianceVmConstant.Params.applianceVmSpec.toString(), ApplianceVmSpec.class);
                mgmtNic = CollectionUtils.find(spec.getDestNics(), new Function<VmNicInventory, VmNicInventory>() {
                    @Override
                    public VmNicInventory call(VmNicInventory arg) {
                        return arg.getL3NetworkUuid().equals(aspec.getManagementNic().getL3NetworkUuid()) ? arg : null;
                    }
                });
            } else {
                ApplianceVmVO avo = dbf.findByUuid(spec.getVmInventory().getUuid(), ApplianceVmVO.class);
                ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(avo);
                mgmtNic = ainv.getManagementNic();
            }
            mgmtNicIp = mgmtNic.getIp();
        } else {
            mgmtNicIp = (String) data.get(Params.managementNicIp.toString());
        }

        final String mgmtIp = mgmtNicIp;
        final String url = ApplianceVmBase.buildAgentUrl(mgmtIp, ApplianceVmConstant.ECHO_PATH);

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            continueConnect(url, trigger);
            return;
        } else if (!isReconnect && !ApplianceVmGlobalConfig.DEPLOY_AGENT_ON_START.value(Boolean.class)) {
            continueConnect(url, trigger);
            return;
        }

        final String username = "root";
        final String privKey = gcf.getConfigValue(ConfigurationConstant.GlobalConfig.privateKey.getCategory(),
                ConfigurationConstant.GlobalConfig.privateKey.toString(), String.class);

        SshFileMd5Checker checker = new SshFileMd5Checker();
        checker.setTargetIp(mgmtIp);
        checker.setUsername(username);
        checker.setPrivateKey(privKey);
        checker.addSrcDestPair(SshFileMd5Checker.ZSTACKLIB_SRC_PATH, String.format("/var/lib/zstack/appliancevm/%s", AnsibleGlobalProperty.ZSTACKLIB_PACKAGE_NAME));
        if (!AnsibleGlobalProperty.USE_PACKAGED_VIRTUAL_ENV) {
            checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/appliancevm/%s", ApplianceVmGlobalProperty.AGENT_PACKAGE_NAME), true).getAbsolutePath(),
                    String.format("/var/lib/zstack/appliancevm/%s", ApplianceVmGlobalProperty.AGENT_PACKAGE_NAME));
        } else {
            checker.addSrcDestPair(PathUtil.findFileOnClassPath(String.format("ansible/appliancevm/%s", ApplianceVmGlobalProperty.VIRTUAL_ENV_PACKAGE), true).getAbsolutePath(),
                    String.format("/var/lib/zstack/appliancevm/%s", ApplianceVmGlobalProperty.VIRTUAL_ENV_PACKAGE));
        }

        AnsibleRunner runner = new AnsibleRunner();
        runner.installChecker(checker);
        runner.setUsername(username);
        runner.setPlayBookName(ApplianceVmConstant.ANSIBLE_PLAYBOOK_NAME);
        runner.setPrivateKey(privKey);
        runner.setAgentPort(ApplianceVmGlobalProperty.AGENT_PORT);
        runner.setTargetIp(mgmtIp);
        if (!AnsibleGlobalProperty.USE_PACKAGED_VIRTUAL_ENV) {
            runner.putArgument("pkg_appliancevm", ApplianceVmGlobalProperty.AGENT_PACKAGE_NAME);
        } else {
            runner.putArgument("virtualenv_pkg", ApplianceVmGlobalProperty.VIRTUAL_ENV_PACKAGE);
        }
        runner.run(new Completion(trigger) {
            @Override
            public void success() {
                continueConnect(url, trigger);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        });
    }
}
