package org.zstack.header.network.l2;

import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HypervisorType;

import java.util.ArrayList;
import java.util.List;

public interface L2NetworkRealizationExtensionPoint {
    void realize(L2NetworkInventory l2Network, String hostUuid, Completion completion);

    default void realize(L2NetworkInventory l2Network, String hostUuid, boolean noStatusCheck, Completion completion) {
        completion.success();
    }

    default void realize(List<L2NetworkInventory> l2Networks, String hostUuid, boolean noStatusCheck, Completion completion) {
        if (l2Networks == null || l2Networks.isEmpty()) {
            completion.success();
            return;
        }
        realizeByIteration(l2Networks, 0, hostUuid, noStatusCheck, new ArrayList<>(), completion);
    }

    default void realizeByIteration(List<L2NetworkInventory> l2Networks, int index, String hostUuid, boolean noStatusCheck, List<ErrorCode> errs, Completion completion) {
        if (index >= l2Networks.size()) {
            if (!errs.isEmpty()) {
                completion.fail(errs.get(0));
            } else {
                completion.success();
            }
            return;
        }
        realize(l2Networks.get(index), hostUuid, noStatusCheck, new Completion(completion) {
            @Override
            public void success() {
                realizeByIteration(l2Networks, index + 1, hostUuid, noStatusCheck, errs, completion);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    void check(L2NetworkInventory l2Network, String hostUuid, Completion completion);

    L2NetworkType getSupportedL2NetworkType();

    HypervisorType getSupportedHypervisorType();

    VSwitchType getSupportedVSwitchType();

    default L2ProviderType getL2ProviderType() {
        return null;
    };

    void delete(L2NetworkInventory l2Network, String hostUuid, Completion completion);
}
