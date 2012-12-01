package jribble_eclipse;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import java.io.File;

public class PathUtils {

  // Resolves /project/bin/... to a file system path via IWorkspaceRoot.getFile
  static File toFile(IJavaProject javaProject, IPath workspaceRelativePath) {
    return javaProject.getProject().getWorkspace().getRoot().getFile(workspaceRelativePath)
        .getRawLocation().toFile();
  }

  /** @return the {@code root}'s output, if configured, or the {@code javaProject}'s default output. */
  static IPath getOutput(IJavaProject javaProject, IPackageFragmentRoot root)
      throws JavaModelException {
    IPath output = root.getResolvedClasspathEntry().getOutputLocation();
    if (output == null) {
      output = javaProject.getOutputLocation();
    }
    return output;
  }

  /** @return the workspace-relative path for the jribble file for {@code unit}. */
  static IPath getAstPath(IPath output, IPath packagePath, ICompilationUnit unit)
      throws JavaModelException {
    // Get to /project/bin/package/Foo.ast
    return output // /project/bin/ +
        .append(unit.getPath() // /project/src/main/java/package/Foo.java
            .removeFileExtension().addFileExtension("ast") // .java -> .ast
            .removeFirstSegments(packagePath.segmentCount())); // drop /project/src/main/java
  }

}
