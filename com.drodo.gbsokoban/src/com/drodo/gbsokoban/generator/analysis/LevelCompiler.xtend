package com.drodo.gbsokoban.generator.analysis

import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.gBSokoban.LegendEntry
import com.drodo.gbsokoban.gBSokoban.Level
import com.drodo.gbsokoban.gBSokoban.ObjectDef
import com.drodo.gbsokoban.gBSokoban.ObjectRef
import com.drodo.gbsokoban.gBSokoban.PlayerRef
import com.drodo.gbsokoban.gBSokoban.TileDef
import com.drodo.gbsokoban.generator.plan.GroupedPos
import com.drodo.gbsokoban.generator.plan.LevelPlan
import com.drodo.gbsokoban.model.SymbolPool
import java.util.IdentityHashMap
import java.util.LinkedHashMap
import java.util.List
import java.util.Map

class LevelCompiler {

	val List<TileDef> tiles
	val Map<TileDef, ObjectDef> goalMap
	val Map<TileDef, Integer> indexOfTile
	val Map<ObjectDef, Integer> indexOfObject
	val Map<String, Integer> tileIndexBySymbol
	val Map<String, LegendEntry> legendBySymbol
	val int outsideTile

	new(Game game, Map<TileDef, ObjectDef> goalMap) {
		this.tiles = game.tiles
		this.goalMap = goalMap
		this.outsideTile = outsideIndex(game)

		this.indexOfTile = new IdentityHashMap<TileDef, Integer>
		for (var i = 0; i < tiles.size; i++) indexOfTile.put(tiles.get(i), i)
		this.indexOfObject = new IdentityHashMap<ObjectDef, Integer>
		for (var i = 0; i < game.objects.size; i++) indexOfObject.put(game.objects.get(i), i)

		this.tileIndexBySymbol = new LinkedHashMap<String, Integer>
		for (tile : tiles) tileIndexBySymbol.put(tile.symbol, indexOfTile.get(tile))
		this.legendBySymbol = new LinkedHashMap<String, LegendEntry>
		for (entry : game.legend) legendBySymbol.put(entry.symbol, entry)
	}

	def LevelPlan compile(Level level) {
		val map = newArrayList
		val boxes = newArrayList
		val goals = newArrayList
		var playerX = -1
		var playerY = -1
		val height = level.rows.size
		val width = level.rows.map[length].max

		for (var y = 0; y < height; y++) {
			val row = level.rows.get(y)
			for (var x = 0; x < width; x++) {
				val beyondRow = x >= row.length
				val symbol = if (beyondRow) SymbolPool.PADDING_SYMBOL else String.valueOf(row.charAt(x))
				var int tile = outsideTile
				val legend = legendBySymbol.get(symbol)
				if (legend !== null) {
					tile = indexOfTile.get(legend.tile)
					if (legend.subject instanceof PlayerRef) {
						playerX = x; playerY = y
					} else if (legend.subject instanceof ObjectRef) {
						val object = (legend.subject as ObjectRef).object
						boxes.add(new GroupedPos(x, y, indexOfObject.get(object)))
					}
				} else if (tileIndexBySymbol.containsKey(symbol)) {
					tile = tileIndexBySymbol.get(symbol)
				} else if (!beyondRow) {
					throw new IllegalStateException(
						"Level symbol '" + symbol + "' at (" + x + "," + y + ") is not a tile and has no legend entry")
				}

				map.add(tile)

				val goalObject = if (tile == outsideTile) null else goalMap.get(tiles.get(tile))
				if (goalObject !== null)
					goals.add(new GroupedPos(x, y, indexOfObject.get(goalObject)))
			}
		}

		new LevelPlan(width, height, map, boxes, goals, playerX, playerY)
	}

	static def int outsideIndex(Game game) {
		game.tiles.size
	}
}
