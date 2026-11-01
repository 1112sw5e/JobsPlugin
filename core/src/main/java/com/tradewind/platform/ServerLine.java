package com.tradewind.platform;

/** Server generations this project declares support for. */
public enum ServerLine {

    PAPER_121("1.21.x", "Paper 1.21.x, Java 21"),
    PAPER_26("26.1.x", "Paper 26.1.x, Java 25"),
    UNKNOWN("неизвестная линейка", "неизвестная линейка");

    private final String label;
    private final String artifactLabel;

    ServerLine(String label, String artifactLabel) {
        this.label = label;
        this.artifactLabel = artifactLabel;
    }

    public String label() {
        return label;
    }

    public String artifactLabel() {
        return artifactLabel;
    }
}

