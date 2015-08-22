package jribble_eclipse;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

public class AddNatureHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    IWorkbenchWindow win = HandlerUtil.getActiveWorkbenchWindow(event);
    Shell shell = win.getShell();
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
        newNatures[natures.length] = "jribble-eclipse.jribblenature";
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
