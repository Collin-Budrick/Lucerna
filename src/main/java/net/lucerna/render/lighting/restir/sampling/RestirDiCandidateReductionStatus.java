package net.lucerna.render.lighting.restir.sampling;

public enum RestirDiCandidateReductionStatus {
    UNAVAILABLE(false, false, "many-light candidate inputs unavailable"),
    PASSTHROUGH(true, false, "sampling keeps all available candidates"),
    REDUCED(true, true, "sampling reduces many-light candidates"),
    EMPTY(true, false, "sampling inputs are valid but contain no candidates");

    private final boolean metadataAvailable;
    private final boolean reduced;
    private final String label;

    RestirDiCandidateReductionStatus(boolean metadataAvailable, boolean reduced, String label) {
        this.metadataAvailable = metadataAvailable;
        this.reduced = reduced;
        this.label = label;
    }

    public boolean metadataAvailable() {
        return this.metadataAvailable;
    }

    public boolean reduced() {
        return this.reduced;
    }

    public String label() {
        return this.label;
    }
}
