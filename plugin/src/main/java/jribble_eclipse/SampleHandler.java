package jribble_eclipse;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class SampleHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    // Get all projects in the workspace
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      try {
        if (project.isOpen() && project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
          printPackageInfos(JavaCore.create(project));
        }
      } catch (CoreException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private static void printPackageInfos(IJavaProject javaProject) throws JavaModelException {
    for (IPackageFragmentRoot root : javaProject.getPackageFragmentRoots()) {
      if (root.getKind() != IPackageFragmentRoot.K_SOURCE) {
        continue;
      }
      for (IJavaElement packageFragment : root.getChildren()) {
        for (ICompilationUnit unit : ((IPackageFragment) packageFragment).getCompilationUnits()) {
          // CompilationUnit parse = parse(unit);
          // parse.accept(new MethodVisitor());
          try {
            IPath ast = PathUtils.getAstPath(PathUtils.getOutput(javaProject, root), root.getPath(), unit);
            File file = PathUtils.toFile(javaProject, ast);
            file.getParentFile().mkdirs(); // is this needed?
            BufferedWriter w = new BufferedWriter(new FileWriter(file));
            w.write("asdf");
            w.flush();
            w.close();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  private static CompilationUnit parse(ICompilationUnit unit) {
    ASTParser parser = ASTParser.newParser(AST.JLS3);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setSource(unit);
    parser.setResolveBindings(true);
    return (CompilationUnit) parser.createAST(null); // parse
  }

}