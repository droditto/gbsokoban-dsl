package com.drodo.gbsokoban.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.drodo.gbsokoban.gBSokoban.AllOn;
import com.drodo.gbsokoban.gBSokoban.Conveyor;
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.Level;
import com.drodo.gbsokoban.gBSokoban.NoEntity;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.ObjectRef;
import com.drodo.gbsokoban.gBSokoban.PlayerRef;
import com.drodo.gbsokoban.gBSokoban.Solid;
import com.drodo.gbsokoban.gBSokoban.SomeOn;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.gBSokoban.WinCondition;
import com.drodo.gbsokoban.model.Directions;
import com.drodo.gbsokoban.model.GameTextures;
import com.drodo.gbsokoban.model.GoalTiles;
import com.drodo.gbsokoban.model.Levels;
import com.drodo.gbsokoban.model.SymbolPool;
import com.drodo.gbsokoban.util.CellGeometry;
import com.drodo.gbsokoban.util.Messages;
import com.drodo.gbsokoban.util.VramLayout;

public class LevelValidator extends AbstractDeclarativeValidator {

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	@Check
	public void checkLevelFits(Level level) {
		int cellPx = GameTextures.cellPx(EcoreUtil2.getContainerOfType(level, Game.class));
		if (cellPx == 0)
			return;
		// The background map is 32x32 hardware tiles.
		int maxCells = VramLayout.BKG_MAP_TILES / new CellGeometry(cellPx).tilesPerSide();

		int height = level.getRows().size();
		int width = Levels.width(level);
		if (width > maxCells)
			error(Messages.get("level.tooWide", maxCells),
					level, GBSokobanPackage.Literals.LEVEL__ROWS, 0);
		if (height > maxCells)
			error(Messages.get("level.tooTall", maxCells),
					level, GBSokobanPackage.Literals.LEVEL__ROWS, height - 1);
	}

	@Check
	public void checkLevelHasExactlyOnePlayer(Level level) {
		Game game = EcoreUtil2.getContainerOfType(level, Game.class);
		if (game == null || game.getPlayer() == null)
			return;

		Set<String> playerSymbols = new HashSet<>();
		for (LegendEntry entry : game.getLegend())
			if (entry.getSubject() instanceof PlayerRef && entry.getSymbol() != null)
				playerSymbols.add(entry.getSymbol());
		if (playerSymbols.isEmpty())
			return;

		int count = 0;
		int extraRow = -1;
		for (int y = 0; y < level.getRows().size(); y++) {
			String row = level.getRows().get(y);
			for (int x = 0; x < row.length(); x++)
				if (playerSymbols.contains(String.valueOf(row.charAt(x)))) {
					count++;
					if (count == 2)
						extraRow = y;
				}
		}

		if (count == 0)
			error(Messages.get("level.noPlayer"), level,
					GBSokobanPackage.Literals.LEVEL__ROWS, 0);
		else if (count > 1)
			error(Messages.get("level.manyPlayers"), level,
					GBSokobanPackage.Literals.LEVEL__ROWS, extraRow, GBSokobanValidator.ISSUE_LEVEL_TOO_MANY_PLAYERS);
	}

	@Check
	public void checkLevelSymbolsAreDeclared(Level level) {
		Game game = EcoreUtil2.getContainerOfType(level, Game.class);
		if (game == null)
			return;
		Set<String> known = SymbolPool.usedIn(game);

		Set<String> reported = new HashSet<>();
		for (int y = 0; y < level.getRows().size(); y++) {
			String row = level.getRows().get(y);
			for (int x = 0; x < row.length(); x++) {
				String symbol = String.valueOf(row.charAt(x));
				if (!known.contains(symbol) && reported.add(symbol))
					error(Messages.get("level.undeclared", symbol),
							level, GBSokobanPackage.Literals.LEVEL__ROWS, y);
			}
		}
	}

	private record LevelGrid(int width, int height, List<TileDef> tiles, List<ObjectDef> objects) {
		TileDef at(int x, int y) {
			return tiles.get(y * width + x);
		}
	}

	private static LevelGrid gridOf(Game game, Level level) {
		Map<String, TileDef> tileBySymbol = new HashMap<>();
		for (TileDef tile : game.getTiles())
			if (tile.getSymbol() != null)
				tileBySymbol.put(tile.getSymbol(), tile);
		Map<String, LegendEntry> legendBySymbol = new HashMap<>();
		for (LegendEntry entry : game.getLegend())
			if (entry.getSymbol() != null)
				legendBySymbol.put(entry.getSymbol(), entry);

		int height = level.getRows().size();
		int width = Levels.width(level);
		List<TileDef> tiles = new ArrayList<>(width * height);
		List<ObjectDef> objects = new ArrayList<>();

		for (int y = 0; y < height; y++) {
			String row = level.getRows().get(y);
			for (int x = 0; x < width; x++) {
				if (x >= row.length()) {
					tiles.add(null);
					continue;
				}
				String symbol = String.valueOf(row.charAt(x));
				LegendEntry entry = legendBySymbol.get(symbol);
				if (entry == null) {
					tiles.add(tileBySymbol.get(symbol));
					continue;
				}
				tiles.add(entry.getTile());
				if (entry.getSubject() instanceof ObjectRef
						&& ((ObjectRef) entry.getSubject()).getObject() != null)
					objects.add(((ObjectRef) entry.getSubject()).getObject());
			}
		}
		return new LevelGrid(width, height, tiles, objects);
	}

