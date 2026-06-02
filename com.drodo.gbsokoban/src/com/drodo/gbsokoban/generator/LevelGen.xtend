package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.ConveyorTile
import com.drodo.gbsokoban.gBSokoban.CrumbleTile
import com.drodo.gbsokoban.gBSokoban.DeadlyTile
import com.drodo.gbsokoban.gBSokoban.Direction
import com.drodo.gbsokoban.gBSokoban.FillableTile
import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.gBSokoban.IceTile
import com.drodo.gbsokoban.gBSokoban.SolidTile
import com.drodo.gbsokoban.gBSokoban.TileDef
import com.drodo.gbsokoban.util.Feature
import java.nio.file.Files
import java.nio.file.Paths
import java.util.List
import java.util.Set
import javax.imageio.ImageIO

import static extension com.drodo.gbsokoban.generator.GenUtils.*

/** Emits level.h and level.c: tile props, logic map, loader, level data tables. */
class LevelGen {

	def String levelH(Game game, List<LevelParser> levels, Set<Feature> features) {
		val hasDeadly = features.contains(Feature.DEADLY)
		val hasCrumble = features.contains(Feature.CRUMBLE)
		val hasFillable = features.contains(Feature.FILLABLE)
		val hasConveyor = features.contains(Feature.CONVEYOR)
		val hasGoals = features.contains(Feature.HAS_GOALS)
		val hasLevelSelect = features.contains(Feature.LEVEL_SELECT)
		val hasBoxes = features.contains(Feature.BOXES)
        '''
        #ifndef LEVEL_H
        #define LEVEL_H

        #include "common.h"

        // ====================================================================
        // Logic map
        // ====================================================================

        extern uint8_t  level_width;
        extern uint8_t  level_height;
        extern uint16_t level_total_cells;
        extern uint8_t  logic_map[];

        extern uint8_t *logic_row[MAX_LEVEL_H];

        #define LOGIC_TILE(x, y) logic_row[(y)][(x)]

        // ====================================================================
        // Entities, level data, win conditions
        // ====================================================================

        «IF hasBoxes || hasGoals»
        // Cell position + object index. Used for box spawns and goal slots.
        typedef struct {
            uint8_t x, y;
            uint8_t group;
        } GroupedPos;

        «ENDIF»
        «IF hasGoals»
        extern GroupedPos goals[];
        extern uint8_t    num_goals;

        «ENDIF»
        typedef struct {
            uint8_t          width;
            uint8_t          height;
            const uint8_t   *map;
            uint8_t          player_x, player_y;
            «IF hasBoxes»
            const GroupedPos *boxes;
            uint8_t          num_boxes;
            «ENDIF»
            «IF hasGoals»
            const GroupedPos *goals;
            uint8_t          num_goals;
            «ENDIF»
        } LevelDef;

        extern const LevelDef levels[TOTAL_LEVELS];

        typedef struct {
            uint8_t type;  // WIN_ALL_ON / WIN_SOME_ON / WIN_NO_OBJECT / WIN_NO_TILE / WIN_PLAYER_ON
            uint8_t group;
        } WinCondition;

        extern const WinCondition win_conditions[];
        extern const uint8_t      num_win_conditions;

        // ====================================================================
        // Tile properties
        // ====================================================================

        typedef struct {
            uint8_t kind;     // TILE_KIND_*
            uint8_t tile_idx; // metatile to render
            uint8_t becomes;  // crumble/fillable target tile, or conveyor direction (0 otherwise)
        } TileProperties;

        extern const TileProperties tile_props[];

        #define TILE_IS_PASSABLE(t) (tile_props[t].kind != TILE_KIND_SOLID)
        «IF hasDeadly»
        static inline uint8_t TILE_KILLS(uint8_t t) {
            uint8_t kind = tile_props[t].kind;
            return kind == TILE_KIND_DEADLY«IF hasFillable» || kind == TILE_KIND_FILLABLE«ENDIF»;
        }
        «ENDIF»
        #define TILE_TO_METATILE(t) (tile_props[t].tile_idx)
        «IF features.contains(Feature.SLIDING)»
        #define TILE_IS_ICE(t)      (tile_props[t].kind == TILE_KIND_ICE)
        «ENDIF»
        «IF hasCrumble»
        #define TILE_IS_CRUMBLE(t)  (tile_props[t].kind == TILE_KIND_CRUMBLE)
        «ENDIF»
        «IF hasFillable»
        #define TILE_IS_FILLABLE(t) (tile_props[t].kind == TILE_KIND_FILLABLE)
        «ENDIF»
        «IF hasConveyor»
        #define TILE_IS_CONVEYOR(t)  (tile_props[t].kind == TILE_KIND_CONVEYOR)
        #define TILE_CONVEYOR_DIR(t) (tile_props[t].becomes)
        «ENDIF»
        «IF hasCrumble || hasFillable»
        #define TILE_BECOMES(t)     (tile_props[t].becomes)
        «ENDIF»

        // ====================================================================
        // API
        // ====================================================================

        void level_load_tileset(void);
        void level_load(uint8_t level_num);
        void level_render_full(void);
        void level_draw_metatile(uint8_t cell_x, uint8_t cell_y, uint8_t metatile_id);
        «IF hasLevelSelect»
        void level_preview(uint8_t level_num);
        «ENDIF»

        #endif // LEVEL_H
        '''
	}

