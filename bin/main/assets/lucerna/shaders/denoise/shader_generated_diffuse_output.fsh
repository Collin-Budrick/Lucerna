#version 330

// Public Mojang fragment resource for lucerna.denoise.diffuse.
//
// This is the runtime-loadable Mojang .fsh form of the shader-generated
// denoise-output slice. The caller must bind an owned Lucerna render target
// named lucerna.denoise.diffuse before drawing this fullscreen pass.
//
// Input sampler contract:
// - InSampler: raw-diffuse-gi-rgba8 radiance, lucerna.lighting.diffuseGi.
//   This is the only accepted input identity for the strict shader-generated
//   denoise-output path. It may contain traced raw-GI evidence from upstream
//   native/Java GI producers, but it must remain labeled as raw-diffuse-gi-rgba8.
//   Native direct-light validation RGBA, lucerna.lighting.direct, direct-light
//   preview textures, or temporary direct-light substitution payloads must not
//   be bound here or counted as shader-denoise evidence.
//
// Keep this shader on the same one-sampler public Mojang contract used by the
// existing preview pipelines. Depth/albedo/history inputs stay documented in
// the stricter GLSL contract until the scheduler owns those bindings.
//
// Output contract:
// - fragColor: denoised diffuse RGBA written into lucerna.denoise.diffuse.
//
// No-overclaim boundary:
// - This is a public Mojang fragment pass into an owned output texture.
// - It is not compute denoise, not a storage-image write, not temporal NRD, and
//   not a final composite draw into the borrowed Minecraft/Sodium color target.
// - It must not draw proof masks, fullscreen wash, fixed rectangles, or
//   focus-window shortcuts. Output shape comes only from raw-diffuse-gi-rgba8
//   local signal, confidence, chroma, and neighborhood contrast.
// - This file being present is not execution proof. Runtime telemetry must not
//   set realDenoiseShaderOutput or realShaderDenoiseOutputReady until the
//   scheduler binds InSampler to raw-diffuse-gi-rgba8 rather than direct-light
//   validation input, binds lucerna.denoise.diffuse
//   as the draw target, submits the pass, and publishes the color-attachment
//   write-to-shader-read handoff for composite/debug consumers.

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);
const float CENTER_WEIGHT = 1.45;
const float AXIS_WEIGHT = 0.23;
const float DIAGONAL_WEIGHT = 0.12;
const float WIDE_WEIGHT = 0.055;
const float RAW_CONTRAST_RESTORE = 0.42;

vec2 clampUv(vec2 uv) {
    return clamp(uv, vec2(0.0), vec2(1.0));
}

vec2 texelSize() {
    return 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
}

vec4 rawDiffuseSample(vec2 uv) {
    return texture(InSampler, clampUv(uv));
}

float luminance(vec3 color) {
    return dot(max(color, vec3(0.0)), LUMA_WEIGHTS);
}

float confidence(vec4 rawValue) {
    return clamp(max(rawValue.a, luminance(rawValue.rgb) * 18.0), 0.0, 1.0);
}

float chromaDistance(vec3 a, vec3 b) {
    return length(max(a, vec3(0.0)) - max(b, vec3(0.0)));
}

float rawCueContinuity(vec4 centerRaw, vec4 sampleRaw) {
    float lumaDelta = abs(luminance(centerRaw.rgb) - luminance(sampleRaw.rgb));
    float chromaDelta = chromaDistance(centerRaw.rgb, sampleRaw.rgb);
    return exp(-(lumaDelta * 5.0 + chromaDelta * 2.2));
}

float rawSignalContinuity(vec4 centerRaw, vec4 sampleRaw) {
    float lumaDelta = abs(luminance(centerRaw.rgb) - luminance(sampleRaw.rgb));
    float confidenceDelta = abs(confidence(centerRaw) - confidence(sampleRaw));
    return exp(-(lumaDelta * 9.0 + confidenceDelta * 1.8));
}

float sampleWeight(vec2 sampleUv, vec4 centerRaw, float baseWeight) {
    vec4 sampleRaw = rawDiffuseSample(sampleUv);
    float cueWeight = rawCueContinuity(centerRaw, sampleRaw);
    float rawWeight = rawSignalContinuity(centerRaw, sampleRaw);
    float confidenceWeight = mix(0.25, 1.0, max(confidence(centerRaw), confidence(sampleRaw)));
    return baseWeight * cueWeight * rawWeight * confidenceWeight;
}

void accumulateSample(inout vec4 sum, inout float weightSum, vec2 sampleUv, vec4 centerRaw, float baseWeight) {
    float weight = sampleWeight(sampleUv, centerRaw, baseWeight);
    vec4 rawValue = rawDiffuseSample(sampleUv);
    sum += vec4(max(rawValue.rgb, vec3(0.0)), clamp(rawValue.a, 0.0, 1.0)) * weight;
    weightSum += weight;
}

float localSignalStructure(vec2 uv, vec4 centerRaw) {
    vec2 texel = texelSize();
    float left = confidence(rawDiffuseSample(uv - vec2(texel.x, 0.0)));
    float right = confidence(rawDiffuseSample(uv + vec2(texel.x, 0.0)));
    float up = confidence(rawDiffuseSample(uv - vec2(0.0, texel.y)));
    float down = confidence(rawDiffuseSample(uv + vec2(0.0, texel.y)));
    float center = confidence(centerRaw);
    return max(max(abs(center - left), abs(center - right)),
            max(abs(center - up), abs(center - down)));
}

