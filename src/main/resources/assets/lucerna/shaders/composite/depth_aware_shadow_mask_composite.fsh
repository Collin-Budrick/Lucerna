#version 330

// Runtime public-Mojang shader for the depth-backed shadow-mask slice.
// Sampler0 = native conservative shadow-mask RGBA8 payload.
// Sampler1 = current Minecraft depth texture view from the live render target.
// The caller must use a SAMPLER0_SAMPLER1 bind layout and only report this as
// depth-sampling evidence when the draw is actually submitted.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord;

out vec4 fragColor;

const float MAX_SHADOW_ALPHA = 0.62;

vec2 clampUv(vec2 uv) {
    return clamp(uv, vec2(0.0), vec2(1.0));
}

float depthAt(vec2 uv) {
    return clamp(texture(Sampler1, clampUv(uv)).r, 0.0, 1.0);
}

float depthSurfaceGate(vec2 uv, float centerDepth) {
    vec2 texel = 1.0 / max(vec2(textureSize(Sampler1, 0)), vec2(1.0));
    float left = depthAt(uv - vec2(texel.x, 0.0));
    float right = depthAt(uv + vec2(texel.x, 0.0));
    float up = depthAt(uv - vec2(0.0, texel.y));
    float down = depthAt(uv + vec2(0.0, texel.y));

    float localSpan = max(max(abs(centerDepth - left), abs(centerDepth - right)),
            max(abs(centerDepth - up), abs(centerDepth - down)));
    float finiteSurface = 1.0 - smoothstep(0.995, 1.0, centerDepth);
    float edgeSafe = 1.0 - smoothstep(0.006, 0.050, localSpan);
    return clamp(max(finiteSurface * edgeSafe, finiteSurface * 0.35), 0.0, 1.0);
}

float borderGuard(vec2 uv) {
    return smoothstep(0.003, 0.020, uv.x)
            * smoothstep(0.003, 0.020, uv.y)
            * (1.0 - smoothstep(0.980, 0.997, uv.x))
            * (1.0 - smoothstep(0.980, 0.997, uv.y));
}

void main() {
    vec4 mask = texture(Sampler0, clampUv(texCoord));
    float coverage = clamp(mask.r, 0.0, 1.0);
    float receiver = clamp(max(mask.g, mask.b), 0.0, 1.0);
    float validity = clamp(max(mask.a, receiver), 0.0, 1.0);

    float centerDepth = depthAt(texCoord);
    float depthGate = depthSurfaceGate(texCoord, centerDepth);
    float receiverGate = smoothstep(0.015, 0.35, max(receiver, coverage * validity));

    float alpha = coverage
            * validity
            * receiverGate
            * depthGate
            * borderGuard(texCoord)
            * MAX_SHADOW_ALPHA;

    fragColor = vec4(0.0, 0.0, 0.0, clamp(alpha, 0.0, MAX_SHADOW_ALPHA));
}
