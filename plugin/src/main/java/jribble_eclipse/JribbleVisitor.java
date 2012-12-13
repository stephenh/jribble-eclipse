package jribble_eclipse;

import com.google.jribble.JribbleProtos.Block;
import com.google.jribble.JribbleProtos.Declaration;
import com.google.jribble.JribbleProtos.Declaration.DeclarationType;
import com.google.jribble.JribbleProtos.Expr.ExprType;
import com.google.jribble.JribbleProtos.FieldRef;
import com.google.jribble.JribbleProtos.Literal.LiteralType;
import com.google.jribble.JribbleProtos.DeclaredType;
import com.google.jribble.JribbleProtos.Expr;
import com.google.jribble.JribbleProtos.FieldDef;
import com.google.jribble.JribbleProtos.GlobalName;
import com.google.jribble.JribbleProtos.Literal;
import com.google.jribble.JribbleProtos.Method;
import com.google.jribble.JribbleProtos.MethodCall;
import com.google.jribble.JribbleProtos.MethodSignature;
import com.google.jribble.JribbleProtos.Modifiers;
import com.google.jribble.JribbleProtos.NewObject;
import com.google.jribble.JribbleProtos.PrimitiveType;
import com.google.jribble.JribbleProtos.Statement;
import com.google.jribble.JribbleProtos.Type;
import com.google.jribble.JribbleProtos.While;
import com.google.jribble.JribbleProtos.Statement.StatementType;
import com.google.jribble.JribbleProtos.Type.TypeType;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/** Looks at a JDT AST and transforms it into a jribble AST. */
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
      org.eclipse.jdt.core.dom.Block _block = (org.eclipse.jdt.core.dom.Block) statement;
      Block.Builder b = Block.newBuilder();
      for (org.eclipse.jdt.core.dom.Statement stmt : (List<org.eclipse.jdt.core.dom.Statement>) _block.statements()) {
        b.addStatement(toStatement(stmt));
      }
      return Statement.newBuilder().setType(StatementType.Block).setBlock(b.build()).build();
    } else if (statement instanceof WhileStatement) {
      WhileStatement _while = (WhileStatement) statement;
      While.Builder b = While.newBuilder() //
        .setBody(toStatement(_while.getBody()))
        .setCondition(toExpression(_while.getExpression()));
      return Statement.newBuilder().setType(StatementType.While).setWhileStat(b.build()).build();
    } else if (statement instanceof ExpressionStatement) {
      ExpressionStatement _expr = (ExpressionStatement) statement;
      return Statement.newBuilder() //
        .setType(StatementType.Expr)
        .setExpr(toExpression(_expr.getExpression()))
        .build();
    } else {
      throw new IllegalStateException("Unhandled statement " + statement);
    }
  }

  @SuppressWarnings("unchecked")
  private static Expr toExpression(Expression expression) {
    if (expression instanceof MethodInvocation) {
      MethodInvocation _method = (MethodInvocation) expression;
      MethodCall.Builder b = MethodCall.newBuilder();
      for (Expression argument : (List<Expression>) _method.arguments()) {
        b.addArgument(toExpression(argument));
      }
      b.setReceiver(toExpression(_method.getExpression()));
      b.setSignature(toSignature(_method.resolveMethodBinding()));
      return Expr.newBuilder().setType(ExprType.MethodCall).setMethodCall(b.build()).build();
    } else if (expression instanceof StringLiteral) {
      StringLiteral _string = (StringLiteral) expression;
      return Expr.newBuilder() //
        .setType(ExprType.Literal)
        .setLiteral(Literal.newBuilder() //
          .setType(LiteralType.String)
          .setStringValue(_string.getLiteralValue())
          .build())
        .build();
    } else if (expression instanceof FieldAccess) {
      FieldAccess _field = (FieldAccess) expression;
      return Expr.newBuilder() //
        .setType(ExprType.FieldRef)
        .setFieldRef(FieldRef.newBuilder() //
          .setName(_field.getName().getFullyQualifiedName())
          .build())
        .build();
    } else if (expression instanceof QualifiedName) {
      QualifiedName _name = (QualifiedName) expression;
      return Expr.newBuilder() //
        .setType(ExprType.FieldRef)
        .setFieldRef(FieldRef.newBuilder() //
          .setEnclosingType(GlobalName.newBuilder().setName(_name.getFullyQualifiedName()).build())
          .setTpe(toType(_name.resolveTypeBinding()))
          .setName(_name.getName().getFullyQualifiedName())
          .build())
        .build();
    } else if (expression instanceof BooleanLiteral) {
      BooleanLiteral _bool = (BooleanLiteral) expression;
      return Expr.newBuilder() //
        .setType(ExprType.Literal)
        .setLiteral(Literal.newBuilder() //
          .setType(LiteralType.Boolean)
          .setBoolValue(_bool.booleanValue())
          .build())
        .build();
    } else if (expression instanceof NumberLiteral) {
      NumberLiteral _number = (NumberLiteral) expression;
      return Expr.newBuilder() //
        .setType(ExprType.Literal)
        .setLiteral(Literal.newBuilder() //
          .setType(LiteralType.Int) // TODO What if it's not an int? Can it be?
          .setIntValue(Integer.parseInt(_number.getToken()))
          .build())
        .build();
    } else if (expression instanceof ClassInstanceCreation) {
      ClassInstanceCreation _new = (ClassInstanceCreation) expression;
      NewObject.Builder b = NewObject.newBuilder();
      b.setClazz(toGlobalName(_new.resolveTypeBinding()));
      b.setSignature(toSignature(_new.resolveConstructorBinding()));
      for (Expression param : (List<Expression>) _new.arguments()) {
        b.addArgument(toExpression(param));
      }
      return Expr.newBuilder().setType(ExprType.NewObject).setNewObject(b.build()).build();
    } else {
      throw new IllegalStateException("Unhandled expression " + expression.getClass() + " " + expression);
    }
  }

  // assume the ITypeBinding is a class, given the caller called us
  private static GlobalName toGlobalName(ITypeBinding binding) {
    GlobalName.Builder b = GlobalName.newBuilder();
    b.setPkg(binding.getPackage().getName());
    b.setName(binding.getName());
    return b.build();
  }

  private static MethodSignature toSignature(IMethodBinding binding) {
    MethodSignature.Builder b = MethodSignature.newBuilder();
    b.setName(binding.getName());
    b.setOwner(toGlobalName(binding.getDeclaringClass()));
    for (ITypeBinding parameter : binding.getParameterTypes()) {
      b.addParamType(toType(parameter));
    }
    b.setReturnType(toType(binding.getReturnType()));
    return b.build();
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
    } else {
      throw new IllegalStateException("Unhandled type " + type);
    }
  }

  private static Type toType(ITypeBinding type) {
    if (type.isPrimitive()) {
      PrimitiveType t = null;
      String c = type.getBinaryName();
      if (c.equals("I")) {
        t = PrimitiveType.Int;
      } else if (c.equals("B")) {
        t = PrimitiveType.Boolean;
      } else if (c.equals("B")) {
        t = PrimitiveType.Byte;
      } else if (c.equals("C")) {
        t = PrimitiveType.Char;
      } else if (c.equals("D")) {
        t = PrimitiveType.Double;
      } else if (c.equals("F")) {
        t = PrimitiveType.Float;
      } else if (c.equals("L")) {
        t = PrimitiveType.Long;
      } else if (c.equals("S")) {
        t = PrimitiveType.Short;
      } else if (c.equals("V")) {
        // jribble doesn't treat this as a primitive
        return Type.newBuilder().setType(TypeType.Void).build();
      }
      return Type.newBuilder().setType(TypeType.Primitive).setPrimitiveType(t).build();
    } else if (type.isClass()) {
      return Type.newBuilder() //
        .setType(TypeType.Named)
        .setNamedType(GlobalName.newBuilder() //
          .setPkg(type.getPackage().getName())
          .setName(type.getName())
          .build())
        .build();
    } else {
      throw new IllegalStateException("Unhandled type " + type);
    }
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
