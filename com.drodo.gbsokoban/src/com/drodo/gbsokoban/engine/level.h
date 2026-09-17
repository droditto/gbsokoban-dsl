#ifndef LEVEL_H
#define LEVEL_H

#include "config.h"
#include "common.h"

extern uint8_t  level_width;
extern uint8_t  level_height;
extern uint16_t level_total_cells;
extern uint8_t  logic_map[];

extern uint8_t *logic_row[MAX_LEVEL_H];

#define LOGIC_TILE(x, y) logic_row[(y)][(x)]

// ====================================================================
// Entities, level data, win conditions
// ====================================================================

#if defined(FEAT_BOXES) || defined(FEAT_HAS_GOALS)
// Cell position + object index. Used for box spawns and goal slots.
typedef struct {
    uint8_t x, y;
    uint8_t group;
} GroupedPos;
#endif

#ifdef FEAT_HAS_GOALS
extern GroupedPos goals[];
extern uint8_t    num_goals;
#endif

typedef struct {
    uint8_t          width;
    uint8_t          height;
    const uint8_t   *map;
    uint8_t          player_x, player_y;
#ifdef FEAT_BOXES
    const GroupedPos *boxes;
    uint8_t          num_boxes;
#endif
#ifdef FEAT_HAS_GOALS
    const GroupedPos *goals;
    uint8_t          num_goals;
#endif
} LevelDef;

extern const LevelDef levels[TOTAL_LEVELS];

// Every loader goes through here, so none indexes past levels[].
static inline uint8_t level_clamp(uint8_t level_num) {
#ifdef FEAT_MULTI_LEVEL
    return level_num >= TOTAL_LEVELS ? 0 : level_num;
#else
    (void)level_num;
    return 0;
#endif
}

typedef struct {
    uint8_t type;  // WIN_ALL_ON / WIN_SOME_ON / WIN_NO_OBJECT / WIN_NO_TILE / WIN_PLAYER_ON
    uint8_t group;
} WinCondition;

extern const WinCondition win_conditions[];
extern const uint8_t      num_win_conditions;

typedef struct {
    uint8_t kind;     // TILE_KIND_*
    uint8_t cell; // index into cell_tiles[]
    uint8_t becomes;  // transform target, or conveyor direction (0 otherwise)
} TileProperties;

extern const TileProperties tile_props[];

#define TILE_IS_PASSABLE(t) (tile_props[t].kind != TILE_KIND_SOLID)
static inline uint8_t TILE_KILLS(uint8_t t) {
    uint8_t kind = tile_props[t].kind;
#ifdef FEAT_FILLABLE
    // Fillable takes the box off the grid too.
    return kind == TILE_KIND_DEADLY || kind == TILE_KIND_FILLABLE;
#else
    return kind == TILE_KIND_DEADLY;
#endif
}
#define TILE_CELL(t)  (tile_props[t].cell)
// Declared unconditionally: an unexpanded macro costs nothing.
#define TILE_IS_ICE(t)       (tile_props[t].kind == TILE_KIND_ICE)
#define TILE_IS_CRUMBLE(t)   (tile_props[t].kind == TILE_KIND_CRUMBLE)
#define TILE_IS_FILLABLE(t)  (tile_props[t].kind == TILE_KIND_FILLABLE)
#define TILE_IS_CONVEYOR(t)  (tile_props[t].kind == TILE_KIND_CONVEYOR)
#define TILE_CONVEYOR_DIR(t) (tile_props[t].becomes)
#define TILE_BECOMES(t)      (tile_props[t].becomes)

void level_load_tileset(void);
void level_load(uint8_t level_num);
void level_render_full(void);
void level_draw_cell(uint8_t grid_x, uint8_t grid_y, uint8_t cell);
#ifdef FEAT_LEVEL_SELECT
void level_preview(uint8_t level_num);
#endif

#endif // LEVEL_H
