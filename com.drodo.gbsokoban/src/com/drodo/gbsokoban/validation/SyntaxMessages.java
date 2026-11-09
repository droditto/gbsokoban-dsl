package com.drodo.gbsokoban.validation;

import org.antlr.runtime.MismatchedTokenException;
import org.antlr.runtime.MissingTokenException;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.UnwantedTokenException;
import org.eclipse.xtext.nodemodel.SyntaxErrorMessage;
import org.eclipse.xtext.parser.antlr.SyntaxErrorMessageProvider;

import com.drodo.gbsokoban.util.Messages;

public class SyntaxMessages extends SyntaxErrorMessageProvider {

	@Override
	public SyntaxErrorMessage getSyntaxErrorMessage(IParserErrorContext context) {
		SyntaxErrorMessage original = super.getSyntaxErrorMessage(context);
		RecognitionException e = context.getRecognitionException();
		if (e == null || e.token == null)
			return original;
		boolean atEnd = e.token.getType() == Token.EOF;
		String found = e.token.getText();
		String message;
		if (e instanceof MissingTokenException) {
			int missing = ((MissingTokenException) e).getMissingType();
			if (missing == Token.EOF)
				message = Messages.get("syntax.noViable", found);
			else
				message = atEnd ? Messages.get("syntax.unfinished", expected(context, missing))
						: Messages.get("syntax.missing", expected(context, missing), found);
		} else if (e instanceof MismatchedTokenException) {
			String expected = expected(context, ((MismatchedTokenException) e).expecting);
			String key = e instanceof UnwantedTokenException ? "syntax.extraneous" : "syntax.mismatched";
			message = atEnd ? Messages.get("syntax.unfinished", expected)
					: withNameHint(Messages.get(key, found, expected), e, context);
		} else
			message = atEnd ? Messages.get("syntax.incomplete") : Messages.get("syntax.noViable", found);
		return new SyntaxErrorMessage(message, original.getIssueCode(), original.getIssueData());
	}

	@Override
	public SyntaxErrorMessage getSyntaxErrorMessage(IValueConverterErrorContext context) {
		SyntaxErrorMessage original = super.getSyntaxErrorMessage(context);
		String text = original.getMessage();
		if (text == null || !text.startsWith("Invalid escape sequence"))
			return original;
		return new SyntaxErrorMessage(Messages.get("syntax.escape"), original.getIssueCode(),
				original.getIssueData());
	}

	private static String withNameHint(String message, RecognitionException e, IParserErrorContext context) {
		int type = ((MismatchedTokenException) e).expecting;
		String expected = type == Token.EOF ? "EOF" : context.getTokenNames()[type];
		boolean secondWord = e.token.getText() != null && e.token.getText().matches("[A-Za-z_]\\w*")
				&& (expected.equals("'USES'") || expected.equals("RULE_STRING"));
		return secondWord ? message + " " + Messages.get("syntax.spaceInName") : message;
	}

	private static String expected(IParserErrorContext context, int type) {
		return type == Token.EOF ? name("EOF") : name(context.getTokenNames()[type]);
	}

	private static String name(String token) {
		if (Messages.has("syntax.token." + token))
			return Messages.get("syntax.token." + token);
		if (token.length() > 2 && token.startsWith("'") && token.endsWith("'"))
			return Messages.get("syntax.keyword", token.substring(1, token.length() - 1));
		return token;
	}
}
