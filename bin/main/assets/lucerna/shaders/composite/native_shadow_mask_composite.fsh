#version 330

// Public Mojang fragment resource for consuming a native conservative
// shadow-map/mask payload during final world-color composition.
//
// Input contract for InSampler:
// - r: native conservative shadow coverage for a receiver pixel, 0..1.
// - g: receiver-support mask from world/depth/material classification, 0..1.
// - b: optional contact/edge confidence for receiver-local stabilization, 0..1.
// - a: payload validity or frame confidence, 0..1.
//
// This shader consumes the native payload only. It does not generate a shadow
// map, does not path trace, does not perform screen-space decal proof, does not
// denoise, and does not add bloom, fog, vignette, focus-window, or proof-marker
// behavior. The caller should blend the output over the borrowed world target
// before HUD composition; black source-over alpha acts as conservative
// darkening for receiver regions encoded by the native mask.
//
// Consumption readiness is supplied by DirectionalShadowMapOutputPayload:
// readyForFinalCompositeConsumption=true means dimensions, byte count, nonzero
// displayable mask data, native sample/caster/receiver counts, depth coverage,
// and checksum are present. It still means native conservative CPU shadow-mask
// consumption, not GPU shadow-map generation or hardware RT.

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float MAX_SHADOW_ALPHA = 0.58;
const float EDGE_SAMPLE_RADIUS = 1.5;
const float SOFT_SHADOW_RADIUS = 1.25;
const float MIN_PAYLOAD_COVERAGE_FOR_CONSUMPTION = 0.035;
const float MIN_RECEIVER_STRUCTURE_FOR_CONSUMPTION = 0.010;

vec4 maskSample(vec2 uv) {
    return texture(InSampler, clamp(uv, vec2(0.0), vec2(1.0)));
}

float receiverSupportOf(vec4 payload) {
    return clamp(max(payload.g, payload.b), 0.0, 1.0);
}

float spatialWeight(int x, int y) {
    int distance = abs(x) + abs(y);
    if (distance == 0) {
        return 1.00;
    }
    if (distance == 1) {
        return 0.72;
    }
    return 0.46;
}

vec4 receiverTiedSoftMask(vec2 uv, vec4 centerPayload) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    float centerReceiver = receiverSupportOf(centerPayload);
    float centerCoverage = clamp(centerPayload.r, 0.0, 1.0);

    float weightedCoverage = 0.0;
    float weightedReceiver = 0.0;
    float weightedEdge = 0.0;
    float weightedValidity = 0.0;
    float totalWeight = 0.0;

    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            vec2 offset = vec2(float(x), float(y)) * texel * SOFT_SHADOW_RADIUS;
            vec4 samplePayload = maskSample(uv + offset);
            float sampleCoverage = clamp(samplePayload.r, 0.0, 1.0);
            float sampleReceiver = receiverSupportOf(samplePayload);
            float sampleValidity = clamp(max(samplePayload.a, sampleReceiver), 0.0, 1.0);

            float receiverSimilarity = 1.0 - smoothstep(0.10, 0.55,
                    abs(sampleReceiver - centerReceiver));
            float coverageSimilarity = 1.0 - smoothstep(0.20, 0.80,
                    abs(sampleCoverage - centerCoverage));
            float supportGate = max(sampleReceiver, centerReceiver);
            float coveragePresence = smoothstep(MIN_PAYLOAD_COVERAGE_FOR_CONSUMPTION, 0.35,
                    max(sampleCoverage, centerCoverage)) * 0.35;
            float weight = spatialWeight(x, y)
                    * mix(0.38, 1.0, receiverSimilarity)
                    * mix(0.68, 1.0, coverageSimilarity)
                    * smoothstep(0.010, 0.18,
                            max(max(supportGate, sampleValidity), coveragePresence));

            weightedCoverage += sampleCoverage * sampleValidity * weight;
            weightedReceiver += sampleReceiver * weight;
            weightedEdge += clamp(samplePayload.b, 0.0, 1.0) * weight;
            weightedValidity += sampleValidity * weight;
            totalWeight += weight;
        }
    }

    if (totalWeight <= 0.0001) {
        return vec4(0.0);
    }

    return vec4(weightedCoverage / totalWeight,
            weightedReceiver / totalWeight,
            weightedEdge / totalWeight,
            weightedValidity / totalWeight);
}

