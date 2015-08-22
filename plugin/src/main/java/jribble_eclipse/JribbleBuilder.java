package jribble_eclipse;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

public class JribbleBuilder extends IncrementalProjectBuilder {

  @Override
  protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
    if (!getProject().isNatureEnabled("org.eclipse.jdt.core.javanature")) {
      return null;
    }
    IJavaProject javaProject = JavaCore.create(getProject());

    launch(javaProject, "foo.Foo", monitor);

    //    final IClasspathEntry[] resolvedClasspath = javaProject.getResolvedClasspath(true);
    //    for (IClasspathEntry classpathEntry : resolvedClasspath) {
    //      int kind2 = classpathEntry.getEntryKind();
    //      String path = classpathEntry.getPath().toOSString();
    //      System.out.println(path + kind2);
    //    }

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
          System.out.println("changed: " + delta.getResource().getRawLocation());
          if ("java".equals(delta.getResource().getFullPath().getFileExtension())) {
            for (IPackageFragmentRoot root : javaProject.getPackageFragmentRoots()) {
              if (root.getPath().isPrefixOf(delta.getResource().getFullPath())) {
                IPath relative = delta.getResource().getFullPath().removeFirstSegments(root.getPath().segmentCount());
                IJavaElement element = javaProject.findElement(relative);
                if (element instanceof ICompilationUnit) {
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

  void launch(IJavaProject project, String main, IProgressMonitor monitor) throws CoreException {
    IVMInstall vm = JavaRuntime.getVMInstall(project);
    if (vm == null) {
      vm = JavaRuntime.getDefaultVMInstall();
    }
    IVMRunner vmr = vm.getVMRunner(ILaunchManager.RUN_MODE);
    String[] cp = JavaRuntime.computeDefaultRuntimeClassPath(project);

    URL[] urls = new URL[cp.length];
    for (int i = 0; i < cp.length; i++) {
      try {
        String suffix = new File(cp[i]).isDirectory() ? "/" : "";
        urls[i] = new URL("file://" + cp[i] + suffix);
      } catch (MalformedURLException e) {
      }
    }
    URLClassLoader u = new URLClassLoader(urls);
    try {
      Class<?> c = Class.forName("foo.Foo", true, u);
      Method m = c.getMethod("main", new Class[] { String[].class });
      m.invoke(null, new Object[] { new String[] {} });
      u.close();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }

    /*
    VMRunnerConfiguration config = new VMRunnerConfiguration(main, cp);
    ILaunch launch = new Launch(null, ILaunchManager.RUN_MODE, null);
    monitor.subTask("Running foo.Foo");
    vmr.run(config, launch, monitor);
    monitor.worked(1);
    while (!launch.isTerminated()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ie) {
      }
    }
    IProcess[] processes = launch.getProcesses();
    String outputStream = processes[0].getStreamsProxy().getOutputStreamMonitor().getContents();
    logStream(outputStream);
    String errorStream = processes[0].getStreamsProxy().getErrorStreamMonitor().getContents();
    logStream(errorStream);
    */
  }

  private void logStream(String outputStream) {
    if (!PlatformUI.isWorkbenchRunning()) {
      return;
    }
    try {
      MessageConsole console = findConsole("Jribble");
      IWorkbench workbench = PlatformUI.getWorkbench();
      IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
      if (workbenchWindow != null) {
        IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
        IConsoleView consoleView = (IConsoleView) workbenchPage.showView(IConsoleConstants.ID_CONSOLE_VIEW);
        consoleView.display(console);
        IOConsoleOutputStream consoleOutputStream = console.newOutputStream();
        consoleOutputStream.write(outputStream);
        consoleOutputStream.close();
      }
    } catch (PartInitException pie) {
    } catch (IOException ioe) {
    }
  }

  private static MessageConsole findConsole(String name) {
    ConsolePlugin plugin = ConsolePlugin.getDefault();
    IConsoleManager conMan = plugin.getConsoleManager();
    IConsole[] existing = conMan.getConsoles();
    for (int i = 0; i < existing.length; i++)
      if (name.equals(existing[i].getName()))
        return (MessageConsole) existing[i];
    //no console found, so create a new one
    MessageConsole myConsole = new MessageConsole(name, null);
    conMan.addConsoles(new IConsole[] { myConsole });
    return myConsole;
  }
}
