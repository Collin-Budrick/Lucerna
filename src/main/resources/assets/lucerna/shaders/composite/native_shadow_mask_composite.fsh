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

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float MAX_SHADOW_ALPHA = 0.58;
const float EDGE_SAMPLE_RADIUS = 1.5;

vec4 maskSample(vec2 uv) {
    return texture(InSampler, clamp(uv, vec2(0.0), vec2(1.0)));
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

float borderGuard(vec2 uv) {
    return smoothstep(0.002, 0.018, uv.x)
            * smoothstep(0.002, 0.018, uv.y)
            * (1.0 - smoothstep(0.982, 0.998, uv.x))
            * (1.0 - smoothstep(0.982, 0.998, uv.y));
}

float conservativeShadowAlpha(vec2 uv, vec4 payload) {
    float coverage = clamp(payload.r, 0.0, 1.0);
    float receiverSupport = clamp(max(payload.g, payload.b), 0.0, 1.0);
    float validity = clamp(max(payload.a, receiverSupport), 0.0, 1.0);
    float structure = localMaskStructure(uv);

    // If the native payload does not mark receivers explicitly, require local
    // mask structure so a flat full-screen texture cannot become shadow proof.
    float receiverGate = max(receiverSupport, smoothstep(0.010, 0.18, structure));
    float coverageGate = smoothstep(0.035, 0.72, coverage * validity);
    float contactSupport = smoothstep(0.020, 0.42, max(receiverSupport, structure * 1.6));
    float edgeSafe = borderGuard(uv);

    return clamp(coverageGate * receiverGate * contactSupport * edgeSafe * MAX_SHADOW_ALPHA, 0.0, MAX_SHADOW_ALPHA);
}

void main() {
    vec4 payload = maskSample(texCoord);
    float shadowAlpha = conservativeShadowAlpha(texCoord, payload);
    fragColor = vec4(0.0, 0.0, 0.0, shadowAlpha);
}
