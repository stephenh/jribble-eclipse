package jribble_eclipse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class SampleHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IWorkbenchWindow win = HandlerUtil.getActiveWorkbenchWindow(event);
    Shell shell = win.getShell();
    MessageDialog.openError(shell, "CLICKED", "HERE");

    ISelection s = win.getActivePage().getSelection();
    if (s instanceof ITreeSelection) {
      TreeSelection tree = (TreeSelection) s;
      TreePath treePath = tree.getPaths()[0];
      // The TreePath contains a series of segments in our usage:
      // o The first segment is usually a project
      // o The last segment generally refers to the file
      Object firstSegment = treePath.getFirstSegment();
      IProject project = (IProject) ((IAdaptable) firstSegment).getAdapter(IProject.class);

      try {
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        newNatures[natures.length] = "com.example.natures.mynature";
        description.setNatureIds(newNatures);
        project.setDescription(description, null);
      } catch (CoreException e) {
        // Something went wrong
      }
      MessageDialog.openError(shell, "CLICKED", "FOUND PROJECT " + project);
    }

    return null;
  }

}
