package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.gBSokoban.Level
import com.drodo.gbsokoban.gBSokoban.LegendEntry
import com.drodo.gbsokoban.gBSokoban.ObjectDef
import com.drodo.gbsokoban.gBSokoban.ObjectRef
import com.drodo.gbsokoban.gBSokoban.PlayerRef
import com.drodo.gbsokoban.gBSokoban.TileDef
import java.util.ArrayList
import java.util.IdentityHashMap
import java.util.LinkedHashMap
import java.util.List
import java.util.Map

import static extension com.drodo.gbsokoban.util.ModelHelpers.*

/**
 * Parses a level's ASCII rows into mapData, box/goal positions, and player
 * start. Character resolution order: legend, then object, then tile chars.
 * A cell is also a goal if its tile appears in an AllOn/SomeOn win condition.
 */
class LevelParser {

	public val List<Integer> mapData = new ArrayList
	public val List<CellEntity> boxes = new ArrayList
	public val List<CellEntity> goals = new ArrayList
	public var int playerX = -1
	public var int playerY = -1
	public var int width
	public var int height

	new(Game game, Level level, Map<TileDef, ObjectDef> goalMap) {
		val tileChars = new LinkedHashMap<String, Integer>
		val objectChars = new LinkedHashMap<String, Integer>
		val legendChars = new LinkedHashMap<String, LegendEntry>
		val tilesList = game.tiles.entries
		val objectsList = objectsOf(game)
		val tileIndexMap = new IdentityHashMap<TileDef, Integer>
		val objectIndexMap = new IdentityHashMap<ObjectDef, Integer>

		for (var i = 0; i < tilesList.size; i++) {
			val tile = tilesList.get(i)
			tileChars.put(tile.char, i)
			tileIndexMap.put(tile, i)
		}
		for (var i = 0; i < objectsList.size; i++) {
			val object = objectsList.get(i)
			objectChars.put(object.char, i)
			objectIndexMap.put(object, i)
		}
		for (entry : legendEntriesOf(game))
			legendChars.put(entry.char, entry)

		width = level.rows.get(0).length
		height = level.rows.size

		for (var y = 0; y < height; y++) {
			val row = level.rows.get(y)
			for (var x = 0; x < width; x++) {
				val character = String.valueOf(row.charAt(x))
				var int tileIdx

				if (legendChars.containsKey(character)) {
					val entry = legendChars.get(character)
					tileIdx = tileIndexMap.get(entry.tile)
					if (entry.entity instanceof PlayerRef) {
						playerX = x; playerY = y
					} else if (entry.entity instanceof ObjectRef) {
						boxes.add(new CellEntity(x, y, objectIndexMap.get((entry.entity as ObjectRef).ref)))
					}
				} else if (objectChars.containsKey(character)) {
					tileIdx = 0
					boxes.add(new CellEntity(x, y, objectChars.get(character)))
				} else if (tileChars.containsKey(character)) {
					tileIdx = tileChars.get(character)
				} else {
					// Validator's checkLevelCharsValid catches this and warns, but only as a
					// warning. Refusing here keeps a corrupted ROM from being emitted silently.
					throw new IllegalStateException(
						"Level character '" + character + "' at (" + x + "," + y + ") is not declared in tiles, objects, or legend")
				}

				mapData.add(tileIdx)

				val goalObject = goalMap.get(tilesList.get(tileIdx))
				if (goalObject !== null)
					goals.add(new CellEntity(x, y, objectIndexMap.get(goalObject)))
			}
		}
	}
}
