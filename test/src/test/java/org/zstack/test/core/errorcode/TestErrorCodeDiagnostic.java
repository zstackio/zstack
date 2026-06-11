package org.zstack.test.core.errorcode;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeDiagnostic;
import org.zstack.header.errorcode.ErrorCodeDiagnosticHelper;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.utils.clouderrorcode.CloudOperationsErrorCode;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.string.ErrorCodeElaboration;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestErrorCodeDiagnostic {
    @Test
    public void testMapGlobalErrorCodeAndLocalizedMessage() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "raw primary storage detail");
        error.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039");
        error.setMessage("主存储不存在");

        ErrorCodeDiagnostic diagnostic = error.toDiagnostic();

        Assert.assertEquals("Cloud", diagnostic.getComponent());
        Assert.assertEquals("STORAGE_PRIMARY", diagnostic.getCategory());
        Assert.assertEquals("10039", diagnostic.getCode());
        Assert.assertEquals("主存储不存在", diagnostic.getMessage());
        Assert.assertEquals("raw primary storage detail", diagnostic.getRawMessage().getSymptom());
        Assert.assertNull(diagnostic.getRawMessage().getCause());
        Assert.assertNull(diagnostic.getRawMessage().getSolution());
    }

    @Test
    public void testFallbackCategoryFromVmPrefix() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "setup nic failed");
        error.setGlobalErrorCode("ORG_ZSTACK_COMPUTE_VM_10331");

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);

        Assert.assertEquals("VM", diagnostic.getCategory());
        Assert.assertEquals("10331", diagnostic.getCode());
        Assert.assertEquals("setup nic failed", diagnostic.getMessage());
    }

    @Test
    public void testRestCoreErrorMapsToInternalDiagnosticBand() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "not found");
        error.setGlobalErrorCode("ORG_ZSTACK_CORE_REST_10015");

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);

        Assert.assertEquals("CORE_REST", diagnostic.getCategory());
        Assert.assertEquals("90015", diagnostic.getCode());
    }

    @Test
    public void testInvalidArgumentErrorMapsToArgumentDiagnosticBand() {
        ErrorCode error = new ErrorCode(SysErrors.INVALID_ARGUMENT_ERROR.toString(), "Invalid Argument", "invalid parameter");
        error.setGlobalErrorCode("ORG_ZSTACK_CORE_ARGUMENT_10015");

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);

        Assert.assertEquals("20015", diagnostic.getCode());
    }

    @Test
    public void testSysInvalidArgumentErrorMapsToArgumentDiagnosticBand() {
        ErrorCode error = new ErrorCode(SysErrors.INVALID_ARGUMENT_ERROR.toString(), "Invalid Argument", "invalid parameter");

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);

        Assert.assertEquals("21007", diagnostic.getCode());
    }

    @Test
    public void testIdentityLoginErrorMapsToAuthDiagnosticBand() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "login failed");
        error.setGlobalErrorCode("ORG_ZSTACK_IDENTITY_LOGIN_10000");

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);

        Assert.assertEquals("IDENTITY_LOGIN", diagnostic.getCategory());
        Assert.assertEquals("30000", diagnostic.getCode());
    }

    @Test
    public void testExternalDependencyErrorMapsToExternalDiagnosticBand() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "agent call failed");
        error.setGlobalErrorCode("ORG_ZSTACK_CORE_KVMAGENT_10042");

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);

        Assert.assertEquals("40042", diagnostic.getCode());
    }

    @Test
    public void testStorageSubModulesUseDistinctDiagnosticCategories() {
        ErrorCode ceph = new ErrorCode("SYS.1006", "Operation Error", "ceph backup failed");
        ceph.setGlobalErrorCode("ORG_ZSTACK_STORAGE_CEPH_BACKUP_10000");
        ErrorCode imageStore = new ErrorCode("SYS.1006", "Operation Error", "imagestore backup failed");
        imageStore.setGlobalErrorCode("ORG_ZSTACK_STORAGE_BACKUP_IMAGESTORE_10000");

        ErrorCodeDiagnostic cephDiagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(ceph);
        ErrorCodeDiagnostic imageStoreDiagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(imageStore);

        Assert.assertEquals("STORAGE_CEPH_BACKUP", cephDiagnostic.getCategory());
        Assert.assertEquals("STORAGE_BACKUP_IMAGESTORE", imageStoreDiagnostic.getCategory());
        Assert.assertEquals("10000", cephDiagnostic.getCode());
        Assert.assertEquals("10000", imageStoreDiagnostic.getCode());
        Assert.assertNotEquals(uniqueDiagnosticKey(cephDiagnostic), uniqueDiagnosticKey(imageStoreDiagnostic));
    }

    @Test
    public void testAutoscalingAndApplianceVmDoNotFallBackToSameOtherDiagnosticKey() {
        ErrorCode autoscaling = new ErrorCode("SYS.1006", "Operation Error", "autoscaling failed");
        autoscaling.setGlobalErrorCode("ORG_ZSTACK_AUTOSCALING_10000");
        ErrorCode applianceVm = new ErrorCode("SYS.1006", "Operation Error", "appliance vm failed");
        applianceVm.setGlobalErrorCode("ORG_ZSTACK_APPLIANCEVM_10000");

        ErrorCodeDiagnostic autoscalingDiagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(autoscaling);
        ErrorCodeDiagnostic applianceVmDiagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(applianceVm);

        Assert.assertEquals("AUTOSCALING", autoscalingDiagnostic.getCategory());
        Assert.assertEquals("APPLIANCEVM", applianceVmDiagnostic.getCategory());
        Assert.assertEquals("90000", autoscalingDiagnostic.getCode());
        Assert.assertEquals("90000", applianceVmDiagnostic.getCode());
        Assert.assertNotEquals(uniqueDiagnosticKey(autoscalingDiagnostic), uniqueDiagnosticKey(applianceVmDiagnostic));
    }

    @Test
    public void testCloudOperationsErrorCodeDiagnosticKeysAreUnique() throws IllegalAccessException {
        Set<String> keys = new HashSet<>();
        for (Field field : CloudOperationsErrorCode.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers) || field.getType() != String.class ||
                    !field.getName().startsWith("ORG_ZSTACK_")) {
                continue;
            }

            String globalErrorCode = (String) field.get(null);
            ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", globalErrorCode);
            error.setGlobalErrorCode(globalErrorCode);
            ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);
            String key = uniqueDiagnosticKey(diagnostic);
            Assert.assertTrue(String.format("duplicated diagnostic key[%s] from globalErrorCode[%s]", key, globalErrorCode), keys.add(key));
        }
    }

    @Test
    public void testElaborationCategoryDoesNotOverrideStableDiagnosticCategory() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "host connection failed");
        error.setGlobalErrorCode("ORG_ZSTACK_COMPUTE_VM_10331");
        ErrorCodeElaboration elaboration = new ErrorCodeElaboration();
        elaboration.setCategory("HOST");
        elaboration.setMessage_en("Host connection failed.");
        error.setMessages(elaboration);

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);

        Assert.assertEquals("VM", diagnostic.getCategory());
        Assert.assertEquals("Host connection failed.", diagnostic.getRawMessage().getSymptom());
    }

    @Test
    public void testUnreviewedDiagnosticDoesNotExposeCauseAndSolution() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "raw symptom");
        error.withOpaque(ErrorCodeDiagnosticHelper.OPAQUE_CAUSE, "not reviewed cause");
        error.withOpaque(ErrorCodeDiagnosticHelper.OPAQUE_SOLUTION, "not reviewed solution");

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error, "en_US");

        Assert.assertFalse(diagnostic.isReviewed());
        Assert.assertEquals("raw symptom", diagnostic.getRawMessage().getSymptom());
        Assert.assertNull(diagnostic.getRawMessage().getCause());
        Assert.assertNull(diagnostic.getRawMessage().getSolution());

        String json = JSONObjectUtil.toJsonString(diagnostic);
        Assert.assertFalse(json.contains("\"cause\""));
        Assert.assertFalse(json.contains("\"solution\""));
    }

    @Test
    public void testReviewedDiagnosticExposesCauseSolutionAndNote() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "raw symptom");
        error.withOpaque(ErrorCodeDiagnosticHelper.OPAQUE_REVIEWED, true);
        error.withOpaque(ErrorCodeDiagnosticHelper.OPAQUE_CAUSE, "reviewed cause");
        error.withOpaque(ErrorCodeDiagnosticHelper.OPAQUE_SOLUTION, "reviewed solution");
        error.withOpaque(ErrorCodeDiagnosticHelper.OPAQUE_NOTE, "reviewed note");

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);

        Assert.assertTrue(diagnostic.isReviewed());
        Assert.assertEquals("reviewed cause", diagnostic.getRawMessage().getCause());
        Assert.assertEquals("reviewed solution", diagnostic.getRawMessage().getSolution());
        Assert.assertEquals("reviewed note", diagnostic.getRawMessage().getNote());
    }

    @Test
    public void testJsonFieldNamesMatchZcfDiagnosticContract() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "raw symptom");
        error.setGlobalErrorCode("ORG_ZSTACK_NETWORK_L3_10077");
        error.setMessage("network failure");

        String json = JSONObjectUtil.toJsonString(ErrorCodeDiagnosticHelper.toDiagnostic(error));

        Assert.assertTrue(json.contains("\"component\":\"Cloud\""));
        Assert.assertTrue(json.contains("\"category\":\"NETWORK_L3\""));
        Assert.assertTrue(json.contains("\"code\":\"10077\""));
        Assert.assertTrue(json.contains("\"message\":\"network failure\""));
        Assert.assertTrue(json.contains("\"raw_message\""));
        Assert.assertFalse(json.contains("\"rawMessage\""));
        Assert.assertTrue(json.contains("\"symptom\":\"raw symptom\""));
        Assert.assertFalse(json.contains("\"cause\""));
        Assert.assertFalse(json.contains("\"solution\""));
    }

    @Test
    public void testErrorCodeListFallsBackToFirstCause() {
        ErrorCode cause = new ErrorCode("SYS.1006", "Operation Error", "first cause symptom");
        cause.setGlobalErrorCode("ORG_ZSTACK_STORAGE_PRIMARY_10039");
        cause.setMessage("primary storage failure");

        ErrorCodeList list = new ErrorCodeList();
        list.setCauses(Collections.singletonList(cause));

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(list);

        Assert.assertEquals("STORAGE_PRIMARY", diagnostic.getCategory());
        Assert.assertEquals("10039", diagnostic.getCode());
        Assert.assertEquals("primary storage failure", diagnostic.getMessage());
    }

    @Test
    public void testDiagnosticCodeFallsBackToSafeNumericCode() {
        ErrorCode error = new ErrorCode("CUSTOM_ERROR", "Operation Error", "raw symptom");

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error);

        Assert.assertEquals(ErrorCodeDiagnosticHelper.FALLBACK_CODE, diagnostic.getCode());
    }

    @Test
    public void testUnreviewedChineseDiagnosticDoesNotAddFallbackSolution() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "raw symptom");

        ErrorCodeDiagnostic diagnostic = ErrorCodeDiagnosticHelper.toDiagnostic(error, "zh_CN");

        Assert.assertEquals("raw symptom", diagnostic.getRawMessage().getSymptom());
        Assert.assertNull(diagnostic.getRawMessage().getSolution());
    }

    private String uniqueDiagnosticKey(ErrorCodeDiagnostic diagnostic) {
        return String.format("%s:%s:%s", diagnostic.getComponent(), diagnostic.getCategory(), diagnostic.getCode());
    }
}
