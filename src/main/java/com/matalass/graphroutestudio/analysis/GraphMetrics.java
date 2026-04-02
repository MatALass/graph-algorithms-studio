package com.matalass.graphroutestudio.analysis;

public record GraphMetrics(
        int nodeCount,
        int edgeCount,
        boolean directed,
        boolean weighted,
        boolean connected,
        boolean hasNegativeWeight,
        double density,
        double averageDegree,
        double totalWeight
) {
}
