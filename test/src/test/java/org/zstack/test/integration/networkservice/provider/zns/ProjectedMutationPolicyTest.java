package org.zstack.test.integration.networkservice.provider.zns;

import org.junit.Test;
import org.zstack.header.network.ProjectedMutationPolicy;

public class ProjectedMutationPolicyTest {
    @Test
    public void staleSourceIsRejectedBeforeMutation() {
        try {
            ProjectedMutationPolicy.requireSourceType("L2VlanNetwork", "L2NoVlanNetwork");
            throw new AssertionError("stale source was accepted");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void conversionWithDependenciesIsRejected() {
        try {
            ProjectedMutationPolicy.requireL2Conversion("L2NoVlanNetwork", "L2VlanNetwork", true);
            throw new AssertionError("unsafe conversion was accepted");
        } catch (IllegalArgumentException expected) {
        }
    }
}
