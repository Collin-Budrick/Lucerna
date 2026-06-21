#version 450

// Final composite fragment resource for lucerna.composite.final.
// This is distinct from core/direct_light_preview_*.fsh preview plumbing.
// Descriptor bindings are intentionally semantic until the controller/native
// descriptor tables assign concrete slots for the final composite path.

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D LucernaWorldColor;
uniform sampler2D LucernaAlbedoOpacity;
uniform sampler2D LucernaDirectLighting;
uniform sampler2D LucernaDenoisedDiffuse;
uniform sampler2D LucernaDebugOverlay;

uniform float LucernaDirectCompositeGain;
uniform float LucernaDiffuseCompositeGain;
uniform float LucernaDebugOverlayAlpha;
uniform float LucernaFinalCompositeEnabled;

const float LUCERNA_MAX_DIRECT_RADIANCE = 4.0;
const float LUCERNA_MAX_DIFFUSE_RADIANCE = 2.0;
const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

vec3 boundedRadiance(vec3 value, float limit) {
    return clamp(max(value, vec3(0.0)), vec3(0.0), vec3(limit));
}

vec2 safeTexelSize(sampler2D source) {
    return 1.0 / max(vec2(textureSize(source, 0)), vec2(1.0));
}

float luminance(vec3 color) {
    return dot(max(color, vec3(0.0)), LUMA_WEIGHTS);
}

float chromaSpan(vec3 color) {
    vec3 positiveColor = max(color, vec3(0.0));
    return max(max(positiveColor.r, positiveColor.g), positiveColor.b)
            - min(min(positiveColor.r, positiveColor.g), positiveColor.b);
}

float lightingSignal(vec4 value) {
    return clamp(max(value.a, luminance(value.rgb) * 18.0), 0.0, 1.0);
}

float albedoStructure(vec2 uv, vec3 centerAlbedo) {
    vec2 texel = safeTexelSize(LucernaAlbedoOpacity);
    vec3 left = texture(LucernaAlbedoOpacity, clamp(uv - vec2(texel.x * 2.0, 0.0), vec2(0.0), vec2(1.0))).rgb;
    vec3 right = texture(LucernaAlbedoOpacity, clamp(uv + vec2(texel.x * 2.0, 0.0), vec2(0.0), vec2(1.0))).rgb;
    vec3 up = texture(LucernaAlbedoOpacity, clamp(uv - vec2(0.0, texel.y * 2.0), vec2(0.0), vec2(1.0))).rgb;
    vec3 down = texture(LucernaAlbedoOpacity, clamp(uv + vec2(0.0, texel.y * 2.0), vec2(0.0), vec2(1.0))).rgb;
    float lumaEdge = abs(luminance(right) - luminance(left)) + abs(luminance(down) - luminance(up));
    float chromaEdge = length(max(right, vec3(0.0)) - max(left, vec3(0.0)))
            + length(max(down, vec3(0.0)) - max(up, vec3(0.0)));
    return clamp(chromaSpan(centerAlbedo) * 0.60 + lumaEdge * 1.40 + chromaEdge * 0.28, 0.0, 1.0);
}

float lightingStructure(vec2 uv, sampler2D source, vec4 centerValue) {
    vec2 texel = safeTexelSize(source);
    float left = lightingSignal(texture(source, clamp(uv - vec2(texel.x * 2.0, 0.0), vec2(0.0), vec2(1.0))));
    float right = lightingSignal(texture(source, clamp(uv + vec2(texel.x * 2.0, 0.0), vec2(0.0), vec2(1.0))));
    float up = lightingSignal(texture(source, clamp(uv - vec2(0.0, texel.y * 2.0), vec2(0.0), vec2(1.0))));
    float down = lightingSignal(texture(source, clamp(uv + vec2(0.0, texel.y * 2.0), vec2(0.0), vec2(1.0))));
    float localGradient = abs(right - left) + abs(down - up);
    return clamp(lightingSignal(centerValue) * 0.55 + localGradient * 0.90 + chromaSpan(centerValue.rgb) * 0.35, 0.0, 1.0);
}

