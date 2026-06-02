package com.drodo.gbsokoban.generator;

import java.util.List;

/** Resolved animation: the frame index list and the mirror-bit mask. */
public record AnimResolution(List<Integer> frames, int mirrorBits) {}