	@Check
	public void checkConveyorCycle(Level level) {
		Game game = EcoreUtil2.getContainerOfType(level, Game.class);
		if (game == null || game.getTiles().isEmpty())
			return;
		LevelGrid grid = gridOf(game, level);
		int cells = grid.width() * grid.height();
		if (cells == 0)
			return;

		int[] carriesTo = new int[cells];
		Arrays.fill(carriesTo, -1);
		for (int y = 0; y < grid.height(); y++)
			for (int x = 0; x < grid.width(); x++) {
				if (!(behaviourOf(grid.at(x, y)) instanceof Conveyor))
					continue;
				Conveyor belt = (Conveyor) behaviourOf(grid.at(x, y));
				int nx = x + Directions.stepX(belt.getDirection());
				int ny = y + Directions.stepY(belt.getDirection());
				if (nx < 0 || nx >= grid.width() || ny < 0 || ny >= grid.height())
					continue;
				TileDef target = grid.at(nx, ny);
				if (target == null || target.getBehaviour() instanceof Solid)
					continue;
				carriesTo[y * grid.width() + x] = ny * grid.width() + nx;
			}

		int[] visited = new int[cells];
		for (int start = 0; start < cells; start++) {
			if (visited[start] != 0 || carriesTo[start] < 0)
				continue;
			List<Integer> walked = new ArrayList<>();
			int cell = start;
			while (cell >= 0 && visited[cell] == 0) {
				visited[cell] = 1;
				walked.add(cell);
				cell = carriesTo[cell];
			}
			if (cell >= 0 && visited[cell] == 1) {
				List<INode> rows = NodeModelUtils.findNodesForFeature(level, GBSokobanPackage.Literals.LEVEL__ROWS);
				String message = Messages.get("level.conveyorLoop");
				int loop = cell;
				do {
					int offset = rows.get(loop / grid.width()).getOffset() + 1 + loop % grid.width();
					getMessageAcceptor().acceptError(message, level, offset, 1, null);
					loop = carriesTo[loop];
				} while (loop != cell);
				return;
			}
			for (int seen : walked)
				visited[seen] = 2;
		}
	}

	private static Object behaviourOf(TileDef tile) {
		return tile == null ? null : tile.getBehaviour();
	}

	@Check
	public void checkLevelIsPlayable(Level level) {
		Game game = EcoreUtil2.getContainerOfType(level, Game.class);
		if (game == null || game.getWin().isEmpty() || game.getTiles().isEmpty()
				|| GBSokobanValidator.hasUnresolvedReferences(game))
			return;
		LevelGrid grid = gridOf(game, level);
		Set<TileDef> present = new HashSet<>();
		for (TileDef tile : grid.tiles())
			if (tile != null)
				present.add(tile);

		for (WinCondition condition : game.getWin()) {
			Boolean vacuous = isVacuousIn(condition, present, grid.objects());
			if (vacuous == null || !vacuous)
				return;
		}
		warning(Messages.get("level.noWinApplies"),
				level, GBSokobanPackage.Literals.LEVEL__ROWS, 0);
	}

	@Check
	public void checkLevelHasEnoughObjects(Level level) {
		Game game = EcoreUtil2.getContainerOfType(level, Game.class);
		if (game == null || game.getTiles().isEmpty() || GBSokobanValidator.hasUnresolvedReferences(game))
			return;
		LevelGrid grid = gridOf(game, level);
		Map<TileDef, ObjectDef> owners = GoalTiles.owners(game);
		for (WinCondition condition : game.getWin()) {
			TileDef tile = GoalTiles.tileOf(condition);
			ObjectDef object = GoalTiles.objectOf(condition);
			if (tile == null || object == null || owners.get(tile) != object)
				continue;
			int goals = Collections.frequency(grid.tiles(), tile);
			int placed = Collections.frequency(grid.objects(), object);
			int needed = condition instanceof AllOn ? goals : Math.min(goals, 1);
			if (placed < needed)
				warning(Messages.get("level.notEnough", object.getName(), tile.getName()),
						level, GBSokobanPackage.Literals.LEVEL__ROWS, 0);
		}
	}

	private static Boolean isVacuousIn(WinCondition condition, Set<TileDef> tiles, List<ObjectDef> objects) {
		if (condition instanceof AllOn) {
			TileDef tile = ((AllOn) condition).getTile();
			return tile == null ? null : !tiles.contains(tile);
		}
		if (condition instanceof SomeOn) {
			TileDef tile = ((SomeOn) condition).getTile();
			return tile == null ? null : !tiles.contains(tile);
		}
		if (condition instanceof NoEntity) {
			EObject entity = ((NoEntity) condition).getEntity();
			if (entity instanceof TileDef)
				return !tiles.contains(entity);
			if (entity instanceof ObjectDef)
				return !objects.contains(entity);
		}
		return null;
	}
}
