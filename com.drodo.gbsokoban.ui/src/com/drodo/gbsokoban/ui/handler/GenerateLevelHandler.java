package com.drodo.gbsokoban.ui.handler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

import dev.langchain4j.model.openai.OpenAiChatModel;

/** Generates a level via Groq and inserts it at the cursor of the open .gbsoko. */
public class GenerateLevelHandler extends AbstractHandler {

	private static final String DIALOG_TITLE = "Generate Level";
	private static final String MODEL = "llama-3.3-70b-versatile";
	private static final double TEMPERATURE = 0.7;
	private static final int PREVIEW_MAX_CHARS = 500;
	private static final int RAW_RESPONSE_MAX_CHARS = 1500;

	private static final String PROMPT_HEADER = """
			You are an expert Sokoban puzzle designer. Produce ONE level for \
			a Game Boy Sokoban game written in the gbsokoban DSL.

			""";

	private static final String PROMPT_RULES = """
			<rules>
			- Every character in the level MUST be declared in <game>'s `tiles`, `objects`, or `legend` blocks. Spaces are not allowed.
			- All rows MUST have the same length. The level MUST fit in a 16x16 grid. Use the size from <intent>; otherwise pick a size in [4, 16].
			- The level MUST contain exactly one player character (the legend entry bound to `Player`).
			- Don't use `deadly` or `fillable` tiles as background filler. They kill the player or destroy pushed objects on contact, so place them only as deliberate gameplay elements.
			- A cell with `solid` tiles on two adjacent sides traps any object pushed into it forever. Don't place objects in non-goal corners, and design the level so the player cannot push one in.
			- For each `all <Object> on <Tile>` win condition in <game>: place at least as many <Object> characters as <Tile> characters, otherwise the puzzle is unsolvable.
			- For each `some <Object> on <Tile>` win condition in <game>: place at least one <Object> character and at least one <Tile> character.
			- For each `no <Tile>` win condition in <game>: include at least one such tile, and ensure the player can reach (or push objects to) every instance.
			</rules>

			""";

	private static final String PROMPT_OUTPUT = """
			<output_format>
			Output ONLY the level block. No prose, no markdown fences, no XML tags, no comments.
			- `level {` on its own line at column 0.
			- Each row is a quoted string on its own line indented with 4 spaces.
			- `}` on its own line at column 0.
			</output_format>
			""";

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		Shell shell = window.getShell();

		IEditorPart editor = window.getActivePage().getActiveEditor();
		if (!(editor instanceof XtextEditor)) {
			MessageDialog.openError(shell, DIALOG_TITLE,
					"Open a .gbsoko file before generating a level.");
			return null;
		}
		XtextEditor xtextEditor = (XtextEditor) editor;
		IXtextDocument doc = xtextEditor.getDocument();
		ITextSelection selection =
				(ITextSelection) xtextEditor.getSelectionProvider().getSelection();
		int cursorOffset = selection.getOffset();
		String gameSource = doc.get();

		String apiKey = System.getenv("GROQ_API");
		if (apiKey == null || apiKey.isBlank()) {
			MessageDialog.openError(shell, DIALOG_TITLE,
					"GROQ_API is not set. Export it in the shell that launches Eclipse.");
			return null;
		}

		InputDialog descDialog = new InputDialog(shell,
				DIALOG_TITLE,
				"Describe the level you want:",
				"easy 8x8 level with one object on ice",
				null);
		if (descDialog.open() != InputDialog.OK) return null;
		String description = descDialog.getValue();
		if (description == null || description.isBlank()) {
			MessageDialog.openWarning(shell, DIALOG_TITLE,
					"A description is required.");
			return null;
		}

