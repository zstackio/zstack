import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.List;

/**
 * Created by frank on 6/30/2015.
 */
public abstract class LocalStorageHypervisorBackend extends LocalStorageBase {
    public LocalStorageHypervisorBackend(PrimaryStorageVO self) {
        super(self);
    }

    abstract void syncPhysicalCapacityInCluster(List<ClusterInventory> clusters, ReturnValueCompletion<PhysicalCapacityUsage> completion);
}
