package jribble_eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.util.Map;

public class JribbleBuilder extends IncrementalProjectBuilder {

  @Override
  protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
    if (!getProject().isNatureEnabled("org.eclipse.jdt.core.javanature")) {
      return null;
    }
    IJavaProject javaProject = JavaCore.create(getProject());
    if (kind == IncrementalProjectBuilder.FULL_BUILD) {
      fullBuild(monitor);
    } else {
      IResourceDelta delta = getDelta(getProject());
      if (delta == null) {
        fullBuild(monitor);
      } else {
        incrementalBuild(javaProject, delta, monitor);
      }
    }
    return null;
  }

  private void incrementalBuild(final IJavaProject javaProject, IResourceDelta delta, IProgressMonitor monitor) {
    System.out.println("incremental build on " + delta);
    try {
      delta.accept(new IResourceDeltaVisitor() {
        public boolean visit(IResourceDelta delta) throws CoreException {
          if ("java".equals(delta.getResource().getFullPath().getFileExtension())) {
            System.out.println("changed: " + delta.getResource().getRawLocation());
            for (IPackageFragmentRoot root : javaProject.getPackageFragmentRoots()) {
              if (root.getPath().isPrefixOf(delta.getResource().getFullPath())) {
                IPath relative = delta.getResource().getFullPath().removeFirstSegments(root.getPath().segmentCount());
                IJavaElement element = javaProject.findElement(relative);
                if (element instanceof ICompilationUnit) {
                  ICompilationUnit unit = (ICompilationUnit) element;
                  CompilationUnit parse = parse(unit);
                  parse.accept(new JribbleVisitor(parse));
                  System.out.println("  parsed: " + relative);
                }
              }
            }
          }
          return true; // visit children too
        }
      });
    } catch (CoreException e) {
      e.printStackTrace();
    }
  }

  private void fullBuild(IProgressMonitor monitor) {
    System.out.println("full build");
  }
  
  private static CompilationUnit parse(ICompilationUnit unit) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setSource(unit);
    parser.setResolveBindings(true);
    return (CompilationUnit) parser.createAST(null); // parse
  }
}