		scheduleGeneration(shell, doc, cursorOffset, gameSource, description, apiKey);
		return null;
	}

	private static void scheduleGeneration(Shell shell, IXtextDocument doc, int cursorOffset,
			String gameSource, String description, String apiKey) {
		Job job = Job.create(DIALOG_TITLE, (IProgressMonitor monitor) -> {
			monitor.beginTask("Generating level via Groq", IProgressMonitor.UNKNOWN);
			try {
				OpenAiChatModel model = OpenAiChatModel.builder()
						.baseUrl("https://api.groq.com/openai/v1")
						.apiKey(apiKey)
						.modelName(MODEL)
						.temperature(TEMPERATURE)
						.build();
				String raw = model.chat(buildPrompt(gameSource, description));
				String levelBlock = cleanGeneratedLevel(raw);
				Display.getDefault().asyncExec(
						() -> presentResult(shell, doc, cursorOffset, raw, levelBlock));
				return Status.OK_STATUS;
			} catch (Exception e) {
				Display.getDefault().asyncExec(() -> MessageDialog.openError(
						shell, DIALOG_TITLE, "Generation failed: " + e.getMessage()));
				// The Job framework would log a real failure. We've already shown the user, so report OK.
				return Status.OK_STATUS;
			} finally {
				monitor.done();
			}
		});
		job.setUser(true);
		job.schedule();
	}

	private static void presentResult(Shell shell, IXtextDocument doc, int cursorOffset,
			String raw, String levelBlock) {
		if (levelBlock.isEmpty()) {
			String detail = (raw == null || raw.isBlank()) ? "(empty response)"
					: raw.length() > RAW_RESPONSE_MAX_CHARS
							? raw.substring(0, RAW_RESPONSE_MAX_CHARS) + "\n... (truncated)"
							: raw;
			MessageDialog.openError(shell, DIALOG_TITLE,
					"The model did not return a recognisable level block.\n\nRaw response:\n" + detail);
			return;
		}
		String preview = levelBlock.length() > PREVIEW_MAX_CHARS
				? levelBlock.substring(0, PREVIEW_MAX_CHARS) + "..."
				: levelBlock;
		if (!MessageDialog.openConfirm(shell, "Generated level",
				"Insert this level at the cursor?\n\n" + preview)) return;
		try {
			doc.replace(cursorOffset, 0, "\n" + levelBlock.indent(8));
		} catch (Exception e) {
			MessageDialog.openError(shell, DIALOG_TITLE,
					"Failed to insert level: " + e.getMessage());
		}
	}

	private static String buildPrompt(String gameSource, String description) {
		StringBuilder prompt = new StringBuilder(PROMPT_HEADER);
		if (!gameSource.isEmpty())
			prompt.append("<game>\n").append(gameSource).append("\n</game>\n\n");
		prompt.append("<intent>\n").append(description).append("\n</intent>\n\n");
		prompt.append(PROMPT_RULES);
		prompt.append(PROMPT_OUTPUT);
		return prompt.toString();
	}

	// Anchored block match: a literal `level { ... }` where the closing `}` starts a line.
	// Prevents a trailing `// notes about }` from being included as part of the block.
	private static final Pattern LEVEL_BLOCK = Pattern.compile(
			"^level\\s*\\{[\\s\\S]*?^\\}", Pattern.MULTILINE);

	// If that fails, locate the keyword `level` immediately followed by `{`, anywhere in the text.
	// Skips a preceding "Here's the level:" preamble that would fool a plain indexOf("level").
	private static final Pattern LEVEL_KEYWORD = Pattern.compile("\\blevel\\s*\\{");

	private static String cleanGeneratedLevel(String raw) {
		if (raw == null) return "";
		String stripped = raw.replaceAll("```[a-zA-Z]*\\n?", "").replaceAll("```", "").trim();
		Matcher m = LEVEL_BLOCK.matcher(stripped);
		if (m.find()) return m.group().trim();
		Matcher startMatcher = LEVEL_KEYWORD.matcher(stripped);
		if (!startMatcher.find()) return stripped;
		int start = startMatcher.start();
		int end = stripped.lastIndexOf('}');
		if (end > start) return stripped.substring(start, end + 1).trim();
		return stripped.substring(start).trim() + "\n}";
	}
}