float localRawLumaRange(vec2 uv, vec4 centerRaw) {
    vec2 texel = texelSize();
    float center = luminance(centerRaw.rgb);
    float minLuma = center;
    float maxLuma = center;

    float left = luminance(rawDiffuseSample(uv - vec2(texel.x, 0.0)).rgb);
    float right = luminance(rawDiffuseSample(uv + vec2(texel.x, 0.0)).rgb);
    float up = luminance(rawDiffuseSample(uv - vec2(0.0, texel.y)).rgb);
    float down = luminance(rawDiffuseSample(uv + vec2(0.0, texel.y)).rgb);

    minLuma = min(minLuma, min(min(left, right), min(up, down)));
    maxLuma = max(maxLuma, max(max(left, right), max(up, down)));
    return max(maxLuma - minLuma, 0.0);
}

vec3 localRawMean(vec2 uv, vec4 centerRaw) {
    vec2 texel = texelSize();
    vec3 sum = max(centerRaw.rgb, vec3(0.0)) * 2.0;
    sum += max(rawDiffuseSample(uv - vec2(texel.x, 0.0)).rgb, vec3(0.0));
    sum += max(rawDiffuseSample(uv + vec2(texel.x, 0.0)).rgb, vec3(0.0));
    sum += max(rawDiffuseSample(uv - vec2(0.0, texel.y)).rgb, vec3(0.0));
    sum += max(rawDiffuseSample(uv + vec2(0.0, texel.y)).rgb, vec3(0.0));
    return sum / 6.0;
}

vec3 preserveRawLocalContrast(vec2 uv, vec4 centerRaw, vec3 filteredRgb) {
    vec3 centerRgb = max(centerRaw.rgb, vec3(0.0));
    vec3 meanRgb = localRawMean(uv, centerRaw);
    vec3 localDetail = centerRgb - meanRgb;
    float range = localRawLumaRange(uv, centerRaw);
    float structure = smoothstep(0.002, 0.055, range);
    float support = smoothstep(0.001, 0.030, max(confidence(centerRaw), luminance(centerRgb)));
    vec3 contrastRgb = filteredRgb + localDetail * (RAW_CONTRAST_RESTORE * structure * support);
    return max(mix(filteredRgb, contrastRgb, structure * support), vec3(0.0));
}

vec4 denoiseDiffuse(vec2 uv, vec4 centerRaw) {
    vec2 texel = texelSize();
    vec4 sum = vec4(max(centerRaw.rgb, vec3(0.0)), clamp(centerRaw.a, 0.0, 1.0)) * CENTER_WEIGHT;
    float weightSum = CENTER_WEIGHT;

    accumulateSample(sum, weightSum, uv + vec2(texel.x, 0.0), centerRaw, AXIS_WEIGHT);
    accumulateSample(sum, weightSum, uv + vec2(-texel.x, 0.0), centerRaw, AXIS_WEIGHT);
    accumulateSample(sum, weightSum, uv + vec2(0.0, texel.y), centerRaw, AXIS_WEIGHT);
    accumulateSample(sum, weightSum, uv + vec2(0.0, -texel.y), centerRaw, AXIS_WEIGHT);

    accumulateSample(sum, weightSum, uv + texel * vec2(1.0, 1.0), centerRaw, DIAGONAL_WEIGHT);
    accumulateSample(sum, weightSum, uv + texel * vec2(-1.0, 1.0), centerRaw, DIAGONAL_WEIGHT);
    accumulateSample(sum, weightSum, uv + texel * vec2(1.0, -1.0), centerRaw, DIAGONAL_WEIGHT);
    accumulateSample(sum, weightSum, uv + texel * vec2(-1.0, -1.0), centerRaw, DIAGONAL_WEIGHT);

    accumulateSample(sum, weightSum, uv + vec2(texel.x * 2.0, 0.0), centerRaw, WIDE_WEIGHT);
    accumulateSample(sum, weightSum, uv + vec2(-texel.x * 2.0, 0.0), centerRaw, WIDE_WEIGHT);
    accumulateSample(sum, weightSum, uv + vec2(0.0, texel.y * 2.0), centerRaw, WIDE_WEIGHT);
    accumulateSample(sum, weightSum, uv + vec2(0.0, -texel.y * 2.0), centerRaw, WIDE_WEIGHT);

    return sum / max(weightSum, 0.0001);
}

void main() {
    vec2 uv = clampUv(texCoord);
    vec4 centerRaw = rawDiffuseSample(uv);

    vec4 filtered = denoiseDiffuse(uv, centerRaw);
    float signalGate = smoothstep(0.001, 0.035, max(confidence(centerRaw), luminance(filtered.rgb)));
    float surfaceGate = smoothstep(0.001, 0.020, max(confidence(centerRaw), luminance(centerRaw.rgb)));
    float edgeRestore = smoothstep(0.003, 0.045, localSignalStructure(uv, centerRaw));
    vec3 restored = mix(filtered.rgb, max(centerRaw.rgb, vec3(0.0)), edgeRestore * 0.72);
    vec3 contrastPreserved = preserveRawLocalContrast(uv, centerRaw, restored);
    float outputAlpha = clamp(max(filtered.a, confidence(centerRaw)) * signalGate * surfaceGate, 0.0, 1.0);

    fragColor = vec4(max(contrastPreserved, vec3(0.0)) * signalGate * surfaceGate, outputAlpha);
}
