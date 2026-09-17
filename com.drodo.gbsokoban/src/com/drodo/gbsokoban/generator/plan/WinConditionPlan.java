package com.drodo.gbsokoban.generator.plan;

import com.drodo.gbsokoban.util.WinKind;

public record WinConditionPlan(WinKind kind, int group) {
}
