#version 450

// Final composite fragment resource for lucerna.composite.final.
// This is distinct from core/direct_light_preview_*.fsh preview plumbing.
// Descriptor bindings are intentionally semantic until the controller/native
// descriptor tables assign concrete slots for the final composite path.
// LucernaDenoisedDiffuse is a semantic composite input. Its runtime source
// identity may be CPU/readback visual denoise, public-Mojang visual shaping, or
// a future shader-generated lucerna.denoise.diffuse image; this fragment shader
// does not prove which producer created the sampled pixels.

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
const float LOW_RES_BLOCK_REJECT = 0.62;
const float TEXTURE_DETAIL_RESTORE = 0.38;
const float SOURCE_GATED_GI_VISUAL_GAIN = 2.15;
const float CONTACT_SHADOW_VISUAL_GAIN = 1.70;
const float DENOISED_SOFTENING_GAIN = 0.16;
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

float lightingSignalAt(sampler2D source, vec2 uv) {
    return lightingSignal(texture(source, clamp(uv, vec2(0.0), vec2(1.0))));
}

float worldTextureStructure(vec2 uv, vec3 centerColor) {
    vec2 texel = safeTexelSize(LucernaWorldColor);
    vec3 left = texture(LucernaWorldColor, clamp(uv - vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb;
    vec3 right = texture(LucernaWorldColor, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb;
    vec3 up = texture(LucernaWorldColor, clamp(uv - vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb;
    vec3 down = texture(LucernaWorldColor, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb;
    float lumaStructure = abs(luminance(centerColor) - luminance(left))
            + abs(luminance(centerColor) - luminance(right))
            + abs(luminance(centerColor) - luminance(up))
            + abs(luminance(centerColor) - luminance(down));
    float chromaStructure = max(max(length(centerColor - left), length(centerColor - right)),
            max(length(centerColor - up), length(centerColor - down)));
    return clamp(lumaStructure * 1.35 + chromaStructure * 0.55, 0.0, 1.0);
}

vec3 localWorldMean(vec2 uv, vec3 centerColor) {
    vec2 texel = safeTexelSize(LucernaWorldColor);
    vec3 sum = centerColor * 2.0;
    sum += texture(LucernaWorldColor, clamp(uv - vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb;
    sum += texture(LucernaWorldColor, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb;
    sum += texture(LucernaWorldColor, clamp(uv - vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb;
    sum += texture(LucernaWorldColor, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb;
    return sum / 6.0;
}

vec3 localWorldMin(vec2 uv, vec3 centerColor) {
    vec2 texel = safeTexelSize(LucernaWorldColor);
    vec3 minRgb = centerColor;
    minRgb = min(minRgb, texture(LucernaWorldColor, clamp(uv - vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb);
    minRgb = min(minRgb, texture(LucernaWorldColor, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb);
    minRgb = min(minRgb, texture(LucernaWorldColor, clamp(uv - vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb);
    minRgb = min(minRgb, texture(LucernaWorldColor, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb);
    return minRgb;
}

vec3 localWorldMax(vec2 uv, vec3 centerColor) {
    vec2 texel = safeTexelSize(LucernaWorldColor);
    vec3 maxRgb = centerColor;
    maxRgb = max(maxRgb, texture(LucernaWorldColor, clamp(uv - vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb);
    maxRgb = max(maxRgb, texture(LucernaWorldColor, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).rgb);
    maxRgb = max(maxRgb, texture(LucernaWorldColor, clamp(uv - vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb);
    maxRgb = max(maxRgb, texture(LucernaWorldColor, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).rgb);
    return maxRgb;
}

float lowResolutionBlockReject(vec2 uv, vec4 directLighting, vec4 denoisedDiffuse) {
    vec2 directTexel = safeTexelSize(LucernaDirectLighting);
    vec2 diffuseTexel = safeTexelSize(LucernaDenoisedDiffuse);
    float center = max(lightingSignal(directLighting), lightingSignal(denoisedDiffuse));
    float directAxis = (
            lightingSignalAt(LucernaDirectLighting, uv + vec2(directTexel.x, 0.0))
            + lightingSignalAt(LucernaDirectLighting, uv - vec2(directTexel.x, 0.0))
            + lightingSignalAt(LucernaDirectLighting, uv + vec2(0.0, directTexel.y))
            + lightingSignalAt(LucernaDirectLighting, uv - vec2(0.0, directTexel.y))) * 0.25;
    float diffuseAxis = (
            lightingSignalAt(LucernaDenoisedDiffuse, uv + vec2(diffuseTexel.x, 0.0))
            + lightingSignalAt(LucernaDenoisedDiffuse, uv - vec2(diffuseTexel.x, 0.0))
            + lightingSignalAt(LucernaDenoisedDiffuse, uv + vec2(0.0, diffuseTexel.y))
            + lightingSignalAt(LucernaDenoisedDiffuse, uv - vec2(0.0, diffuseTexel.y))) * 0.25;
    float axis = max(directAxis, diffuseAxis);
    float blockyPlateau = smoothstep(0.055, 0.26, max(center, axis))
            * (1.0 - smoothstep(0.010, 0.085, abs(center - axis)));
    return blockyPlateau;
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

float sourceGatedGiMask(vec2 uv, vec4 albedoOpacity, vec4 directLighting, vec4 denoisedDiffuse) {
    float giSignal = lightingSignal(denoisedDiffuse);
    float directSignal = lightingSignal(directLighting);
    float giStructure = lightingStructure(uv, LucernaDenoisedDiffuse, denoisedDiffuse);
    float directStructure = lightingStructure(uv, LucernaDirectLighting, directLighting);
    float colorCue = smoothstep(0.008, 0.105,
            chromaSpan(denoisedDiffuse.rgb) + chromaSpan(albedoOpacity.rgb) * 0.38);
    float signalCue = smoothstep(0.008, 0.28, giSignal + giStructure * 0.62);
    float sourceAnchor = smoothstep(0.012, 0.34,
            max(directSignal, directStructure * 0.72) + giSignal * 0.48);
    float materialAnchor = smoothstep(0.015, 0.22, albedoStructure(uv, albedoOpacity.rgb));
    float flatWashReject = mix(0.22, 1.0,
            smoothstep(0.012, 0.12, giStructure + materialAnchor + colorCue * 0.56));
    return clamp(signalCue * max(sourceAnchor, materialAnchor * 0.62)
            * mix(0.70, 1.0, colorCue)
            * flatWashReject, 0.0, 1.0);
}

float contactShadowMask(vec2 uv, vec4 albedoOpacity, vec4 directLighting, vec4 denoisedDiffuse) {
    float materialEdge = albedoStructure(uv, albedoOpacity.rgb);
    float directSignal = lightingStructure(uv, LucernaDirectLighting, directLighting);
    float giSignal = lightingStructure(uv, LucernaDenoisedDiffuse, denoisedDiffuse);
    float sourceLocality = sourceGatedGiMask(uv, albedoOpacity, directLighting, denoisedDiffuse);
    float occlusionCue = smoothstep(0.022, 0.20, materialEdge + giSignal * 0.34 + sourceLocality * 0.16)
            * (1.0 - smoothstep(0.22, 0.82, directSignal));
    float contactCore = smoothstep(0.040, 0.24, materialEdge)
            * smoothstep(0.012, 0.20, max(giSignal, sourceLocality));
    return clamp((occlusionCue + contactCore * 0.26)
            * smoothstep(0.16, 0.82, albedoOpacity.a)
            * CONTACT_SHADOW_VISUAL_GAIN, 0.0, 0.56);
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
    float receiverDetail = max(albedoStructure(texCoord, albedo),
            worldTextureStructure(texCoord, baseColor));
    float blockReject = lowResolutionBlockReject(texCoord, directLighting, denoisedDiffuse)
            * (1.0 - smoothstep(0.035, 0.22, receiverDetail));
    float receiverLocalMask = surfaceMask * mix(1.0, 1.0 - LOW_RES_BLOCK_REJECT, blockReject);
    float sourceGiMask = sourceGatedGiMask(texCoord, albedoOpacity, directLighting, denoisedDiffuse)
            * receiverLocalMask;
    vec3 directSpill = boundedRadiance(directLighting.rgb, LUCERNA_MAX_DIRECT_RADIANCE)
        * max(LucernaDirectCompositeGain, 0.0)
        * directVisibility;
    vec3 coloredBounceGi = boundedRadiance(denoisedDiffuse.rgb, LUCERNA_MAX_DIFFUSE_RADIANCE)
        * max(LucernaDiffuseCompositeGain, 0.0);
    vec3 giTint = mix(vec3(luminance(coloredBounceGi)), coloredBounceGi,
            0.64 + smoothstep(0.010, 0.16, chromaSpan(coloredBounceGi)) * 0.28);
    vec3 receiverTint = mix(vec3(1.0), normalize(albedo + vec3(0.0001)) * 1.35,
            0.36 + smoothstep(0.020, 0.24, receiverDetail) * 0.14);
    vec3 visualDenoiseContribution = giTint
            * receiverTint
            * SOURCE_GATED_GI_VISUAL_GAIN
            * mix(0.18, 1.0, sourceGiMask);

    vec3 litColor = baseColor * (1.0 - contactShadow * receiverLocalMask)
            + (directSpill + visualDenoiseContribution) * albedo * receiverLocalMask;
    vec3 localMean = localWorldMean(texCoord, baseColor);
    vec3 localDetail = baseColor - localMean;
    float smoothDenoiseMask = sourceGiMask
            * (1.0 - smoothstep(0.050, 0.28, receiverDetail))
            * (1.0 - blockReject * 0.70);
    litColor = mix(litColor, mix(litColor, localMean + visualDenoiseContribution * albedo, 0.42),
            DENOISED_SOFTENING_GAIN * smoothDenoiseMask);
    litColor += localDetail * (TEXTURE_DETAIL_RESTORE
            * smoothstep(0.020, 0.20, receiverDetail)
            * receiverLocalMask);
    vec3 localMin = localWorldMin(texCoord, baseColor);
    vec3 localMax = localWorldMax(texCoord, baseColor);
    vec3 localRange = max(localMax - localMin, vec3(0.010));
    vec3 lowerBound = max(localMin - localRange * 0.30, vec3(0.0));
    vec3 upperBound = min(max(localMax, baseColor)
            + localRange * (0.80 + receiverLocalMask)
            + vec3(0.045 + sourceGiMask * 0.080), vec3(1.0));
    litColor = clamp(litColor, lowerBound, upperBound);
    vec3 finalColor = mix(baseColor, clamp(litColor, vec3(0.0), vec3(1.0)), enabled);

    float overlayAlpha = clamp(debugOverlay.a * LucernaDebugOverlayAlpha, 0.0, 1.0);
    finalColor = mix(finalColor, clamp(debugOverlay.rgb, vec3(0.0), vec3(1.0)), overlayAlpha);

    fragColor = vec4(finalColor, worldColor.a);
}
