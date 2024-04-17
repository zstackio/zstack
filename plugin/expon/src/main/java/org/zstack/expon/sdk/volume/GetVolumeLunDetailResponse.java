package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.ExponResponse;

import java.util.List;

public class GetVolumeLunDetailResponse extends ExponResponse {
    private List<VolumeLunModule> lunDetails;

    public List<VolumeLunModule> getLunDetails() {
        return lunDetails;
    }

    public void setLunDetails(List<VolumeLunModule> lunDetails) {
        this.lunDetails = lunDetails;
    }
}
