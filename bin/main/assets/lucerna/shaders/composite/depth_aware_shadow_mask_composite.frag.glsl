#version 450

// Depth-aware shadow-mask composite resource for lucerna.composite.final.
//
// This is the shader-resource form of the native conservative shadow-mask
// consumer. It is not wired by the current public Mojang single InSampler path.
// The scheduler must bind all semantic samplers below before this resource can
// be runtime evidence. If CurrentDepthSampler is not bound, this shader must
// contribute transparent output rather than falling back to screen-space paint.
//
// Required bindings:
// - ShadowMaskSampler: lucerna.lighting.nativeConservativeShadowMask.
// - CurrentDepthSampler: lucerna.gbuffer.depth for current receiver depth.
// - CurrentNormalRoughnessSampler: lucerna.gbuffer.normalRoughness.
// - SourceLightingSampler: lucerna.lighting.direct or resolved lighting support.
//
// Output:
// - Black source-over alpha for receiver-supported conservative darkening only.
//   This resource does not draw fixed blobs, fog, bloom, debug colors, proof
//   markers, or shadows derived from UV coordinates alone.

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D ShadowMaskSampler;
uniform sampler2D CurrentDepthSampler;
uniform sampler2D CurrentNormalRoughnessSampler;
uniform sampler2D SourceLightingSampler;

uniform float LucernaShadowCompositeEnabled;
uniform float LucernaDepthBindingReady;
uniform float LucernaNormalBindingReady;

const float MAX_SHADOW_ALPHA = 0.62;
const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

vec2 texelSize(sampler2D source) {
    return 1.0 / max(vec2(textureSize(source, 0)), vec2(1.0));
}

vec2 clampUv(vec2 uv) {
    return clamp(uv, vec2(0.0), vec2(1.0));
}

float luminance(vec3 color) {
    return dot(max(color, vec3(0.0)), LUMA_WEIGHTS);
}

float borderGuard(vec2 uv) {
    return smoothstep(0.002, 0.018, uv.x)
            * smoothstep(0.002, 0.018, uv.y)
            * (1.0 - smoothstep(0.982, 0.998, uv.x))
            * (1.0 - smoothstep(0.982, 0.998, uv.y));
}

float depthAt(vec2 uv) {
    return clamp(texture(CurrentDepthSampler, clampUv(uv)).r, 0.0, 1.0);
}

float depthReceiverGate(vec2 uv, float centerDepth) {
    vec2 texel = texelSize(CurrentDepthSampler);
    float left = depthAt(uv - vec2(texel.x, 0.0));
    float right = depthAt(uv + vec2(texel.x, 0.0));
    float up = depthAt(uv - vec2(0.0, texel.y));
    float down = depthAt(uv + vec2(0.0, texel.y));

    float localSpan = max(max(abs(centerDepth - left), abs(centerDepth - right)),
            max(abs(centerDepth - up), abs(centerDepth - down)));
    float finiteSurface = 1.0 - smoothstep(0.995, 1.0, centerDepth);
    float edgeSafe = 1.0 - smoothstep(0.004, 0.045, localSpan);
    return clamp(finiteSurface * edgeSafe, 0.0, 1.0);
}

vec3 decodedNormal(vec2 uv) {
    vec3 packed = texture(CurrentNormalRoughnessSampler, clampUv(uv)).xyz;
    vec3 normal = packed * 2.0 - vec3(1.0);
    return normalize(mix(vec3(0.0, 0.0, 1.0), normal, step(0.001, dot(normal, normal))));
}

float normalReceiverGate(vec2 uv) {
    if (LucernaNormalBindingReady < 0.5) {
        return 1.0;
    }

    vec2 texel = texelSize(CurrentNormalRoughnessSampler);
    vec3 center = decodedNormal(uv);
    vec3 left = decodedNormal(uv - vec2(texel.x, 0.0));
    vec3 right = decodedNormal(uv + vec2(texel.x, 0.0));
    vec3 up = decodedNormal(uv - vec2(0.0, texel.y));
    vec3 down = decodedNormal(uv + vec2(0.0, texel.y));

    float agreement = min(min(dot(center, left), dot(center, right)),
            min(dot(center, up), dot(center, down)));
    return smoothstep(0.42, 0.96, clamp(agreement, 0.0, 1.0));
}

float maskStructure(vec2 uv, vec4 centerMask) {
    vec2 texel = texelSize(ShadowMaskSampler);
    vec4 left = texture(ShadowMaskSampler, clampUv(uv - vec2(texel.x, 0.0)));
    vec4 right = texture(ShadowMaskSampler, clampUv(uv + vec2(texel.x, 0.0)));
    vec4 up = texture(ShadowMaskSampler, clampUv(uv - vec2(0.0, texel.y)));
    vec4 down = texture(ShadowMaskSampler, clampUv(uv + vec2(0.0, texel.y)));

    float coverageEdge = abs(right.r - left.r) + abs(down.r - up.r);
    float receiverEdge = abs(max(right.g, right.b) - max(left.g, left.b))
            + abs(max(down.g, down.b) - max(up.g, up.b));
    float centerSupport = max(centerMask.g, centerMask.b);
    return clamp(centerSupport * 0.60 + coverageEdge * 0.45 + receiverEdge * 0.90, 0.0, 1.0);
}

float lightingReceiverGate(vec2 uv) {
    vec4 lighting = texture(SourceLightingSampler, clampUv(uv));
    float signal = luminance(lighting.rgb) + lighting.a * 0.25;
    return smoothstep(0.010, 0.18, signal);
}

void main() {
    float enabled = clamp(LucernaShadowCompositeEnabled, 0.0, 1.0);

    if (enabled <= 0.0 || LucernaDepthBindingReady < 0.5) {
        fragColor = vec4(0.0);
        return;
    }

    vec4 mask = texture(ShadowMaskSampler, clampUv(texCoord));
    float coverage = clamp(mask.r, 0.0, 1.0);
    float receiverSupport = clamp(max(mask.g, mask.b), 0.0, 1.0);
    float validity = clamp(max(mask.a, receiverSupport), 0.0, 1.0);

    float centerDepth = depthAt(texCoord);
    float depthGate = depthReceiverGate(texCoord, centerDepth);
    float normalGate = normalReceiverGate(texCoord);
    float structureGate = smoothstep(0.018, 0.42, maskStructure(texCoord, mask));
    float lightingGate = lightingReceiverGate(texCoord);
    float receiverGate = max(receiverSupport, structureGate * 0.72);

    float alpha = coverage
            * validity
            * receiverGate
            * depthGate
            * normalGate
            * lightingGate
            * borderGuard(texCoord)
            * MAX_SHADOW_ALPHA;

    fragColor = vec4(0.0, 0.0, 0.0, clamp(alpha, 0.0, MAX_SHADOW_ALPHA));
}
