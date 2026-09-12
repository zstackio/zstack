package org.zstack.sdk;

import org.zstack.sdk.DocumentProcessing;
import org.zstack.sdk.QuestionGeneration;
import org.zstack.sdk.EvalQuestionGeneration;
import org.zstack.sdk.ConversationGeneration;
import org.zstack.sdk.ImageVqaGeneration;
import org.zstack.sdk.Execution;

public class ZsDatasetTaskSettingsConfigSpec  {

    public DocumentProcessing documentProcessing;
    public void setDocumentProcessing(DocumentProcessing documentProcessing) {
        this.documentProcessing = documentProcessing;
    }
    public DocumentProcessing getDocumentProcessing() {
        return this.documentProcessing;
    }

    public QuestionGeneration questionGeneration;
    public void setQuestionGeneration(QuestionGeneration questionGeneration) {
        this.questionGeneration = questionGeneration;
    }
    public QuestionGeneration getQuestionGeneration() {
        return this.questionGeneration;
    }

    public EvalQuestionGeneration evalQuestionGeneration;
    public void setEvalQuestionGeneration(EvalQuestionGeneration evalQuestionGeneration) {
        this.evalQuestionGeneration = evalQuestionGeneration;
    }
    public EvalQuestionGeneration getEvalQuestionGeneration() {
        return this.evalQuestionGeneration;
    }

    public ConversationGeneration conversationGeneration;
    public void setConversationGeneration(ConversationGeneration conversationGeneration) {
        this.conversationGeneration = conversationGeneration;
    }
    public ConversationGeneration getConversationGeneration() {
        return this.conversationGeneration;
    }

    public ImageVqaGeneration imageVqaGeneration;
    public void setImageVqaGeneration(ImageVqaGeneration imageVqaGeneration) {
        this.imageVqaGeneration = imageVqaGeneration;
    }
    public ImageVqaGeneration getImageVqaGeneration() {
        return this.imageVqaGeneration;
    }

    public Execution execution;
    public void setExecution(Execution execution) {
        this.execution = execution;
    }
    public Execution getExecution() {
        return this.execution;
    }

}
