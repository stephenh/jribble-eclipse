package jribble_eclipse;

import com.google.jribble.JribbleProtos.GlobalName;
import com.google.jribble.JribbleProtos.Method;
import com.google.jribble.JribbleProtos.Type;
import com.google.jribble.JribbleProtos.Type.TypeType;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

class JribbleVisitor extends ASTVisitor {
  
  private final CompilationUnit unit;
  
  JribbleVisitor(CompilationUnit unit) {
    this.unit = unit;
  }

  @Override
  public void preVisit(ASTNode node) {
    System.out.println("   visiting: " + node.getClass());
  }

  @Override
  public boolean visit(PackageDeclaration node) {
    return super.visit(node);
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    GlobalName globalName = GlobalName.newBuilder().setPkg("foo").setName("Bar").build();
    Type returnType = Type.newBuilder().setType(TypeType.Named).setNamedType(globalName).build();
    Method m =
        Method.newBuilder().setName(node.getName().getFullyQualifiedName()).setReturnType(
            returnType).build();
    System.out.println("Found " + node + " and built " + m.toString());
    return false;
  }

}