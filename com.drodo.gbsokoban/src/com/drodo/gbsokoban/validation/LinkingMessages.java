package com.drodo.gbsokoban.validation;

import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.linking.impl.LinkingDiagnosticMessageProvider;

import com.drodo.gbsokoban.util.Messages;

public class LinkingMessages extends LinkingDiagnosticMessageProvider {

	@Override
	public DiagnosticMessage getUnresolvedProxyMessage(ILinkingDiagnosticContext context) {
		DiagnosticMessage original = super.getUnresolvedProxyMessage(context);
		String key = "link." + context.getReference().getEReferenceType().getName();
		String linkText;
		try {
			linkText = context.getLinkText();
		} catch (IllegalNodeException e) {
			linkText = e.getNode().getText();
		}
		return new DiagnosticMessage(Messages.get(key, linkText), original.getSeverity(), original.getIssueCode(),
				original.getIssueData());
	}
}
