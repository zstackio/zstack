package org.zstack.sdk;

import org.zstack.sdk.Ratios;

public class EvalQuestionGeneration extends org.zstack.sdk.QuestionGeneration {

    public Ratios ratios;
    public void setRatios(Ratios ratios) {
        this.ratios = ratios;
    }
    public Ratios getRatios() {
        return this.ratios;
    }

}
