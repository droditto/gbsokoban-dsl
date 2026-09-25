package com.drodo.gbsokoban.generator;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.generator.GeneratorContext;
import org.eclipse.xtext.generator.GeneratorDelegate;
import org.eclipse.xtext.generator.JavaIoFileSystemAccess;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import com.drodo.gbsokoban.GBSokobanStandaloneSetup;
import com.google.inject.Injector;

/** Generates the C project for a .gbsoko. Prints issues as severity|line|message. */
public final class GenerateCommand {

	private GenerateCommand() {
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("usage: GenerateCommand <file.gbsoko> <output directory>");
			System.exit(2);
		}
		Path input = Path.of(args[0]).toAbsolutePath();
		Path output = Path.of(args[1]).toAbsolutePath();

		Injector injector = new GBSokobanStandaloneSetup().createInjectorAndDoEMFRegistration();
		XtextResourceSet resources = injector.getInstance(XtextResourceSet.class);
		Resource resource = resources.getResource(URI.createFileURI(input.toString()), true);

		List<Issue> issues = injector.getInstance(IResourceValidator.class)
				.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
		boolean failed = false;
		for (Issue issue : issues) {
			System.out.println(issue.getSeverity() + "|" + issue.getLineNumber() + "|" + issue.getMessage());
			failed |= issue.getSeverity() == Severity.ERROR;
		}
		if (failed)
			System.exit(1);

		JavaIoFileSystemAccess fsa = injector.getInstance(JavaIoFileSystemAccess.class);
		fsa.setOutputPath(output.toString());
		injector.getInstance(GeneratorDelegate.class).generate(resource, fsa, new GeneratorContext());
	}
}
