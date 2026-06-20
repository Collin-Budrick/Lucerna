#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

// Round 7 shader-side denoise shaping resource.
//
// This resource deliberately preserves the existing public Mojang preview
// contract: one color sampler named InSampler plus texCoord. Because this path
// does not yet receive depth, normal, material id, motion, variance, or history
// inputs, edge awareness is inferred only from local luminance/chroma gradients
// in the supplied lighting texture. Controller validation is still required
// before this resource can be treated as a real denoise milestone.

const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);
const float CENTER_WEIGHT = 0.22;
const float NEAR_WEIGHT = 0.11;
const float DIAGONAL_WEIGHT = 0.075;
const float WIDE_WEIGHT = 0.055;
const float SIGNAL_GAIN = 0.46;
const float SIGNAL_FLOOR = 0.0025;
const vec3 MAX_ADDITIVE_PER_DRAW = vec3(0.030, 0.027, 0.018);

vec2 safeTexelSize() {
    return 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
}

vec4 sourceSample(vec2 uv) {
    return texture(InSampler, clamp(uv, vec2(0.0), vec2(1.0)));
}

float luminance(vec3 color) {
    return dot(max(color, vec3(0.0)), LUMA_WEIGHTS);
}

float chromaSpan(vec3 color) {
    vec3 positiveColor = max(color, vec3(0.0));
    return max(max(positiveColor.r, positiveColor.g), positiveColor.b)
            - min(min(positiveColor.r, positiveColor.g), positiveColor.b);
}

float signalConfidence(vec4 sampleValue) {
    float rgbSignal = luminance(sampleValue.rgb);
    float alphaSignal = clamp(sampleValue.a, 0.0, 1.0);
    return clamp(max(alphaSignal, rgbSignal * 24.0), 0.0, 1.0);
}

void accumulateSample(inout vec4 colorSum, inout float weightSum, vec2 uv, float baseWeight) {
    vec4 neighbor = sourceSample(uv);
    colorSum += neighbor * baseWeight;
    weightSum += baseWeight;
}

vec4 denoisedSample(vec2 uv) {
    vec2 texel = safeTexelSize();
    vec4 center = sourceSample(uv);
    vec4 colorSum = center * CENTER_WEIGHT;
    float weightSum = CENTER_WEIGHT;

    accumulateSample(colorSum, weightSum, uv + vec2(texel.x, 0.0), NEAR_WEIGHT);
    accumulateSample(colorSum, weightSum, uv + vec2(-texel.x, 0.0), NEAR_WEIGHT);
    accumulateSample(colorSum, weightSum, uv + vec2(0.0, texel.y), NEAR_WEIGHT);
    accumulateSample(colorSum, weightSum, uv + vec2(0.0, -texel.y), NEAR_WEIGHT);

    accumulateSample(colorSum, weightSum, uv + texel * vec2(1.0, 1.0), DIAGONAL_WEIGHT);
    accumulateSample(colorSum, weightSum, uv + texel * vec2(-1.0, 1.0), DIAGONAL_WEIGHT);
    accumulateSample(colorSum, weightSum, uv + texel * vec2(1.0, -1.0), DIAGONAL_WEIGHT);
    accumulateSample(colorSum, weightSum, uv + texel * vec2(-1.0, -1.0), DIAGONAL_WEIGHT);

    accumulateSample(colorSum, weightSum, uv + vec2(texel.x * 2.0, 0.0), WIDE_WEIGHT);
    accumulateSample(colorSum, weightSum, uv + vec2(-texel.x * 2.0, 0.0), WIDE_WEIGHT);
    accumulateSample(colorSum, weightSum, uv + vec2(0.0, texel.y * 2.0), WIDE_WEIGHT);
    accumulateSample(colorSum, weightSum, uv + vec2(0.0, -texel.y * 2.0), WIDE_WEIGHT);

    return colorSum / max(weightSum, 0.0001);
}

void main() {
    vec4 center = sourceSample(texCoord);
    vec4 denoised = denoisedSample(texCoord);
    float confidence = clamp(max(signalConfidence(center), signalConfidence(denoised)), 0.0, 1.0);

    vec3 shaped = denoised.rgb;
    shaped = max(shaped, vec3(SIGNAL_FLOOR) * confidence);
    shaped *= SIGNAL_GAIN * confidence;

    fragColor = vec4(min(max(shaped, vec3(0.0)), MAX_ADDITIVE_PER_DRAW), 1.0);
}
