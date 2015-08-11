package org.zstack.appliancevm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.job.Job;
import org.zstack.core.job.JobContext;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CreateApplianceVmJob implements Job {
	protected static final CLogger logger = Utils.getLogger(CreateApplianceVmJob.class);

	@JobContext
    protected ApplianceVmSpec spec;

	@Autowired
	protected DatabaseFacade dbf;
	@Autowired
	protected CloudBus bus;
    @Autowired
	private AccountManager acntMgr;
    @Autowired
    private ApplianceVmFactory apvmFactory;
    @Autowired
    private TagManager tagMgr;

	@Override
	public void run(final ReturnValueCompletion<Object> complete) {
        // if syncCreate is set, the name is the unique id for the vm
        if (spec.isSyncCreate()) {
            SimpleQuery<ApplianceVmVO> q = dbf.createQuery(ApplianceVmVO.class);
            q.add(ApplianceVmVO_.name, SimpleQuery.Op.EQ, spec.getName());
            q.add(ApplianceVmVO_.state, SimpleQuery.Op.EQ, VmInstanceState.Running);
            ApplianceVmVO vo = q.find();
            if (vo != null) {
                complete.success(ApplianceVmInventory.valueOf(vo));
                return;
            }
        }

        ApplianceVmVO avo = new ApplianceVmVO();
        avo.setName(spec.getName());
        if (spec.getUuid() != null) {
            avo.setUuid(spec.getUuid());
        } else {
            avo.setUuid(Platform.getUuid());
        }
        String defaultRouteL3NetworkUuid = spec.getDefaultRouteL3Network() != null ? spec.getDefaultRouteL3Network().getUuid() : spec.getManagementNic().getL3NetworkUuid();
        avo.setDefaultRouteL3NetworkUuid(defaultRouteL3NetworkUuid);
        avo.setDescription(spec.getDescription());
        avo.setImageUuid(spec.getTemplate().getUuid());
        avo.setInstanceOfferingUuid(spec.getInstanceOffering().getUuid());
        avo.setState(VmInstanceState.Created);
        avo.setType(ApplianceVmConstant.APPLIANCE_VM_TYPE);
		avo.setInternalId(dbf.generateSequenceNumber(VmInstanceSequenceNumberVO.class));
        avo.setApplianceVmType(spec.getApplianceVmType().toString());

        SimpleQuery<ImageVO> imgq = dbf.createQuery(ImageVO.class);
        imgq.select(ImageVO_.platform);
        imgq.add(ImageVO_.uuid, Op.EQ, spec.getTemplate().getUuid());
        ImagePlatform platform = imgq.findValue();
        avo.setPlatform(platform.toString());

        InstanceOfferingVO iovo = dbf.findByUuid(spec.getInstanceOffering().getUuid(), InstanceOfferingVO.class);
        avo.setCpuNum(iovo.getCpuNum());
        avo.setCpuSpeed(iovo.getCpuSpeed());
        avo.setMemorySize(iovo.getMemorySize());
        avo.setAllocatorStrategy(iovo.getAllocatorStrategy());
		
		acntMgr.createAccountResourceRef(spec.getAccountUuid(), avo.getUuid(), VmInstanceVO.class);

        ApplianceVmSubTypeFactory factory = apvmFactory.getApplianceVmSubTypeFactory(avo.getApplianceVmType());
        avo = factory.persistApplianceVm(spec, avo);

        tagMgr.copySystemTag(iovo.getUuid(), InstanceOfferingVO.class.getSimpleName(), avo.getUuid(), VmInstanceVO.class.getSimpleName());
        if (spec.getInherentSystemTags() != null && !spec.getInherentSystemTags().isEmpty()) {
            tagMgr.createInherentSystemTags(spec.getInherentSystemTags(), avo.getUuid(), VmInstanceVO.class.getSimpleName());
        }
        if (spec.getNonInherentSystemTags() != null && !spec.getNonInherentSystemTags().isEmpty()) {
            tagMgr.createNonInherentSystemTags(spec.getNonInherentSystemTags(), avo.getUuid(), VmInstanceVO.class.getSimpleName());
        }

		final ApplianceVmInventory inv = ApplianceVmInventory.valueOf(avo);
		StartNewCreatedApplianceVmMsg msg = new StartNewCreatedApplianceVmMsg();
        List<String> nws = new ArrayList<String>();
        nws.add(spec.getManagementNic().getL3NetworkUuid());
        for (ApplianceVmNicSpec nicSpec : spec.getAdditionalNics()) {
            nws.add(nicSpec.getL3NetworkUuid());
        }
		msg.setL3NetworkUuids(nws);
		msg.setVmInstanceInventory(inv);
        msg.setApplianceVmSpec(spec);
        bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, inv.getUuid());
        final ApplianceVmVO finalAvo = avo;
        bus.send(msg, new CloudBusCallBack(complete) {
			@Override
			public void run(MessageReply reply) {
				if (reply.isSuccess()) {
					logger.debug(String.format("successfully created appliance vm[uuid:%s, name: %s, appliance vm type: %s]", inv.getUuid(), inv.getName(), inv.getApplianceVmType()));
                    ApplianceVmVO apvo = dbf.findByUuid(finalAvo.getUuid(), ApplianceVmVO.class);
					ApplianceVmInventory ainv = ApplianceVmInventory.valueOf(apvo);
					complete.success(ainv);
				} else {
                    logger.warn(String.format("failed to create appliance vm[uuid:%s, name: %s, appliance vm type: %s], %s", inv.getUuid(), inv.getName(), inv.getApplianceVmType(), reply.getError()));
					complete.fail(reply.getError());
				}
			}
		});
	}

    public ApplianceVmSpec getSpec() {
        return spec;
    }

    public void setSpec(ApplianceVmSpec spec) {
        this.spec = spec;
    }
}
