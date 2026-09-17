package com.drodo.gbsokoban.generator;

import org.eclipse.xtext.generator.IFileSystemAccess2;

public interface BuildTrigger {

	void afterGenerate(IFileSystemAccess2 fsa, String makefilePath);

	final class None implements BuildTrigger {

		@Override
		public void afterGenerate(IFileSystemAccess2 fsa, String makefilePath) {
		}
	}
}