	/** Width in pixels of the tile sheet PNG, or -1 if it cannot be read. */
	private def int readTileSheetWidthPx(String path) {
		try {
			if (path === null) return -1
			val source = Paths.get(path)
			if (!Files.exists(source)) return -1
			val img = ImageIO.read(source.toFile)
			if (img === null) return -1
			return img.width
		} catch (Exception e) {
			return -1
		}
	}

	def String levelC(Game game, List<LevelParser> parsed, Set<Feature> features) {
		val hasDestroy = features.contains(Feature.DESTROYABLE)
		val hasGoals = features.contains(Feature.HAS_GOALS)
		val hasLevelSelect = features.contains(Feature.LEVEL_SELECT)
		val hasBoxes = features.contains(Feature.BOXES)
		val sheetWidthPx = readTileSheetWidthPx(game.tileSheet)
		val mtPerRow = if (sheetWidthPx > 0) sheetWidthPx / 16 else -1
		val tilesetCols = if (sheetWidthPx > 0) sheetWidthPx / 8 else -1
		val mtCount = game.mtCount
		val tileNames = game.tiles.entries.map["TILE_" + cname(it)]
        '''
        #include "level.h"
        #include "background.h"
        «IF hasBoxes»
        #include "box.h"
        «ENDIF»
        #include "common.h"
        #include "player.h"
        #include <gb/gb.h>
        #include <string.h>

        // ====================================================================
        // Module constants
        // ====================================================================

        «IF mtPerRow > 0»
        #define METATILES_PER_ROW «mtPerRow»
        #define TILESET_COLS      «tilesetCols»
        «ELSE»
        #define METATILES_PER_ROW (background_WIDTH / 2)
        #define TILESET_COLS      background_WIDTH
        «ENDIF»

        // ====================================================================
        // State
        // ====================================================================

        const TileProperties tile_props[] = {
            «FOR i : 0 ..< game.tiles.entries.size SEPARATOR ','»
            [TILE_«cname(game.tiles.entries.get(i))»] = «tileEntry(game.tiles.entries.get(i), game)»
            «ENDFOR»
        };

        uint8_t  level_width, level_height;
        uint16_t level_total_cells;
        uint8_t  logic_map[MAX_LEVEL_W * MAX_LEVEL_H];
        uint8_t *logic_row[MAX_LEVEL_H];

        «IF hasGoals»
        GroupedPos goals[MAX_GOALS];
        uint8_t    num_goals;

        «ENDIF»
        // Metatile -> 4 corner hw-tile indices. Built once at startup.
        static uint8_t metatile_corners[MT_COUNT * 4];

        // ====================================================================
        // Helpers
        // ====================================================================

        static void build_metatile_table(void) {
            «IF mtPerRow > 0»
            «FOR t : 0 ..< mtCount»
            «val col = t % mtPerRow»
            «val row = t / mtPerRow»
            «val tl  = row * 2 * tilesetCols + col * 2»
            metatile_corners[«t * 4»]     = TILESET_VRAM_BASE + background_map[«tl»];
            metatile_corners[«t * 4 + 1»] = TILESET_VRAM_BASE + background_map[«tl + 1»];
            metatile_corners[«t * 4 + 2»] = TILESET_VRAM_BASE + background_map[«tl + tilesetCols»];
            metatile_corners[«t * 4 + 3»] = TILESET_VRAM_BASE + background_map[«tl + tilesetCols + 1»];
            «ENDFOR»
            «ELSE»
            uint8_t t;
            for (t = 0; t < MT_COUNT; t++) {
                uint8_t col = t % METATILES_PER_ROW;
                uint8_t row = t / METATILES_PER_ROW;
                metatile_corners[t * 4 + 0] = TILESET_VRAM_BASE + background_map[(row * 2)     * TILESET_COLS + col * 2];
                metatile_corners[t * 4 + 1] = TILESET_VRAM_BASE + background_map[(row * 2)     * TILESET_COLS + col * 2 + 1];
                metatile_corners[t * 4 + 2] = TILESET_VRAM_BASE + background_map[(row * 2 + 1) * TILESET_COLS + col * 2];
                metatile_corners[t * 4 + 3] = TILESET_VRAM_BASE + background_map[(row * 2 + 1) * TILESET_COLS + col * 2 + 1];
            }
            «ENDIF»
        }

        // Writes a tile-id map to BG, two hw-tile rows at a time.
        static void render_map_to_bg(const uint8_t *map, uint8_t width, uint8_t height) {
            // Bulk-write scratch: a metatile row spans two hw-tile rows.
            // Non-reentrant, so stack-local is safe and keeps these out of WRAM.
            uint8_t row_top[MAX_LEVEL_W * 2];
            uint8_t row_bot[MAX_LEVEL_W * 2];
            uint8_t x, y;
            fill_bkg_rect(0, 0, 32, 32, 0);
            for (y = 0; y < height && y < MAX_LEVEL_H; y++) {
                const uint8_t *row = &map[(uint16_t)y * width];
                for (x = 0; x < width && x < MAX_LEVEL_W; x++) {
                    const uint8_t *corners = &metatile_corners[TILE_TO_METATILE(row[x]) * 4];
                    row_top[x * 2]     = corners[0];
                    row_top[x * 2 + 1] = corners[1];
                    row_bot[x * 2]     = corners[2];
                    row_bot[x * 2 + 1] = corners[3];
                }
                set_bkg_tiles(0, (uint8_t)(y * 2),     (uint8_t)(width * 2), 1, row_top);
                set_bkg_tiles(0, (uint8_t)(y * 2 + 1), (uint8_t)(width * 2), 1, row_bot);
            }
        }

        // ====================================================================
        // Public API
        // ====================================================================

        void level_load_tileset(void) {
            set_bkg_data(TILESET_VRAM_BASE, background_TILE_COUNT, background_tiles);
            build_metatile_table();
        }

        void level_draw_metatile(uint8_t cell_x, uint8_t cell_y, uint8_t metatile_id) {
            uint8_t bg_x = (cell_x * 2) & 31;
            uint8_t bg_y = (cell_y * 2) & 31;
            const uint8_t *corners = &metatile_corners[metatile_id * 4];
            set_bkg_tiles(bg_x, bg_y,     2, 1, &corners[0]);
            set_bkg_tiles(bg_x, bg_y + 1, 2, 1, &corners[2]);
        }

        void level_load(uint8_t level_num) {
            const LevelDef *level;
            «IF hasBoxes»
            uint8_t i;
            «ENDIF»

            «IF features.contains(Feature.MULTI_LEVEL)»
            if (level_num >= TOTAL_LEVELS)
                level_num = 0;
            «ELSE»
            level_num = 0;
            «ENDIF»

            level = &levels[level_num];
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

            «IF hasGoals»
            num_goals = level->num_goals;
            if (num_goals > MAX_GOALS) num_goals = MAX_GOALS;
            if (num_goals > 0)
                memcpy(goals, level->goals, num_goals * sizeof(GroupedPos));

            «ENDIF»
            player_init(level->player_x, level->player_y);

            «IF hasBoxes»
            num_boxes = level->num_boxes < MAX_BOXES ? level->num_boxes : MAX_BOXES;
            for (i = 0; i < num_boxes; i++) {
                uint8_t spawn_x = level->boxes[i].x;
                uint8_t spawn_y = level->boxes[i].y;
                memset(&boxes[i], 0, sizeof(Box));
                boxes[i].grid_x  = spawn_x;
                boxes[i].grid_y  = spawn_y;
                boxes[i].pixel_x = GRID_TO_PIXEL(spawn_x);
                boxes[i].pixel_y = GRID_TO_PIXEL(spawn_y);
                boxes[i].group   = level->boxes[i].group;
            }
            «ENDIF»
        }

        void level_render_full(void) {
            «IF hasBoxes»
            uint8_t i;
            «ENDIF»
            render_map_to_bg(logic_map, level_width, level_height);
            «IF hasBoxes»
            for (i = 0; i < num_boxes; i++) {
                «IF hasDestroy»if (boxes[i].destroyed) continue;«ENDIF»
                box_draw_to_bg(&boxes[i]);
            }
            «ENDIF»
        }

        «IF hasLevelSelect»
        // Draws the level for the level-select screen without touching live
        // state (logic_map, goals, num_boxes, player position).
        void level_preview(uint8_t level_num) {
            const LevelDef *level;
            «IF hasBoxes»
            uint8_t i;
            «ENDIF»

            «IF features.contains(Feature.MULTI_LEVEL)»
            if (level_num >= TOTAL_LEVELS) level_num = 0;
            «ELSE»
            level_num = 0;
            «ENDIF»
            level = &levels[level_num];

            render_map_to_bg(level->map, level->width, level->height);

            «IF hasBoxes»
            for (i = 0; i < level->num_boxes; i++)
                level_draw_metatile(level->boxes[i].x, level->boxes[i].y,
                                    box_type_props[level->boxes[i].group].tile_idx);
            «ENDIF»
        }

        «ENDIF»
        // ====================================================================
        // Level data
        // ====================================================================

        «FOR i : 0 ..< parsed.size»
        static const uint8_t level_«i+1»_map[] = {«parsed.get(i).mapData.map[tileNames.get(it)].join(", ")»};
        «IF hasBoxes && !parsed.get(i).boxes.empty»
        static const GroupedPos level_«i+1»_boxes[] = {«parsed.get(i).boxes.map['''{«x», «y», «group»}'''].join(", ")»};
        «ENDIF»
        «IF hasGoals && !parsed.get(i).goals.empty»
        static const GroupedPos level_«i+1»_goals[] = {«parsed.get(i).goals.map['''{«x», «y», «group»}'''].join(", ")»};
        «ENDIF»

        «ENDFOR»
        const LevelDef levels[] = {
            «FOR i : 0 ..< parsed.size SEPARATOR ','»
            {.width = «parsed.get(i).width», .height = «parsed.get(i).height», .map = level_«i+1»_map, .player_x = «parsed.get(i).playerX», .player_y = «parsed.get(i).playerY»«entityFields(parsed.get(i), i+1, hasBoxes, hasGoals)»}
            «ENDFOR»
        };
        '''
	}

