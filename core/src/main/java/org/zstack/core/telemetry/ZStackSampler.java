package org.zstack.core.telemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ZStackSampler implements Sampler {
    private static final CLogger logger = Utils.getLogger(ZStackSampler.class);

    private final TelemetryGlobalProperty.Environment environment;
    private final double samplingRate;
    private final boolean alwaysSampleErrors;

    public ZStackSampler() {
        this.environment = TelemetryGlobalProperty.Environment.fromString(TelemetryGlobalProperty.ENVIRONMENT);
        this.samplingRate = TelemetryGlobalProperty.SAMPLING_RATE;
        this.alwaysSampleErrors = TelemetryGlobalProperty.ALWAYS_SAMPLE_ERRORS;

        logger.info(String.format("ZStackSampler initialized: environment=%s, samplingRate=%.2f, alwaysSampleErrors=%s",
                environment, getEffectiveSamplingRate(), alwaysSampleErrors));
    }

    public ZStackSampler(TelemetryGlobalProperty.Environment environment, double samplingRate,
            boolean alwaysSampleErrors) {
        this.environment = environment;
        this.samplingRate = samplingRate;
        this.alwaysSampleErrors = alwaysSampleErrors;
    }

    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks) {

        if (environment.isFullSampling()) {
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
        }

        // First check parent context sampling decision to maintain trace continuity
        Span parentSpan = Span.fromContext(parentContext);
        SpanContext parentSpanContext = parentSpan.getSpanContext();
        
        // If a valid parent Span exists, respect its sampling decision
        if (parentSpanContext.isValid()) {
            if (parentSpanContext.isSampled()) {
                // Parent Span is sampled, continue sampling to maintain trace completeness
                return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
            } else {
                // Parent Span is not sampled, drop to avoid creating orphan spans
                // But if it's an error and alwaysSampleErrors is configured, still record
                if (alwaysSampleErrors) {
                    return SamplingResult.create(SamplingDecision.RECORD_ONLY);
                }
                return SamplingResult.create(SamplingDecision.DROP);
            }
        }

        // No parent context (root span), apply local sampling rate
        double effectiveRate = getEffectiveSamplingRate();

        if (effectiveRate >= 1.0) {
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
        }

        if (effectiveRate <= 0.0) {
            return SamplingResult.create(SamplingDecision.DROP);
        }

        boolean shouldSample = ThreadLocalRandom.current().nextDouble() < effectiveRate;

        if (shouldSample) {
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
        }

        if (alwaysSampleErrors) {
            return SamplingResult.create(SamplingDecision.RECORD_ONLY);
        }

        return SamplingResult.create(SamplingDecision.DROP);
    }

    @Override
    public String getDescription() {
        return String.format("ZStackSampler{environment=%s, rate=%.2f, alwaysSampleErrors=%s}",
                environment, getEffectiveSamplingRate(), alwaysSampleErrors);
    }

    private double getEffectiveSamplingRate() {
        switch (environment) {
            case DEV:
            case TEST:
                return 1.0;
            case STAGING:
                return Math.max(samplingRate, 0.1);
            case PROD:
            default:
                return samplingRate;
        }
    }

    public TelemetryGlobalProperty.Environment getEnvironment() {
        return environment;
    }

    public double getSamplingRate() {
        return samplingRate;
    }

    public boolean isAlwaysSampleErrors() {
        return alwaysSampleErrors;
    }
}
