#version 330

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D InSampler;

void main() {
    if (texCoord.x < 0.5) {
        discard;
    }

    vec2 uv = clamp(texCoord, vec2(0.0), vec2(1.0));
    vec2 dims = vec2(textureSize(InSampler, 0));
    vec2 texel = 1.0 / max(dims, vec2(1.0));

    vec4 mask = texture(InSampler, uv);
    float shadow = clamp(mask.r, 0.0, 1.0);
    float confidence = clamp(mask.a, 0.0, 1.0);

    float north = clamp(texture(InSampler, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).r, 0.0, 1.0);
    float south = clamp(texture(InSampler, clamp(uv - vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).r, 0.0, 1.0);
    float east = clamp(texture(InSampler, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).r, 0.0, 1.0);
    float west = clamp(texture(InSampler, clamp(uv - vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).r, 0.0, 1.0);

    float localRange = max(max(abs(shadow - north), abs(shadow - south)), max(abs(shadow - east), abs(shadow - west)));
    float maskStructure = smoothstep(0.010, 0.115, localRange);
    float penumbra = smoothstep(0.040, 0.420, shadow) * (1.0 - smoothstep(0.760, 1.0, shadow));
    float core = smoothstep(0.300, 0.880, shadow * confidence);
    float shaped = clamp((penumbra * 0.34 + core * 0.64) * confidence * smoothstep(0.12, 1.0, maskStructure), 0.0, 1.0);

    vec3 shadowInk = mix(vec3(0.012, 0.016, 0.024), vec3(0.038, 0.047, 0.068), core);
    fragColor = vec4(shadowInk * shaped, clamp(shaped * 0.34, 0.0, 0.40));
}