float localMaskStructure(vec2 uv) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    vec2 dx = vec2(texel.x * EDGE_SAMPLE_RADIUS, 0.0);
    vec2 dy = vec2(0.0, texel.y * EDGE_SAMPLE_RADIUS);

    vec4 left = maskSample(uv - dx);
    vec4 right = maskSample(uv + dx);
    vec4 up = maskSample(uv - dy);
    vec4 down = maskSample(uv + dy);

    float coverageEdge = abs(right.r - left.r) + abs(down.r - up.r);
    float receiverEdge = abs(max(right.g, right.b) - max(left.g, left.b))
            + abs(max(down.g, down.b) - max(up.g, up.b));
    return clamp(coverageEdge * 0.75 + receiverEdge, 0.0, 1.0);
}

float receiverLocalityGate(vec2 uv, vec4 payload) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    float centerCoverage = clamp(payload.r, 0.0, 1.0);
    float centerReceiver = receiverSupportOf(payload);
    float leftCoverage = clamp(maskSample(uv - vec2(texel.x * 2.0, 0.0)).r, 0.0, 1.0);
    float rightCoverage = clamp(maskSample(uv + vec2(texel.x * 2.0, 0.0)).r, 0.0, 1.0);
    float upCoverage = clamp(maskSample(uv - vec2(0.0, texel.y * 2.0)).r, 0.0, 1.0);
    float downCoverage = clamp(maskSample(uv + vec2(0.0, texel.y * 2.0)).r, 0.0, 1.0);
    float coverageSpan = max(max(abs(centerCoverage - leftCoverage), abs(centerCoverage - rightCoverage)),
            max(abs(centerCoverage - upCoverage), abs(centerCoverage - downCoverage)));
    float localStructure = localMaskStructure(uv);
    float broadFlatMask = smoothstep(0.10, 0.46, centerCoverage)
            * (1.0 - smoothstep(0.010, 0.075, coverageSpan + localStructure))
            * (1.0 - smoothstep(0.045, 0.24, centerReceiver));
    float receiverAnchor = smoothstep(0.018, 0.20, max(centerReceiver, localStructure));
    return clamp(mix(1.0 - broadFlatMask * 0.78, 1.0, receiverAnchor), 0.0, 1.0);
}

float borderGuard(vec2 uv) {
    return smoothstep(0.002, 0.018, uv.x)
            * smoothstep(0.002, 0.018, uv.y)
            * (1.0 - smoothstep(0.982, 0.998, uv.x))
            * (1.0 - smoothstep(0.982, 0.998, uv.y));
}

float conservativeShadowAlpha(vec2 uv, vec4 payload) {
    vec4 softPayload = receiverTiedSoftMask(uv, payload);
    float coverage = clamp(softPayload.r, 0.0, 1.0);
    float receiverSupport = receiverSupportOf(softPayload);
    float validity = clamp(max(softPayload.a, receiverSupport), 0.0, 1.0);
    float structure = localMaskStructure(uv);
    float edgeConfidence = clamp(max(softPayload.b, payload.b), 0.0, 1.0);

    // If the native payload does not mark receivers explicitly, require local
    // mask structure so a flat full-screen texture cannot become shadow proof.
    float receiverGate = max(receiverSupport, smoothstep(0.010, 0.18, structure));
    float coverageGate = smoothstep(MIN_PAYLOAD_COVERAGE_FOR_CONSUMPTION, 0.72, coverage * validity);
    float contactSupport = smoothstep(0.020, 0.42,
            max(receiverSupport, max(structure * 1.6, edgeConfidence * 0.65)));
    float structuredReceiverGate = smoothstep(MIN_RECEIVER_STRUCTURE_FOR_CONSUMPTION, 0.18, structure);
    float softFalloffShape = mix(0.86, 1.08, smoothstep(0.05, 0.45,
            max(receiverSupport, edgeConfidence)));
    float edgeSafe = borderGuard(uv);
    float receiverLocality = receiverLocalityGate(uv, payload);

    return clamp(coverageGate
            * max(receiverGate, structuredReceiverGate)
            * contactSupport
            * softFalloffShape
            * receiverLocality
            * edgeSafe
            * MAX_SHADOW_ALPHA, 0.0, MAX_SHADOW_ALPHA);
}

void main() {
    vec4 payload = maskSample(texCoord);
    float shadowAlpha = conservativeShadowAlpha(texCoord, payload);
    fragColor = vec4(0.0, 0.0, 0.0, shadowAlpha);
}
