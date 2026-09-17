package com.drodo.gbsokoban.generator.plan;

import java.util.List;

/** One 8x8 tile in 2bpp. Identical tiles compare equal, so the allocator shares a VRAM slot. */
public record HardwareTile(List<Integer> bytes) {
}
