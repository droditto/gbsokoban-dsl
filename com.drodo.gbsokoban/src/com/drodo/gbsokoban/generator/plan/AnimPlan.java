package com.drodo.gbsokoban.generator.plan;

import java.util.List;

public record AnimPlan(String name, List<List<String>> frames, List<Integer> mirrors) {
}