	private def String tileEntry(TileDef tile, Game game) {
		val type = tile.type
		val kind = tileKind(type)
		switch type {
            CrumbleTile:  '''{.kind = «kind», .tile_idx = MT_«cname(tile)», .becomes = «game.tileIndex(type.collapseTile)»}'''
            FillableTile: '''{.kind = «kind», .tile_idx = MT_«cname(tile)», .becomes = «game.tileIndex(type.filledTile)»}'''
            ConveyorTile: '''{.kind = «kind», .tile_idx = MT_«cname(tile)», .becomes = «dirToConst(type.direction)»}'''
            default:      '''{.kind = «kind», .tile_idx = MT_«cname(tile)»}'''
		}
	}

	private def String tileKind(Object type) {
		switch (type) {
			SolidTile: "TILE_KIND_SOLID"
			DeadlyTile: "TILE_KIND_DEADLY"
			IceTile: "TILE_KIND_ICE"
			CrumbleTile: "TILE_KIND_CRUMBLE"
			FillableTile: "TILE_KIND_FILLABLE"
			ConveyorTile: "TILE_KIND_CONVEYOR"
			default: "TILE_KIND_FLOOR"
		}
	}

	private def String dirToConst(Direction dir) {
		switch dir {
			case DOWN: "DIR_DOWN"
			case UP: "DIR_UP"
			case LEFT: "DIR_LEFT"
			default: "DIR_RIGHT"
		}
	}

	private def String entityFields(LevelParser level, int oneBasedIndex, boolean hasBoxes, boolean hasGoals) {
        '''«IF hasBoxes»«IF level.boxes.empty», .boxes = 0, .num_boxes = 0«ELSE», .boxes = level_«oneBasedIndex»_boxes, .num_boxes = «level.boxes.size»«ENDIF»«ENDIF»«IF hasGoals»«IF level.goals.empty», .goals = 0, .num_goals = 0«ELSE», .goals = level_«oneBasedIndex»_goals, .num_goals = «level.goals.size»«ENDIF»«ENDIF»'''
	}
}
