package jribble_eclipse;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class JribbleNature implements IProjectNature {

  private static final String BUILDER_ID = "jribble-eclipse.jribblebuilder";
  private IProject project;

  @Override
  public void configure() throws CoreException {
    IProjectDescription desc = project.getDescription();
    if (!hasBuilder(desc.getBuildSpec())) {
      // Add it before other builders.
      ICommand command = desc.newCommand();
      command.setBuilderName(BUILDER_ID);
      ICommand[] newCommands = new ICommand[desc.getBuildSpec().length + 1];
      System.arraycopy(desc.getBuildSpec(), 0, newCommands, 1, desc.getBuildSpec().length);
      newCommands[0] = command;
      desc.setBuildSpec(newCommands);
      project.setDescription(desc, null);
    }
  }

  @Override
  public void deconfigure() throws CoreException {
    IProjectDescription desc = project.getDescription();
    ICommand[] commands = desc.getBuildSpec();
    if (hasBuilder(commands)) {
      ICommand[] newCommands = new ICommand[commands.length - 1];
      for (int i = 0, j = 0; i < commands.length; i++) {
        if (!commands[i].getBuilderName().equals(BUILDER_ID)) {
          newCommands[j++] = commands[i];
        }
      }
      desc.setBuildSpec(newCommands);
      project.setDescription(desc, null);
    }
  }

  @Override
  public IProject getProject() {
    return project;
  }

  @Override
  public void setProject(IProject project) {
    this.project = project;
  }

  private static boolean hasBuilder(ICommand[] commands) {
    for (int i = 0; i < commands.length; ++i) {
      if (commands[i].getBuilderName().equals(BUILDER_ID)) {
        return true;
      }
    }
    return false;
  }

}
