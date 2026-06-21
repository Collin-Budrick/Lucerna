#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float PREVIEW_GAIN = 24.0;
const vec3 MIN_PREVIEW_RADIANCE = vec3(0.45, 0.28, 0.08);

float luma(vec3 color) {
    return dot(max(color, vec3(0.0)), vec3(0.2126, 0.7152, 0.0722));
}

float sourceSurfaceMask(vec2 uv, vec4 sampleValue) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    float center = max(clamp(sampleValue.a, 0.0, 1.0), luma(sampleValue.rgb) * 18.0);
    float xGradient = abs(luma(texture(InSampler, clamp(uv + vec2(texel.x * 2.0, 0.0), vec2(0.0), vec2(1.0))).rgb)
            - luma(texture(InSampler, clamp(uv + vec2(-texel.x * 2.0, 0.0), vec2(0.0), vec2(1.0))).rgb));
    float yGradient = abs(luma(texture(InSampler, clamp(uv + vec2(0.0, texel.y * 2.0), vec2(0.0), vec2(1.0))).rgb)
            - luma(texture(InSampler, clamp(uv + vec2(0.0, -texel.y * 2.0), vec2(0.0), vec2(1.0))).rgb));
    float sourceSupport = smoothstep(0.012, 0.18, center + (xGradient + yGradient) * 0.60);
    float softEdgeGuard = smoothstep(0.004, 0.035, uv.x)
            * smoothstep(0.004, 0.035, uv.y)
            * (1.0 - smoothstep(0.965, 0.996, uv.x))
            * (1.0 - smoothstep(0.965, 0.996, uv.y));
    return clamp(sourceSupport * softEdgeGuard, 0.0, 1.0);
}

void main() {
    vec4 directPreview = texture(InSampler, texCoord);
    float previewMask = clamp(directPreview.a, 0.0, 1.0);
    float surfaceMask = sourceSurfaceMask(texCoord, directPreview);
    vec3 directLight = max(directPreview.rgb, vec3(0.0)) * PREVIEW_GAIN;
    directLight += MIN_PREVIEW_RADIANCE * previewMask;
    fragColor = vec4(directLight * surfaceMask, 1.0);
}