float sceneSurfaceMask(vec2 uv, vec4 albedoOpacity, vec4 directLighting, vec4 denoisedDiffuse) {
    float opacityMask = smoothstep(0.08, 0.72, albedoOpacity.a);
    float materialMask = smoothstep(0.018, 0.18, albedoStructure(uv, albedoOpacity.rgb));
    float directMask = lightingStructure(uv, LucernaDirectLighting, directLighting);
    float giMask = lightingStructure(uv, LucernaDenoisedDiffuse, denoisedDiffuse);
    float signalMask = smoothstep(0.015, 0.26, max(directMask, giMask));
    float antiWashout = mix(0.18, 1.0, max(materialMask, signalMask));
    float softEdgeGuard = smoothstep(0.004, 0.035, uv.x)
            * smoothstep(0.004, 0.035, uv.y)
            * (1.0 - smoothstep(0.965, 0.996, uv.x))
            * (1.0 - smoothstep(0.965, 0.996, uv.y));
    return clamp(opacityMask * signalMask * antiWashout * softEdgeGuard, 0.0, 1.0);
}

float contactShadowMask(vec2 uv, vec4 albedoOpacity, vec4 directLighting, vec4 denoisedDiffuse) {
    float materialEdge = albedoStructure(uv, albedoOpacity.rgb);
    float directSignal = lightingStructure(uv, LucernaDirectLighting, directLighting);
    float giSignal = lightingStructure(uv, LucernaDenoisedDiffuse, denoisedDiffuse);
    float occlusionCue = smoothstep(0.030, 0.22, materialEdge + giSignal * 0.28)
            * (1.0 - smoothstep(0.24, 0.86, directSignal));
    return clamp(occlusionCue * smoothstep(0.18, 0.84, albedoOpacity.a), 0.0, 0.34);
}

void main() {
    vec4 worldColor = texture(LucernaWorldColor, texCoord);
    vec4 albedoOpacity = texture(LucernaAlbedoOpacity, texCoord);
    vec4 directLighting = texture(LucernaDirectLighting, texCoord);
    vec4 denoisedDiffuse = texture(LucernaDenoisedDiffuse, texCoord);
    vec4 debugOverlay = texture(LucernaDebugOverlay, texCoord);

    float enabled = clamp(LucernaFinalCompositeEnabled, 0.0, 1.0);
    float surfaceMask = sceneSurfaceMask(texCoord, albedoOpacity, directLighting, denoisedDiffuse);
    float directVisibility = clamp(directLighting.a, 0.0, 1.0);
    float contactShadow = contactShadowMask(texCoord, albedoOpacity, directLighting, denoisedDiffuse);

    vec3 baseColor = clamp(worldColor.rgb, vec3(0.0), vec3(1.0));
    vec3 albedo = clamp(albedoOpacity.rgb, vec3(0.0), vec3(1.0));
    vec3 directSpill = boundedRadiance(directLighting.rgb, LUCERNA_MAX_DIRECT_RADIANCE)
        * max(LucernaDirectCompositeGain, 0.0)
        * directVisibility;
    vec3 coloredBounceGi = boundedRadiance(denoisedDiffuse.rgb, LUCERNA_MAX_DIFFUSE_RADIANCE)
        * max(LucernaDiffuseCompositeGain, 0.0);
    vec3 shaderDenoisedGi = coloredBounceGi * mix(vec3(1.0), normalize(albedo + vec3(0.0001)) * 1.25, 0.34);

    vec3 litColor = baseColor * (1.0 - contactShadow * surfaceMask)
            + (directSpill + shaderDenoisedGi) * albedo * surfaceMask;
    vec3 finalColor = mix(baseColor, clamp(litColor, vec3(0.0), vec3(1.0)), enabled);

    float overlayAlpha = clamp(debugOverlay.a * LucernaDebugOverlayAlpha, 0.0, 1.0);
    finalColor = mix(finalColor, clamp(debugOverlay.rgb, vec3(0.0), vec3(1.0)), overlayAlpha);

    fragColor = vec4(finalColor, worldColor.a);
}
