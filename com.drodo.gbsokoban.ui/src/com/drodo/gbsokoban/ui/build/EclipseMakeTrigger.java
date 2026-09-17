package com.drodo.gbsokoban.ui.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.generator.IFileSystemAccess2;

import com.drodo.gbsokoban.generator.BuildTrigger;

public class EclipseMakeTrigger implements BuildTrigger {

	@Override
	public void afterGenerate(IFileSystemAccess2 fsa, String makefilePath) {
		Path directory = resolveDirectory(fsa.getURI(makefilePath));
		if (directory == null || !Files.isDirectory(directory))
			return;
		try {
			new ProcessBuilder("make").directory(directory.toFile()).inheritIO().start();
		} catch (IOException e) {
			ILog.of(EclipseMakeTrigger.class)
					.error("Could not launch make in " + directory + ". Check that make is on the PATH.", e);
		}
	}

	private static Path resolveDirectory(URI uri) {
		if (uri.isFile())
			return Paths.get(uri.toFileString()).getParent();
		if (!uri.isPlatformResource())
			return null;
		IFile file = ResourcesPlugin.getWorkspace().getRoot()
				.getFile(IPath.fromPortableString(uri.toPlatformString(true)));
		IPath location = file.getRawLocation();
		return location == null ? null : Paths.get(location.toOSString()).getParent();
	}
}
