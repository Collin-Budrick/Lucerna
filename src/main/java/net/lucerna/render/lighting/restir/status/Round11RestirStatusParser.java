package net.lucerna.render.lighting.restir.status;

public final class Round11RestirStatusParser {
    private Round11RestirStatusParser() {
    }

    public static Round11RestirExecutionStatus parse(String nativeStatus) {
        return Round11RestirExecutionStatus.fromNativeStatus(nativeStatus);
    }
}
