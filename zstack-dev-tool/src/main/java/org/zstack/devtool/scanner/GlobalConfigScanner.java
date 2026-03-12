package org.zstack.devtool.scanner;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import org.zstack.devtool.model.GlobalConfigInfo;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class GlobalConfigScanner {

    private final JavaParser parser = new JavaParser();

    public List<GlobalConfigInfo> scan(List<Path> sourceDirs) {
        List<GlobalConfigInfo> results = new ArrayList<>();

        for (Path dir : sourceDirs) {
            if (!Files.isDirectory(dir)) continue;
            try {
                scanDirectory(dir, results);
            } catch (IOException e) {
                System.err.println("WARN: Failed to scan " + dir + ": " + e.getMessage());
            }
        }

        return results;
    }

    private void scanDirectory(Path dir, List<GlobalConfigInfo> results) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                String fileName = path.getFileName().toString();
                if (fileName.endsWith(".java") && fileName.contains("GlobalConfig")) {
                    try {
                        scanFile(path, results);
                    } catch (Exception e) {
                        System.err.println("WARN: Failed to parse " + path + ": " + e.getMessage());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void scanFile(Path path, List<GlobalConfigInfo> results) throws IOException {
        ParseResult<CompilationUnit> parseResult = parser.parse(path);
        if (!parseResult.isSuccessful() || !parseResult.getResult().isPresent()) {
            return;
        }

        CompilationUnit cu = parseResult.getResult().get();

        for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (!hasAnnotation(cls, "GlobalConfigDefinition")) continue;

            String category = extractCategory(cls);
            if (category == null) continue;

            for (FieldDeclaration field : cls.getFields()) {
                if (!field.isStatic()) continue;
                if (!isGlobalConfigType(field)) continue;
                if (!hasAnnotation(field, "GlobalConfigDef")) continue;

                GlobalConfigInfo info = extractConfigInfo(field, category, path.toString());
                if (info != null) {
                    results.add(info);
                }
            }
        }
    }

    private GlobalConfigInfo extractConfigInfo(FieldDeclaration field, String category, String sourceFile) {
        GlobalConfigInfo info = new GlobalConfigInfo();
        info.setCategory(category);
        info.setSourceFile(sourceFile);

        // field name
        if (!field.getVariables().isEmpty()) {
            info.setFieldName(field.getVariable(0).getNameAsString());
        }

        // extract config name from: new GlobalConfig(CATEGORY, "name")
        String configName = extractConfigName(field);
        if (configName == null) return null;
        info.setName(configName);

        // @GlobalConfigDef
        extractGlobalConfigDef(field, info);

        // @GlobalConfigValidation
        extractGlobalConfigValidation(field, info);

        // @BindResourceConfig
        extractBindResourceConfig(field, info);

        return info;
    }

    private String extractConfigName(FieldDeclaration field) {
        for (VariableDeclarator var : field.getVariables()) {
            if (!var.getInitializer().isPresent()) continue;
            Expression init = var.getInitializer().get();

            if (init.isObjectCreationExpr()) {
                ObjectCreationExpr ctor = init.asObjectCreationExpr();
                List<Expression> args = ctor.getArguments();
                if (args.size() >= 2) {
                    Expression nameArg = args.get(1);
                    if (nameArg.isStringLiteralExpr()) {
                        return nameArg.asStringLiteralExpr().getValue();
                    }
                }
            }
        }
        return null;
    }

    private void extractGlobalConfigDef(FieldDeclaration field, GlobalConfigInfo info) {
        field.getAnnotationByName("GlobalConfigDef").ifPresent(ann -> {
            if (ann.isNormalAnnotationExpr()) {
                NormalAnnotationExpr normal = ann.asNormalAnnotationExpr();
                for (MemberValuePair pair : normal.getPairs()) {
                    String key = pair.getNameAsString();
                    Expression value = pair.getValue();

                    switch (key) {
                        case "type":
                            info.setType(resolveTypeClass(value));
                            break;
                        case "defaultValue":
                            if (value.isStringLiteralExpr()) {
                                info.setDefaultValue(value.asStringLiteralExpr().getValue());
                            }
                            break;
                        case "description":
                            if (value.isStringLiteralExpr()) {
                                info.setDescription(value.asStringLiteralExpr().getValue());
                            }
                            break;
                        case "validatorRegularExpression":
                            if (value.isStringLiteralExpr()) {
                                info.setValidatorRegularExpression(value.asStringLiteralExpr().getValue());
                            }
                            break;
                    }
                }
            }
        });

        // defaults
        if (info.getType() == null) info.setType("java.lang.String");
        if (info.getDefaultValue() == null) info.setDefaultValue("");
        if (info.getDescription() == null) info.setDescription("");
    }

    private void extractGlobalConfigValidation(FieldDeclaration field, GlobalConfigInfo info) {
        field.getAnnotationByName("GlobalConfigValidation").ifPresent(ann -> {
            if (ann.isNormalAnnotationExpr()) {
                NormalAnnotationExpr normal = ann.asNormalAnnotationExpr();
                for (MemberValuePair pair : normal.getPairs()) {
                    String key = pair.getNameAsString();
                    Expression value = pair.getValue();

                    switch (key) {
                        case "numberGreaterThan":
                            info.setNumberGreaterThan(parseLong(value));
                            break;
                        case "numberLessThan":
                            info.setNumberLessThan(parseLong(value));
                            break;
                        case "inNumberRange":
                            info.setInNumberRange(parseLongArray(value));
                            break;
                        case "validValues":
                            info.setValidValues(parseStringArray(value));
                            break;
                    }
                }
            }
            // @GlobalConfigValidation with no params = use defaults (already set)
        });
    }

    private void extractBindResourceConfig(FieldDeclaration field, GlobalConfigInfo info) {
        field.getAnnotationByName("BindResourceConfig").ifPresent(ann -> {
            List<String> resources = new ArrayList<>();

            if (ann.isSingleMemberAnnotationExpr()) {
                Expression value = ann.asSingleMemberAnnotationExpr().getMemberValue();
                extractClassReferences(value, resources);
            } else if (ann.isNormalAnnotationExpr()) {
                NormalAnnotationExpr normal = ann.asNormalAnnotationExpr();
                for (MemberValuePair pair : normal.getPairs()) {
                    if ("value".equals(pair.getNameAsString())) {
                        extractClassReferences(pair.getValue(), resources);
                    }
                }
            }

            info.setBindResources(resources);
        });
    }

    private void extractClassReferences(Expression expr, List<String> resources) {
        if (expr.isArrayInitializerExpr()) {
            for (Expression element : expr.asArrayInitializerExpr().getValues()) {
                extractClassReferences(element, resources);
            }
        } else if (expr.isClassExpr()) {
            resources.add(expr.asClassExpr().getType().asString());
        } else if (expr.isFieldAccessExpr()) {
            // e.g., VmInstanceVO.class
            String text = expr.toString();
            if (text.endsWith(".class")) {
                resources.add(text.substring(0, text.length() - 6));
            }
        }
    }

    private String resolveTypeClass(Expression expr) {
        // Handle: String.class, Long.class, Integer.class, Boolean.class, etc.
        String text = expr.toString();
        if (text.endsWith(".class")) {
            String simpleName = text.substring(0, text.length() - 6);
            switch (simpleName) {
                case "String":  return "java.lang.String";
                case "Long":    return "java.lang.Long";
                case "Integer": return "java.lang.Integer";
                case "Boolean": return "java.lang.Boolean";
                case "Float":   return "java.lang.Float";
                case "Double":  return "java.lang.Double";
                default:        return simpleName;
            }
        }
        return "java.lang.String";
    }

    private String extractCategory(ClassOrInterfaceDeclaration cls) {
        // Look for: public static final String CATEGORY = "xxx";
        for (FieldDeclaration field : cls.getFields()) {
            for (VariableDeclarator var : field.getVariables()) {
                if ("CATEGORY".equals(var.getNameAsString()) && var.getInitializer().isPresent()) {
                    Expression init = var.getInitializer().get();
                    if (init.isStringLiteralExpr()) {
                        return init.asStringLiteralExpr().getValue();
                    }
                }
            }
        }
        return null;
    }

    private boolean hasAnnotation(NodeWithAnnotations<?> node, String name) {
        return node.getAnnotationByName(name).isPresent();
    }

    private boolean isGlobalConfigType(FieldDeclaration field) {
        String typeStr = field.getElementType().asString();
        return "GlobalConfig".equals(typeStr) || typeStr.endsWith(".GlobalConfig");
    }

    private long parseLong(Expression expr) {
        if (expr.isLongLiteralExpr()) {
            return expr.asLongLiteralExpr().asLong();
        }
        if (expr.isIntegerLiteralExpr()) {
            return expr.asIntegerLiteralExpr().asInt();
        }
        if (expr.isUnaryExpr() && expr.asUnaryExpr().getOperator() == UnaryExpr.Operator.MINUS) {
            return -parseLong(expr.asUnaryExpr().getExpression());
        }
        // Handle Long.MIN_VALUE, Long.MAX_VALUE etc.
        String text = expr.toString();
        if (text.contains("MIN_VALUE")) return Long.MIN_VALUE;
        if (text.contains("MAX_VALUE")) return Long.MAX_VALUE;
        try {
            return Long.parseLong(text.replaceAll("[Ll]$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long[] parseLongArray(Expression expr) {
        if (expr.isArrayInitializerExpr()) {
            List<Expression> values = expr.asArrayInitializerExpr().getValues();
            long[] result = new long[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = parseLong(values.get(i));
            }
            return result;
        }
        return new long[0];
    }

    private String[] parseStringArray(Expression expr) {
        if (expr.isArrayInitializerExpr()) {
            List<Expression> values = expr.asArrayInitializerExpr().getValues();
            String[] result = new String[values.size()];
            for (int i = 0; i < values.size(); i++) {
                Expression v = values.get(i);
                if (v.isStringLiteralExpr()) {
                    result[i] = v.asStringLiteralExpr().getValue();
                } else {
                    result[i] = v.toString();
                }
            }
            return result;
        }
        return new String[0];
    }
}
