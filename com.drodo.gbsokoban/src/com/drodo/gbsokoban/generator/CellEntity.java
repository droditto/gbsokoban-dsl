package com.drodo.gbsokoban.generator;

/** Cell coordinates plus the group index of the entity occupying that cell. */
public record CellEntity(int x, int y, int group) {}
