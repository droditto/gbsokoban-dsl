package com.drodo.gbsokoban.generator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EnginePreprocessor {

	private EnginePreprocessor() {
	}

	public static String resolve(String source, Set<String> flags, Map<String, String> constants) {
		Resolver resolver = new Resolver(flags, constants);
		StringBuilder out = new StringBuilder(source.length());

		for (String line : source.split("\n", -1)) {
			if (resolver.dropsLine(line.strip()))
				continue;
			out.append(line).append('\n');
		}
		out.setLength(out.length() - 1);
		return collapseBlankRuns(out.toString());
	}

	/** Dropping a directive leaves the blank lines that framed it doubled up. */
	private static String collapseBlankRuns(String text) {
		return text.replaceAll("\n{3,}", "\n\n");
	}

	private static final class Block {
		final boolean resolved;
		boolean live;
		boolean branchTaken;

		Block(boolean resolved, boolean live) {
			this.resolved = resolved;
			this.live = live;
			this.branchTaken = live;
		}
	}

	private static final class Resolver {
		private final Set<String> flags;
		private final Map<String, String> constants;
		private final Deque<Block> open = new ArrayDeque<>();

		Resolver(Set<String> flags, Map<String, String> constants) {
			this.flags = flags;
			this.constants = constants;
		}

		boolean dropsLine(String line) {
			if (!line.startsWith("#"))
				return !emitting();

			String directive = line.substring(1).stripLeading();
			if (directive.startsWith("if"))
				return openBlock(directive);
			if (directive.startsWith("elif") || directive.startsWith("else"))
				return continueBlock(directive);
			if (directive.startsWith("endif")) {
				Block block = open.pop();
				return block.resolved || !emitting();
			}
			return !emitting();
		}

		private boolean openBlock(String directive) {
			Boolean value = evaluate(directive);
			boolean enclosed = emitting();
			open.push(new Block(value != null, value != null && value && enclosed));
			return value != null || !enclosed;
		}

		private boolean continueBlock(String directive) {
			Block block = open.peek();
			if (!block.resolved)
				return !emittingOutside(block);

			if (block.branchTaken) {
				block.live = false;
			} else {
				Boolean value = directive.startsWith("else") ? Boolean.TRUE : evaluate(directive);
				block.live = value != null && value && emittingOutside(block);
				block.branchTaken = block.live;
			}
			return true;
		}

		private boolean emitting() {
			for (Block block : open) {
				if (block.resolved && !block.live)
					return false;
			}
			return true;
		}

		private boolean emittingOutside(Block inner) {
			for (Block block : open) {
				if (block != inner && block.resolved && !block.live)
					return false;
			}
			return true;
		}

		private Boolean evaluate(String directive) {
			int space = directive.indexOf(' ');
			if (space < 0)
				return null;
			String keyword = directive.substring(0, space);
			String expression = directive.substring(space + 1).strip();

			switch (keyword) {
			case "ifdef":
				return isKnownFlag(expression) ? flags.contains(expression) : null;
			case "ifndef":
				return isKnownFlag(expression) ? !flags.contains(expression) : null;
			default:
				return expressionValue(expression);
			}
		}

		private Boolean expressionValue(String expression) {
			Boolean any = null;
			for (String term : splitOutsideParens(expression, "||")) {
				Boolean all = null;
				for (String atom : splitOutsideParens(term, "&&")) {
					Boolean value = atomValue(atom.strip());
					if (value == null)
						return null;
					all = all == null ? value : all && value;
				}
				any = any == null ? all : any || all;
			}
			return any;
		}

		private static List<String> splitOutsideParens(String expression, String operator) {
			List<String> parts = new ArrayList<>();
			int depth = 0;
			int start = 0;
			for (int i = 0; i < expression.length(); i++) {
				char c = expression.charAt(i);
				if (c == '(') {
					depth++;
				} else if (c == ')') {
					depth--;
				} else if (depth == 0 && expression.startsWith(operator, i)) {
					parts.add(expression.substring(start, i));
					i += operator.length() - 1;
					start = i + 1;
				}
			}
			parts.add(expression.substring(start));
			return parts;
		}

		private static boolean isGrouped(String atom) {
			if (!atom.startsWith("(") || !atom.endsWith(")"))
				return false;
			int depth = 0;
			for (int i = 0; i < atom.length() - 1; i++) {
				if (atom.charAt(i) == '(')
					depth++;
				else if (atom.charAt(i) == ')' && --depth == 0)
					return false;
			}
			return true;
		}

		private Boolean atomValue(String atom) {
			if (atom.startsWith("!")) {
				Boolean value = atomValue(atom.substring(1).stripLeading());
				return value == null ? null : !value;
			}
			if (isGrouped(atom))
				return expressionValue(atom.substring(1, atom.length() - 1).strip());
			if (atom.startsWith("defined")) {
				String name = atom.substring("defined".length()).strip();
				if (name.startsWith("(") && name.endsWith(")"))
					name = name.substring(1, name.length() - 1).strip();
				return isKnownFlag(name) ? flags.contains(name) : null;
			}
			int equals = atom.indexOf("==");
			if (equals < 0)
				return null;
			String left = valueOf(atom.substring(0, equals).strip());
			String right = valueOf(atom.substring(equals + 2).strip());
			return left == null || right == null ? null : left.equals(right);
		}

		private String valueOf(String term) {
			if (!term.isEmpty() && term.chars().allMatch(Character::isDigit))
				return term;
			return constants.get(term);
		}

		private boolean isKnownFlag(String name) {
			return name.startsWith("FEAT_");
		}
	}
}
