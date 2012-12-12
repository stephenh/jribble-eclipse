package jribble_eclipse;

import com.google.jribble.JribbleProtos.DeclaredType;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JribbleVisitorTest {

  @Test
  public void testField() throws Exception {
    DeclaredType t = compile(//
      "package p;",
      "public class X {",
      "  public int i;",
      "}").get(0);
    assertText(t, //
      "name {",
      "  pkg: \"p\"",
      "  name: \"X\"",
      "}",
      "modifiers {",
      "  isPublic: true",
      "}",
      "member {",
      "  type: Field",
      "  modifiers {",
      "    isPublic: true",
      "  }",
      "  fieldDef {",
      "    tpe {",
      "      type: Primitive",
      "      primitiveType: Int",
      "    }",
      "    name: \"i\"",
      "  }",
      "}");
  }
  
  @Test
  public void testMethod() throws Exception {
    DeclaredType t = compile(//
      "package p;",
      "public class X {",
      "  public void foo() {",
      "  };",
      "}").get(0);
    assertText(t, //
      "name {",
      "  pkg: \"p\"",
      "  name: \"X\"",
      "}",
      "modifiers {",
      "  isPublic: true",
      "}",
      "member {",
      "  type: Method",
      "  modifiers {",
      "    isPublic: true",
      "  }",
      "  method {",
      "    name: \"foo\"",
      "    returnType {",
      "      type: Void",
      "    }",
      "    body {",
      "      type: Block",
      "      block {",
      "      }",
      "    }",
      "  }",
      "}");
  }
  
  @Test
  public void testMethodWithStatements() throws Exception {
    DeclaredType t = compile(//
      "package p;",
      "public class X {",
      "  public void foo() {",
      "    while (true) {",
      "       System.out.println(\"a\");",
      "    }",
      "  };",
      "}").get(0);
    assertText(t, //
      "name {",
      "  pkg: \"p\"",
      "  name: \"X\"",
      "}",
      "modifiers {",
      "  isPublic: true",
      "}",
      "member {",
      "  type: Method",
      "  modifiers {",
      "    isPublic: true",
      "  }",
      "  method {",
      "    name: \"foo\"",
      "    returnType {",
      "      type: Void",
      "    }",
      "    body {",
      "      type: Block",
      "      block {",
      "        statement {",
      "          type: While",
      "          whileStat {",
      "            condition {",
      "              type: Literal",
      "              literal {",
      "                type: Boolean",
      "                boolValue: true",
      "              }",
      "            }",
      "            body {",
      "              type: Block",
      "              block {",
      "                statement {",
      "                  type: Expr",
      "                  expr {",
      "                    type: MethodCall",
      "                    methodCall {",
      "                      receiver {",
      "                        type: FieldRef",
      "                        fieldRef {",
      "                          enclosingType {",
      "                            name: \"System.out\"",
      "                          }",
      "                          name: \"out\"",
      "                          tpe {",
      "                            type: Named",
      "                            namedType {",
      "                              pkg: \"java.io\"",
      "                              name: \"PrintStream\"",
      "                            }",
      "                          }",
      "                        }",
      "                      }",
      "                      signature {",
      "                        name: \"println\"",
      "                        owner {",
      "                          pkg: \"java.io\"",
      "                          name: \"PrintStream\"",
      "                        }",
      "                        paramType {",
      "                          type: Named",
      "                          namedType {",
      "                            pkg: \"java.lang\"",
      "                            name: \"String\"",
      "                          }",
      "                        }",
      "                        returnType {",
      "                          type: Void",
      "                        }",
      "                      }",
      "                      argument {",
      "                        type: Literal",
      "                        literal {",
      "                          type: String",
      "                          stringValue: \"a\"",
      "                        }",
      "                      }",
      "                    }",
      "                  }",
      "                }",
      "              }",
      "            }",
      "          }",
      "        }",
      "      }",
      "    }",
      "  }",
      "}");
  }

  private static List<DeclaredType> compile(String... lines) {
    ASTNode n = runConversion(AST.JLS4, join(lines), true, true, true, "X.java");
    JribbleVisitor v = new JribbleVisitor();
    n.accept(v);
    return v.getTypes();
  }

  private static void assertText(Message message, String... lines) throws Exception {
    StringBuilder sb = new StringBuilder();
    TextFormat.print(message, sb);
    Assert.assertEquals(join(lines), sb.toString());
  }

  private static String join(String... lines) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      sb.append(lines[i]);
      sb.append("\n");
    }
    return sb.toString();
  }

  private static ASTNode runConversion(
    int astLevel,
    String source,
    boolean resolveBindings,
    boolean statementsRecovery,
    boolean bindingsRecovery,
    String unitName) {
    ASTParser parser = ASTParser.newParser(astLevel);
    parser.setSource(source.toCharArray());
    parser.setEnvironment(null, null, null, true);
    parser.setResolveBindings(resolveBindings);
    parser.setStatementsRecovery(statementsRecovery);
    parser.setBindingsRecovery(bindingsRecovery);
    // parser.setCompilerOptions();
    parser.setUnitName(unitName);
    return parser.createAST(null);
  }

}
