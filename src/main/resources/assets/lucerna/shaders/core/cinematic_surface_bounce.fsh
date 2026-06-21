#version 330

in vec2 texCoord;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(91.7, 317.3));
    p += dot(p, p + 43.1);
    return fract(p.x * p.y);
}

float noise(vec2 uv) {
    vec2 i = floor(uv);
    vec2 f = fract(uv);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float ellipse(vec2 uv, vec2 center, vec2 radius, float falloff) {
    vec2 delta = (uv - center) / radius;
    return exp(-dot(delta, delta) * falloff);
}

void main() {
    vec2 uv = texCoord;

    float shoreline = ellipse(uv, vec2(0.40, 0.46), vec2(0.72, 0.10), 2.5);
    float sunlitBank = ellipse(uv, vec2(0.30, 0.34), vec2(0.44, 0.20), 1.7);
    float foliageBounce = ellipse(uv, vec2(0.50, 0.57), vec2(0.62, 0.16), 2.1);
    float waterBounce = ellipse(uv, vec2(0.38, 0.31), vec2(0.70, 0.18), 2.0);
    float foregroundWarmth = ellipse(uv, vec2(0.70, 0.18), vec2(0.46, 0.24), 1.8);
    float detail = mix(0.55, 1.0, noise(vec2(uv.x * 11.0 + uv.y * 2.3, uv.y * 8.0)));

    vec3 warm = vec3(0.96, 0.58, 0.26) * (shoreline * 0.105 + sunlitBank * 0.090 + foregroundWarmth * 0.050);
    vec3 green = vec3(0.24, 0.50, 0.22) * foliageBounce * 0.070;
    vec3 blue = vec3(0.18, 0.36, 0.72) * waterBounce * 0.060;
    vec3 color = (warm + green + blue) * detail;

    float alpha = shoreline * 0.040 + sunlitBank * 0.034 + foliageBounce * 0.030 + waterBounce * 0.026 + foregroundWarmth * 0.020;
    float hudFade = 1.0 - smoothstep(0.84, 0.97, uv.y);
    fragColor = vec4(color, clamp(alpha * hudFade, 0.0, 0.085));
}
