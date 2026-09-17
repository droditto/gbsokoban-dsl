#include "level.h"
#include "assets.h"
#include "common.h"
#include <gb/gb.h>
#include <string.h>

#include "level_data.h"

uint8_t  level_width, level_height;
uint16_t level_total_cells;
uint8_t  logic_map[MAX_LEVEL_W * MAX_LEVEL_H];
uint8_t *logic_row[MAX_LEVEL_H];

#ifdef FEAT_HAS_GOALS
GroupedPos goals[MAX_GOALS];
uint8_t    num_goals;
#endif

// Bulk-write scratch. Too big for the stack.
static uint8_t bg_row[BKG_MAP_TILES];

// Not fill_bkg_rect: the tile depends on where it falls in the cell.
static void fill_bkg_with_cell(uint8_t cell) {
    uint8_t x, y;
    for (y = 0; y < BKG_MAP_TILES; y++) {
        const uint8_t *tiles = &cell_tiles[cell * TILES_PER_CELL
                                          + (y & (TILES_PER_SIDE - 1)) * TILES_PER_SIDE];
        for (x = 0; x < BKG_MAP_TILES; x++)
            bg_row[x] = tiles[x & (TILES_PER_SIDE - 1)];
        set_bkg_tiles(0, y, BKG_MAP_TILES, 1, bg_row);
    }
}

// Writes a tile-id map to the background, TILES_PER_SIDE hardware rows per pass.
static void render_map_to_bg(const uint8_t *map, uint8_t width, uint8_t height) {
    uint8_t x, y, sub;

    // Whole map, not just this level: the last one would show through.
    fill_bkg_with_cell(BLANK_CELL);
#ifdef FEAT_COLOR
    if (_cpu == CGB_TYPE) {
        VBK_REG = VBK_ATTRIBUTES;
        fill_bkg_rect(0, 0, BKG_MAP_TILES, BKG_MAP_TILES, cell_attrs[BLANK_CELL]);
        VBK_REG = VBK_TILES;
    }
#endif
    for (y = 0; y < height; y++) {
        const uint8_t *src = &map[(uint16_t)y * width];
#if TILES_PER_SIDE == 1
        sub = 0;
        {
#else
        for (sub = 0; sub < TILES_PER_SIDE; sub++) {
#endif
            for (x = 0; x < width; x++)
                memcpy(&bg_row[x * TILES_PER_SIDE],
                       &cell_tiles[TILE_CELL(src[x]) * TILES_PER_CELL
                                   + sub * TILES_PER_SIDE],
                       TILES_PER_SIDE);
            set_bkg_tiles(0, (uint8_t)(y * TILES_PER_SIDE + sub),
                          (uint8_t)(width * TILES_PER_SIDE), 1, bg_row);
#ifdef FEAT_COLOR
            // Same row in VRAM bank 1: one palette index per tile.
            if (_cpu == CGB_TYPE) {
                for (x = 0; x < width; x++)
                    memset(&bg_row[x * TILES_PER_SIDE],
                           cell_attrs[TILE_CELL(src[x])], TILES_PER_SIDE);
                VBK_REG = VBK_ATTRIBUTES;
                set_bkg_tiles(0, (uint8_t)(y * TILES_PER_SIDE + sub),
                              (uint8_t)(width * TILES_PER_SIDE), 1, bg_row);
                VBK_REG = VBK_TILES;
            }
#endif
        }
    }
}

void level_load_tileset(void) {
    set_bkg_data(TILESET_VRAM_BASE, BKG_TILE_COUNT, bkg_tiles);
}

// cell_tiles rows are row-major, the layout set_bkg_tiles expects.
void level_draw_cell(uint8_t grid_x, uint8_t grid_y, uint8_t cell) {
    uint8_t bg_x = (grid_x * TILES_PER_SIDE) & (BKG_MAP_TILES - 1);
    uint8_t bg_y = (grid_y * TILES_PER_SIDE) & (BKG_MAP_TILES - 1);

    set_bkg_tiles(bg_x, bg_y, TILES_PER_SIDE, TILES_PER_SIDE,
                  &cell_tiles[cell * TILES_PER_CELL]);
#ifdef FEAT_COLOR
    if (_cpu == CGB_TYPE) {
        uint8_t attrs[TILES_PER_CELL];
        memset(attrs, cell_attrs[cell], TILES_PER_CELL);
        VBK_REG = VBK_ATTRIBUTES;
        set_bkg_tiles(bg_x, bg_y, TILES_PER_SIDE, TILES_PER_SIDE, attrs);
        VBK_REG = VBK_TILES;
    }
#endif
}

void level_load(uint8_t level_num) {
    const LevelDef *level = &levels[level_clamp(level_num)];

    level_width       = level->width;
    level_height      = level->height;
    level_total_cells = (uint16_t)level_width * level_height;

    memcpy(logic_map, level->map, level_total_cells);

    {
        uint8_t  y;
        uint8_t *p = logic_map;
        for (y = 0; y < level_height; y++) {
            logic_row[y] = p;
            p += level_width;
        }
    }

#ifdef FEAT_HAS_GOALS
    num_goals = level->num_goals;
    if (num_goals > 0)
        memcpy(goals, level->goals, num_goals * sizeof(GroupedPos));
#endif
}

void level_render_full(void) {
    render_map_to_bg(logic_map, level_width, level_height);
}

#ifdef FEAT_LEVEL_SELECT
// Draws the level for the level-select screen without touching live state (logic_map,
// goals, num_boxes, player position).
void level_preview(uint8_t level_num) {
    const LevelDef *level = &levels[level_clamp(level_num)];

    render_map_to_bg(level->map, level->width, level->height);
}
#endif
