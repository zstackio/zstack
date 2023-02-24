package org.zstack.kvm.hypervisor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoVO;
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoVO_;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.zstack.kvm.KVMAgentCommands.GetVirtualizerInfoRsp;
import static org.zstack.kvm.KVMAgentCommands.VirtualizerInfoTO;

/**
 * Created by Wenhao.Zhang on 23/02/23
 */
public class KvmHypervisorInfoManagerImpl implements KvmHypervisorInfoManager {
    @Autowired
    private DatabaseFacade db;

    @Override
    public void save(GetVirtualizerInfoRsp rsp) {
        List<VirtualizerInfoTO> tos = new ArrayList<>(rsp.getVmInfoList());
        tos.add(rsp.getHostInfo());
        save(tos);
    }

    @Transactional
    private void save(List<VirtualizerInfoTO> list) {
        Map<String, VirtualizerInfoTO> uuidToMap = list.stream()
                .collect(Collectors.toMap(VirtualizerInfoTO::getUuid, Function.identity()));

        List<KvmHypervisorInfoVO> voList = Q.New(KvmHypervisorInfoVO.class)
                .in(KvmHypervisorInfoVO_.uuid, new ArrayList<>(uuidToMap.keySet()))
                .list();
        Map<String, KvmHypervisorInfoVO> uuidVoMapToUpdate = voList.stream()
                .collect(Collectors.toMap(KvmHypervisorInfoVO::getUuid, Function.identity()));
        if (!uuidVoMapToUpdate.isEmpty()) {
            uuidVoMapToUpdate.forEach((uuid, vo) -> updateKvmHypervisorInfoVO(uuid, vo, uuidToMap.get(uuid)));
            db.updateCollection(voList);
        }

        List<KvmHypervisorInfoVO> voListToPersist = uuidToMap.entrySet().stream()
                .filter(entry -> !uuidVoMapToUpdate.containsKey(entry.getKey()))
                .map(entry -> updateKvmHypervisorInfoVO(entry.getKey(), null, uuidToMap.get(entry.getKey())))
                .collect(Collectors.toList());
        if (!voListToPersist.isEmpty()) {
            db.persistCollection(voListToPersist);
        }
    }

    private KvmHypervisorInfoVO updateKvmHypervisorInfoVO(String uuid, KvmHypervisorInfoVO vo, VirtualizerInfoTO to) {
        if (vo == null) {
            vo = new KvmHypervisorInfoVO();
            vo.setUuid(uuid);
        }
        vo.setHypervisor(to.getVirtualizer());
        vo.setVersion(to.getVersion());
        return vo;
    }
}
