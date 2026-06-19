#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

const float FINAL_COMPOSITE_GAIN = 3.25;
const float SURFACE_PROJECTION_GAIN = 192.0;
const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

vec4 sourceSample(vec2 uv) {
    return texture(InSampler, clamp(uv, vec2(0.0), vec2(1.0)));
}

vec4 localSurfaceSample(vec2 uv) {
    vec2 texel = 1.0 / max(vec2(textureSize(InSampler, 0)), vec2(1.0));
    vec4 center = sourceSample(uv) * 0.40;
    vec4 cardinal = (
            sourceSample(uv + vec2(texel.x, 0.0))
            + sourceSample(uv + vec2(-texel.x, 0.0))
            + sourceSample(uv + vec2(0.0, texel.y))
            + sourceSample(uv + vec2(0.0, -texel.y))
    ) * 0.12;
    vec4 diagonal = (
            sourceSample(uv + texel * vec2(1.0, 1.0))
            + sourceSample(uv + texel * vec2(-1.0, 1.0))
            + sourceSample(uv + texel * vec2(1.0, -1.0))
            + sourceSample(uv + texel * vec2(-1.0, -1.0))
    ) * 0.03;
    return center + cardinal + diagonal;
}

vec4 frameSurfaceSignal() {
    vec4 signal = vec4(0.0);
    for (int y = 0; y < 5; ++y) {
        for (int x = 0; x < 7; ++x) {
            vec2 probe = (vec2(float(x), float(y)) + vec2(0.5)) / vec2(7.0, 5.0);
            signal = max(signal, localSurfaceSample(probe));
        }
    }
    return signal;
}

float centralWorldSurfaceMask(vec2 uv) {
    float left = smoothstep(0.18, 0.34, uv.x);
    float right = 1.0 - smoothstep(0.72, 0.92, uv.x);
    float top = smoothstep(0.12, 0.28, uv.y);
    float bottom = 1.0 - smoothstep(0.82, 0.96, uv.y);
    float vertical = 0.72 + (1.0 - abs(uv.y - 0.48) * 1.4) * 0.28;
    return clamp(left * right * top * bottom * vertical, 0.0, 1.0);
}

void main() {
    vec4 nativeLighting = localSurfaceSample(texCoord);
    vec4 frameLighting = max(nativeLighting, frameSurfaceSignal());
    float rgbLuma = dot(max(frameLighting.rgb, vec3(0.0)), LUMA_WEIGHTS);
    float signal = clamp(max(frameLighting.a, rgbLuma), 0.0, 1.0);
    float projection = centralWorldSurfaceMask(texCoord);
    vec3 localLight = max(nativeLighting.rgb, vec3(0.0)) * FINAL_COMPOSITE_GAIN * signal;
    vec3 projectedLight = max(frameLighting.rgb, vec3(0.0)) * SURFACE_PROJECTION_GAIN * signal * projection;
    vec3 additiveLight = localLight + projectedLight;
    fragColor = vec4(additiveLight, signal);
}
