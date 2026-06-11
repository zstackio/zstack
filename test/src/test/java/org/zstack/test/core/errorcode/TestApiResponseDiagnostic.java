package org.zstack.test.core.errorcode;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.rest.ApiResponse;
import org.zstack.utils.gson.JSONObjectUtil;

public class TestApiResponseDiagnostic {
    @Test
    public void testFailureResponseKeepsLegacyErrorAndAddsDiagnostic() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "raw symptom");
        error.setGlobalErrorCode("ORG_ZSTACK_COMPUTE_VM_10331");
        error.setMessage("VM NIC lifecycle setup failed");

        ApiResponse response = new ApiResponse();
        response.setError(error);
        response.completeFailure("7e9bb780c1d94c46a2dd12bf4f8debaa", "en_US");

        Assert.assertEquals("7e9bb780c1d94c46a2dd12bf4f8debaa", response.getId());
        Assert.assertEquals("false", response.getSuccess());
        Assert.assertSame(error, response.getError());
        Assert.assertNotNull(response.getDiagnostic());
        Assert.assertEquals("Cloud", response.getDiagnostic().getComponent());
        Assert.assertEquals("VM", response.getDiagnostic().getCategory());
        Assert.assertEquals("10331", response.getDiagnostic().getCode());
        Assert.assertEquals("VM NIC lifecycle setup failed", response.getDiagnostic().getMessage());

        String json = JSONObjectUtil.toJsonString(response);
        Assert.assertTrue(json.contains("\"error\""));
        Assert.assertTrue(json.contains("\"diagnostic\""));
        Assert.assertTrue(json.contains("\"success\":\"false\""));
    }

    @Test
    public void testSuccessResponseKeepsLegacyShapeWithoutDiagnosticFields() {
        ApiResponse response = new ApiResponse();

        Assert.assertNull(response.getId());
        Assert.assertNull(response.getSuccess());
        Assert.assertNull(response.getDiagnostic());
        Assert.assertFalse(response.containsKey("id"));
        Assert.assertFalse(response.containsKey("success"));
        Assert.assertFalse(response.containsKey("diagnostic"));
    }

    @Test
    public void testFallbackIdIsGeneratedForFailureWhenApiIdIsMissing() {
        ErrorCode error = new ErrorCode("SYS.1006", "Operation Error", "raw symptom");

        ApiResponse response = new ApiResponse();
        response.setError(error);
        response.completeFailure(null, "en_US");

        Assert.assertNotNull(response.getId());
        Assert.assertTrue(response.getId().matches("^[0-9a-f]{32}$"));
        Assert.assertEquals("false", response.getSuccess());
    }
}
