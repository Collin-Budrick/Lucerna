#version 330

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    if (texCoord.x < 0.5) {
        discard;
    }

    vec2 uv = clamp(texCoord, vec2(0.0), vec2(1.0));
    vec2 dims = vec2(textureSize(Sampler0, 0));
    vec2 texel = 1.0 / max(dims, vec2(1.0));

    vec4 mask = texture(Sampler0, uv);
    float depth = texture(Sampler1, uv).r;
    float finiteSurface = 1.0 - smoothstep(0.995, 1.0, clamp(depth, 0.0, 1.0));
    float coverage = clamp(mask.r, 0.0, 1.0);
    float receiver = clamp(max(mask.g, mask.b), 0.0, 1.0);
    float validity = clamp(max(mask.a, receiver), 0.0, 1.0);

    float coverageN = clamp(texture(Sampler0, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).r, 0.0, 1.0);
    float coverageS = clamp(texture(Sampler0, clamp(uv - vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).r, 0.0, 1.0);
    float coverageE = clamp(texture(Sampler0, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).r, 0.0, 1.0);
    float coverageW = clamp(texture(Sampler0, clamp(uv - vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).r, 0.0, 1.0);
    float maskRange = max(max(abs(coverage - coverageN), abs(coverage - coverageS)), max(abs(coverage - coverageE), abs(coverage - coverageW)));

    float depthN = texture(Sampler1, clamp(uv + vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).r;
    float depthS = texture(Sampler1, clamp(uv - vec2(0.0, texel.y), vec2(0.0), vec2(1.0))).r;
    float depthE = texture(Sampler1, clamp(uv + vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).r;
    float depthW = texture(Sampler1, clamp(uv - vec2(texel.x, 0.0), vec2(0.0), vec2(1.0))).r;
    float depthRange = max(max(abs(depth - depthN), abs(depth - depthS)), max(abs(depth - depthE), abs(depth - depthW)));

    float receiverGate = smoothstep(0.035, 0.340, receiver);
    float maskStructure = smoothstep(0.008, 0.110, maskRange);
    float geometryStructure = smoothstep(0.00010, 0.00650, depthRange) * finiteSurface;
    float structure = max(maskStructure, geometryStructure * 0.80);
    float contactStructure = smoothstep(0.16, 1.0, structure);
    float body = smoothstep(0.120, 0.720, coverage * validity);
    float alpha = body
            * receiverGate
            * contactStructure
            * mix(0.20, 1.0, finiteSurface)
            * 0.36;
    vec3 shadowColor = mix(vec3(0.0), vec3(0.010, 0.013, 0.020), structure);
    fragColor = vec4(shadowColor, clamp(alpha, 0.0, 0.42));
}
