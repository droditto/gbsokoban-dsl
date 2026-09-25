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
import com.drodo.gbsokoban.gBSokoban.Subject;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.gBSokoban.WinCondition;
import com.drodo.gbsokoban.model.Directions;
import com.drodo.gbsokoban.model.GameTextures;
import com.drodo.gbsokoban.model.Levels;
import com.drodo.gbsokoban.model.SymbolPool;
import com.drodo.gbsokoban.util.CellGeometry;
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
			error("This level is " + width + " cells wide. At " + cellPx + "x" + cellPx
					+ " the most that fits is " + maxCells,
					level, GBSokobanPackage.Literals.LEVEL__ROWS, 0);
		if (height > maxCells)
			error("This level is " + height + " cells tall. At " + cellPx + "x" + cellPx
					+ " the most that fits is " + maxCells,
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
			error("A level must place exactly one player", level,
					GBSokobanPackage.Literals.LEVEL__ROWS, 0);
		else if (count > 1)
			error("This level has " + count + " players. It needs exactly one", level,
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
					error("'" + symbol + "' is not declared. Add a tile or a legend entry for it",
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
				String symbol = x < row.length()
						? String.valueOf(row.charAt(x))
						: SymbolPool.PADDING_SYMBOL;
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
				error("The conveyor at (" + cell % grid.width() + "," + cell / grid.width()
						+ ") carries the player round in a loop. The level could never be left",
						level, GBSokobanPackage.Literals.LEVEL__ROWS, cell / grid.width());
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
		if (game == null || game.getWin().isEmpty() || game.getTiles().isEmpty())
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
		warning("No win condition applies here, so the level clears on the first move",
				level, GBSokobanPackage.Literals.LEVEL__ROWS, 0);
	}

	@Check
	public void checkLevelHasEnoughObjects(Level level) {
		Game game = EcoreUtil2.getContainerOfType(level, Game.class);
		if (game == null || game.getTiles().isEmpty())
			return;
		LevelGrid grid = gridOf(game, level);
		for (WinCondition condition : game.getWin()) {
			boolean all = condition instanceof AllOn;
			if (!all && !(condition instanceof SomeOn))
				continue;
			Subject subject = all ? ((AllOn) condition).getSubject() : ((SomeOn) condition).getSubject();
			TileDef tile = all ? ((AllOn) condition).getTile() : ((SomeOn) condition).getTile();
			if (!(subject instanceof ObjectRef) || tile == null)
				continue;
			ObjectDef object = ((ObjectRef) subject).getObject();
			int goals = Collections.frequency(grid.tiles(), tile);
			int placed = Collections.frequency(grid.objects(), object);
			int needed = all ? goals : Math.min(goals, 1);
			if (object != null && placed < needed)
				warning("This level needs " + needed + " '" + object.getName() + "' on '" + tile.getName()
						+ "' and has " + placed + ", so it can never be completed",
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
