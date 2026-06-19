package net.lucerna.render.lighting.direct;

public enum DirectCelestialLightSource {
    SUN("sun"),
    MOON("moon");

    private final String wireName;

    DirectCelestialLightSource(String wireName) {
        this.wireName = wireName;
    }

    public String wireName() {
        return this.wireName;
    }
}
