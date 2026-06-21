#version 330

in vec2 texCoord;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(173.3, 419.1));
    p += dot(p, p + 29.7);
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

float fbm(vec2 uv) {
    float sum = 0.0;
    float amp = 0.55;
    for (int i = 0; i < 4; i++) {
        sum += noise(uv) * amp;
        uv = uv * 2.05 + vec2(7.1, 3.4);
        amp *= 0.48;
    }
    return sum;
}

float radial(vec2 uv, vec2 center, vec2 radius, float falloff) {
    vec2 delta = (uv - center) / radius;
    return exp(-dot(delta, delta) * falloff);
}

void main() {
    vec2 uv = texCoord;
    vec2 sun = vec2(0.118, 0.885);

    float edgeSafety = smoothstep(0.012, 0.060, uv.x) * (1.0 - smoothstep(0.955, 0.995, uv.x));
    float skyMask = smoothstep(0.66, 0.84, uv.y);
    float topFade = 1.0 - smoothstep(0.925, 0.995, uv.y);
    float horizonMask = smoothstep(0.34, 0.50, uv.y) * (1.0 - smoothstep(0.65, 0.82, uv.y));
    float midDepthMask = smoothstep(0.25, 0.40, uv.y) * (1.0 - smoothstep(0.70, 0.88, uv.y));
    float solarCoreGuard = radial(uv, sun, vec2(0.060, 0.052), 2.5);
    float solarScatter = radial(uv, sun, vec2(0.420, 0.220), 2.0) * skyMask * (1.0 - solarCoreGuard * 0.80);
    float directionalHaze = radial(uv, vec2(0.050, 0.780), vec2(0.950, 0.340), 1.8) * skyMask * topFade;
    float skyGradient = skyMask * topFade * (0.18 + 0.50 * smoothstep(0.55, 0.93, uv.y));

    vec2 cloudUv = vec2(uv.x * 2.42 + uv.y * 0.46, uv.y * 3.05);
    float large = fbm(cloudUv + vec2(0.0, 1.7));
    float detail = fbm(cloudUv * 2.85 + vec2(6.3, 0.8));
    float wisps = fbm(vec2(uv.x * 6.4 + uv.y * 1.8, uv.y * 8.2) + vec2(1.1, 5.7));
    float cloudField = large * 0.62 + detail * 0.24 + wisps * 0.14;
    float clouds = smoothstep(0.52, 0.81, cloudField);
    clouds *= skyMask * topFade;
    clouds *= 0.34 + 0.34 * radial(uv, sun, vec2(0.90, 0.36), 1.55);
    clouds *= 1.0 - solarCoreGuard * 0.45;

    float horizonFog = horizonMask * (0.12 + 0.14 * fbm(vec2(uv.x * 3.2, uv.y * 1.4) + vec2(2.2, 8.0)));
    float atmosphericPerspective = midDepthMask * (0.045 + 0.070 * radial(uv, sun, vec2(1.05, 0.34), 1.35));

    float waterBand = smoothstep(0.27, 0.36, uv.y) * (1.0 - smoothstep(0.55, 0.68, uv.y));
    float waterNoise = fbm(vec2(uv.x * 9.8 + uv.y * 2.8, uv.y * 21.0));
    float waterLines = pow(max(0.0, 0.5 + 0.5 * sin((uv.x * 1.20 + uv.y * 0.36) * 94.0)), 5.5);
    float tightWaterLines = pow(max(0.0, 0.5 + 0.5 * sin((uv.x * 1.52 - uv.y * 0.28) * 152.0)), 10.0);
    float waterStreaks = waterBand * smoothstep(0.30, 0.86, waterNoise) * (0.50 + 0.50 * waterLines);
    waterStreaks *= radial(uv, vec2(0.33, 0.415), vec2(0.72, 0.20), 1.85) * 0.26;
    float shoreSparkle = waterBand * tightWaterLines
            * radial(uv, vec2(0.315, 0.455), vec2(0.560, 0.120), 2.0)
            * smoothstep(0.50, 0.90, waterNoise)
            * 0.16;

    vec3 skyBlue = mix(vec3(0.24, 0.36, 0.58), vec3(0.54, 0.68, 0.86), smoothstep(0.48, 0.96, uv.y));
    vec3 sunGold = vec3(0.92, 0.70, 0.42);
    vec3 cloudColor = mix(vec3(0.56, 0.64, 0.76), vec3(0.90, 0.78, 0.58), radial(uv, sun, vec2(0.68, 0.34), 1.65));
    vec3 fogColor = mix(vec3(0.46, 0.56, 0.68), vec3(0.72, 0.64, 0.50), radial(uv, sun, vec2(1.00, 0.42), 1.55));
    vec3 waterColor = mix(vec3(0.24, 0.42, 0.66), vec3(0.80, 0.68, 0.48), radial(uv, vec2(0.34, 0.42), vec2(0.74, 0.20), 1.65));

    vec3 color = vec3(0.0);
    color += skyBlue * skyGradient * 0.035;
    color += cloudColor * clouds * 0.115;
    color += fogColor * horizonFog * 0.035;
    color += vec3(0.56, 0.64, 0.74) * atmosphericPerspective * 0.020;
    color += sunGold * solarScatter * 0.040;
    color += sunGold * directionalHaze * 0.018;
    color += waterColor * waterStreaks * 0.070;
    color += vec3(0.90, 0.82, 0.62) * shoreSparkle * 0.040;
    color = color / (vec3(1.0) + color * 1.25);

    float alpha = skyGradient * 0.006 + clouds * 0.026 + horizonFog * 0.010 + atmosphericPerspective * 0.008
            + solarScatter * 0.010 + directionalHaze * 0.006 + waterStreaks * 0.014 + shoreSparkle * 0.010;
    float screenSafety = 1.0 - smoothstep(0.86, 0.98, uv.y);
    fragColor = vec4(color, clamp(alpha * edgeSafety * screenSafety, 0.0, 0.048));
}
