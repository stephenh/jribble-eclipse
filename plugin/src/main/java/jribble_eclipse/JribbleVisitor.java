package jribble_eclipse;

import com.google.jribble.JribbleProtos.Block;
import com.google.jribble.JribbleProtos.Declaration;
import com.google.jribble.JribbleProtos.Declaration.DeclarationType;
import com.google.jribble.JribbleProtos.DeclaredType;
import com.google.jribble.JribbleProtos.FieldDef;
import com.google.jribble.JribbleProtos.GlobalName;
import com.google.jribble.JribbleProtos.Method;
import com.google.jribble.JribbleProtos.Modifiers;
import com.google.jribble.JribbleProtos.PrimitiveType;
import com.google.jribble.JribbleProtos.Statement;
import com.google.jribble.JribbleProtos.Type;
import com.google.jribble.JribbleProtos.Statement.StatementType;
import com.google.jribble.JribbleProtos.Type.TypeType;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class JribbleVisitor extends ASTVisitor {

  private final List<DeclaredType> types = new ArrayList<DeclaredType>();

  public List<DeclaredType> getTypes() {
    return types;
  }

  @Override
  public void preVisit(ASTNode node) {
    System.out.println("   visiting: " + node.getClass());
  }

  // Called for visiting the top-level
  @Override
  @SuppressWarnings("unchecked")
  public boolean visit(TypeDeclaration node) {
    // Assume this is a top-level type
    DeclaredType.Builder b = DeclaredType.newBuilder()//
      .setName(GlobalName.newBuilder()//
        .setPkg(((CompilationUnit) node.getParent()).getPackage().getName().getFullyQualifiedName())
        .setName(node.getName().getIdentifier())
        .build())
      .setModifiers(toModifiers(node.getModifiers()));
    // fields
    for (FieldDeclaration field : node.getFields()) {
      for (VariableDeclarationFragment fragment : (List<VariableDeclarationFragment>) field.fragments()) {
        b.addMember(Declaration.newBuilder() //
          .setType(DeclarationType.Field)
          .setModifiers(toModifiers(field.getModifiers()))
          .setFieldDef(FieldDef.newBuilder() //
            .setTpe(toType(field.getType()))
            .setName(fragment.getName().getFullyQualifiedName())
            .build())
          .build());
      }
    }
    // methods
    for (MethodDeclaration method : node.getMethods()) {
      b.addMember(Declaration.newBuilder() //
        .setType(DeclarationType.Method)
        .setModifiers(toModifiers(method.getModifiers()))
        .setMethod(Method.newBuilder() //
          .setName(method.getName().getFullyQualifiedName())
          .setReturnType(toType(method.getReturnType2()))
          .setBody(toStatement(method.getBody()))
          .build())
        .build());
    }
    types.add(b.build());
    return false;
  }

  @SuppressWarnings("unchecked")
  private static Statement toStatement(org.eclipse.jdt.core.dom.Statement statement) {
    if (statement instanceof org.eclipse.jdt.core.dom.Block) {
      Block.Builder b = Block.newBuilder();
      org.eclipse.jdt.core.dom.Block block = (org.eclipse.jdt.core.dom.Block) statement;
      for (org.eclipse.jdt.core.dom.Statement stmt : (List<org.eclipse.jdt.core.dom.Statement>) block.statements()) {
        b.addStatement(toStatement(stmt));
      }
      return Statement.newBuilder().setType(StatementType.Block).setBlock(b.build()).build();
    }
    return null;
  }

  private static Type toType(org.eclipse.jdt.core.dom.Type type) {
    if (type.isPrimitiveType()) {
      org.eclipse.jdt.core.dom.PrimitiveType.Code c = ((org.eclipse.jdt.core.dom.PrimitiveType) type).getPrimitiveTypeCode();
      PrimitiveType t = null;
      if (c == org.eclipse.jdt.core.dom.PrimitiveType.INT) {
        t = PrimitiveType.Int;
      } else if (c == org.eclipse.jdt.core.dom.PrimitiveType.BOOLEAN) {
        t = PrimitiveType.Boolean;
      } else if (c == org.eclipse.jdt.core.dom.PrimitiveType.BYTE) {
        t = PrimitiveType.Byte;
      } else if (c == org.eclipse.jdt.core.dom.PrimitiveType.CHAR) {
        t = PrimitiveType.Char;
      } else if (c == org.eclipse.jdt.core.dom.PrimitiveType.DOUBLE) {
        t = PrimitiveType.Double;
      } else if (c == org.eclipse.jdt.core.dom.PrimitiveType.FLOAT) {
        t = PrimitiveType.Float;
      } else if (c == org.eclipse.jdt.core.dom.PrimitiveType.LONG) {
        t = PrimitiveType.Long;
      } else if (c == org.eclipse.jdt.core.dom.PrimitiveType.SHORT) {
        t = PrimitiveType.Short;
      } else if (c == org.eclipse.jdt.core.dom.PrimitiveType.VOID) {
        // jribble doesn't treat this as a primitive
        return Type.newBuilder().setType(TypeType.Void).build();
      }
      return Type.newBuilder().setType(TypeType.Primitive).setPrimitiveType(t).build();
    } else if (type.isSimpleType()) {
      return Type.newBuilder().setType(TypeType.Named).build();
    }
    return null;
  }

  private static Modifiers toModifiers(int modifiers) {
    Modifiers.Builder b = Modifiers.newBuilder();
    if ((modifiers & Modifier.FINAL) > 0) {
      b.setIsFinal(true);
    }
    if ((modifiers & Modifier.PUBLIC) > 0) {
      b.setIsPublic(true);
    }
    if ((modifiers & Modifier.PRIVATE) > 0) {
      b.setIsPrivate(true);
    }
    return b.build();
  }

  @Override
  public boolean visit(MethodDeclaration node) {
    GlobalName globalName = GlobalName.newBuilder().setPkg("foo").setName("Bar").build();
    Type returnType = Type.newBuilder().setType(TypeType.Named).setNamedType(globalName).build();
    Method m = Method.newBuilder().setName(node.getName().getFullyQualifiedName()).setReturnType(returnType).build();
    System.out.println("Found " + node + " and built " + m.toString());
    return false;
  }

}
