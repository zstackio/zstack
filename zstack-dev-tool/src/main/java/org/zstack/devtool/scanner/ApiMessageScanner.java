package org.zstack.devtool.scanner;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.zstack.devtool.model.ApiMessageInfo;
import org.zstack.devtool.model.ApiParamInfo;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class ApiMessageScanner {

    private final JavaParser parser = new JavaParser();
    // className -> source path, for inheritance resolution (keyed by simple name)
    // When duplicates exist, last-write wins, but we also keep a FQCN index
    private final Map<String, Path> classIndex = new HashMap<>();
    // FQCN (package.ClassName) -> source path, for precise lookup
    private final Map<String, Path> fqcnIndex = new HashMap<>();
    // className -> parsed CompilationUnit cache
    private final Map<String, CompilationUnit> cuCache = new HashMap<>();

    public List<ApiMessageInfo> scan(List<Path> sourceDirs) {
        // Phase 1: build class index (file name -> path)
        for (Path dir : sourceDirs) {
            if (!Files.isDirectory(dir)) continue;
            try {
                buildIndex(dir);
            } catch (IOException e) {
                System.err.println("WARN: Failed to index " + dir + ": " + e.getMessage());
            }
        }

        // Phase 2: scan for @RestRequest annotated classes
        List<ApiMessageInfo> results = new ArrayList<>();
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

    private void buildIndex(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                String fileName = path.getFileName().toString();
                if (fileName.endsWith(".java")) {
                    String className = fileName.substring(0, fileName.length() - 5);
                    // Derive FQCN from path: dir is a source root like .../src/main/java
                    // so relativize path to get package structure
                    String relativePath = dir.relativize(path).toString();
                    String fqcn = relativePath.replace('/', '.').replace('\\', '.');
                    if (fqcn.endsWith(".java")) {
                        fqcn = fqcn.substring(0, fqcn.length() - 5);
                    }
                    fqcnIndex.put(fqcn, path);
                    classIndex.put(className, path);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void scanDirectory(Path dir, List<ApiMessageInfo> results) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                String fileName = path.getFileName().toString();
                // Only scan API*.java files for @RestRequest
                if (fileName.startsWith("API") && fileName.endsWith(".java")) {
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

    private void scanFile(Path path, List<ApiMessageInfo> results) throws IOException {
        CompilationUnit cu = parseCached(path);
        if (cu == null) return;

        for (ClassOrInterfaceDeclaration cls : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (!cls.getAnnotationByName("RestRequest").isPresent()) continue;
            if (cls.isAbstract()) continue;
            if (cls.getAnnotationByName("NoSDK").isPresent()) continue;

            ApiMessageInfo info = new ApiMessageInfo();
            info.setClassName(cls.getNameAsString());
            info.setSourceFile(path.toString());

            // package
            cu.getPackageDeclaration().ifPresent(pd -> info.setPackageName(pd.getNameAsString()));

            // @RestRequest
            extractRestRequest(cls, info);

            // parent class analysis
            analyzeParentClass(cls, info);

            // @SuppressCredentialCheck
            info.setSuppressCredentialCheck(cls.getAnnotationByName("SuppressCredentialCheck").isPresent());

            // fields with @APIParam (own fields)
            List<ApiParamInfo> params = extractParams(cls);

            // inherited fields (from parent classes) - marked as inherited
            List<ApiParamInfo> inheritedParams = resolveInheritedParams(cls);
            for (ApiParamInfo p : inheritedParams) {
                p.setInherited(true);
            }
            params.addAll(inheritedParams);

            info.setParams(params);
            results.add(info);
        }
    }

    private void extractRestRequest(ClassOrInterfaceDeclaration cls, ApiMessageInfo info) {
        cls.getAnnotationByName("RestRequest").ifPresent(ann -> {
            if (ann.isNormalAnnotationExpr()) {
                NormalAnnotationExpr normal = ann.asNormalAnnotationExpr();
                for (MemberValuePair pair : normal.getPairs()) {
                    String key = pair.getNameAsString();
                    Expression value = pair.getValue();

                    switch (key) {
                        case "path":
                            if (value.isStringLiteralExpr())
                                info.setPath(value.asStringLiteralExpr().getValue());
                            break;
                        case "method":
                            info.setHttpMethod(resolveHttpMethod(value));
                            break;
                        case "responseClass":
                            info.setResponseClass(resolveClassName(value));
                            break;
                        case "parameterName":
                            if (value.isStringLiteralExpr())
                                info.setParameterName(value.asStringLiteralExpr().getValue());
                            break;
                        case "isAction":
                            if (value.isBooleanLiteralExpr())
                                info.setAction(value.asBooleanLiteralExpr().getValue());
                            break;
                        case "optionalPaths":
                            info.setOptionalPaths(parseStringList(value));
                            break;
                    }
                }
            }
        });

        // defaults
        if (info.getParameterName() == null) info.setParameterName("params");
        if (info.getHttpMethod() == null) info.setHttpMethod("POST");
    }

    private List<ApiParamInfo> extractParams(ClassOrInterfaceDeclaration cls) {
        List<ApiParamInfo> params = new ArrayList<>();

        for (FieldDeclaration field : cls.getFields()) {
            if (field.isStatic()) continue;

            boolean hasApiParam = field.getAnnotationByName("APIParam").isPresent();
            boolean hasApiNoSee = field.getAnnotationByName("APINoSee").isPresent();

            // SDK only includes fields with @APIParam (excluding @APINoSee)
            if (!hasApiParam) continue;
            if (hasApiNoSee) continue;

            for (VariableDeclarator var : field.getVariables()) {
                ApiParamInfo param = new ApiParamInfo();
                param.setFieldName(var.getNameAsString());
                param.setFieldType(resolveFieldType(field));
                param.setNoSee(hasApiNoSee);

                if (hasApiParam) {
                    extractApiParam(field, param);
                }

                params.add(param);
            }
        }

        return params;
    }

    private void extractApiParam(FieldDeclaration field, ApiParamInfo param) {
        field.getAnnotationByName("APIParam").ifPresent(ann -> {
            if (ann.isNormalAnnotationExpr()) {
                NormalAnnotationExpr normal = ann.asNormalAnnotationExpr();
                for (MemberValuePair pair : normal.getPairs()) {
                    String key = pair.getNameAsString();
                    Expression value = pair.getValue();

                    switch (key) {
                        case "required":
                            if (value.isBooleanLiteralExpr())
                                param.setRequired(value.asBooleanLiteralExpr().getValue());
                            break;
                        case "maxLength":
                            param.setMaxLength(parseIntValue(value));
                            break;
                        case "minLength":
                            param.setMinLength(parseIntValue(value));
                            break;
                        case "validRegexValues":
                            if (value.isStringLiteralExpr())
                                param.setValidRegexValues(value.asStringLiteralExpr().getValue());
                            break;
                        case "validValues":
                            param.setValidValues(parseStringArray(value));
                            break;
                        case "nonempty":
                            if (value.isBooleanLiteralExpr())
                                param.setNonempty(value.asBooleanLiteralExpr().getValue());
                            break;
                        case "nullElements":
                            if (value.isBooleanLiteralExpr())
                                param.setNullElements(value.asBooleanLiteralExpr().getValue());
                            break;
                        case "emptyString":
                            if (value.isBooleanLiteralExpr())
                                param.setEmptyString(value.asBooleanLiteralExpr().getValue());
                            break;
                        case "noTrim":
                            if (value.isBooleanLiteralExpr())
                                param.setNoTrim(value.asBooleanLiteralExpr().getValue());
                            break;
                        case "numberRange":
                            param.setNumberRange(parseLongArray(value));
                            break;
                    }
                }
            }
            // @APIParam with no explicit required → default is true
        });
    }

    private void analyzeParentClass(ClassOrInterfaceDeclaration cls, ApiMessageInfo info) {
        for (ClassOrInterfaceType parent : cls.getExtendedTypes()) {
            String parentName = parent.getNameAsString();
            info.setParentClass(parentName);

            // Check if it's a query message
            if (parentName.contains("QueryMessage") || parentName.contains("APIQueryMsg")) {
                info.setQuery(true);
            }
        }
    }

    private List<ApiParamInfo> resolveInheritedParams(ClassOrInterfaceDeclaration cls) {
        List<ApiParamInfo> inherited = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(cls.getNameAsString());

        for (ClassOrInterfaceType parentType : cls.getExtendedTypes()) {
            collectParentParams(parentType.getNameAsString(), inherited, visited);
        }

        return inherited;
    }

    private void collectParentParams(String className, List<ApiParamInfo> params, Set<String> visited) {
        if (visited.contains(className)) return;
        visited.add(className);

        // Skip known base classes that don't have API params
        if ("APIMessage".equals(className) || "NeedReplyMessage".equals(className) ||
            "Message".equals(className) || "Object".equals(className)) {
            return;
        }

        // Try FQCN first, then fall back to simple name
        Path parentPath = fqcnIndex.get(className);
        if (parentPath == null) parentPath = classIndex.get(className);
        if (parentPath == null) return;

        CompilationUnit parentCu = parseCached(parentPath);
        if (parentCu == null) return;

        for (ClassOrInterfaceDeclaration parentCls : parentCu.findAll(ClassOrInterfaceDeclaration.class)) {
            if (!parentCls.getNameAsString().equals(className)) continue;

            // Extract params from parent
            params.addAll(extractParams(parentCls));

            // Recurse into grandparent
            for (ClassOrInterfaceType grandparent : parentCls.getExtendedTypes()) {
                collectParentParams(grandparent.getNameAsString(), params, visited);
            }
        }
    }

    private CompilationUnit parseCached(Path path) {
        String key = path.toString();
        if (cuCache.containsKey(key)) return cuCache.get(key);

        try {
            ParseResult<CompilationUnit> result = parser.parse(path);
            CompilationUnit cu = result.isSuccessful() && result.getResult().isPresent()
                    ? result.getResult().get() : null;
            cuCache.put(key, cu);
            return cu;
        } catch (IOException e) {
            cuCache.put(key, null);
            return null;
        }
    }

    private String resolveHttpMethod(Expression expr) {
        String text = expr.toString();
        // HttpMethod.POST -> POST
        if (text.contains(".")) {
            return text.substring(text.lastIndexOf('.') + 1);
        }
        return text;
    }

    private String resolveClassName(Expression expr) {
        String text = expr.toString();
        if (text.endsWith(".class")) {
            return text.substring(0, text.length() - 6);
        }
        return text;
    }

    private String resolveFieldType(FieldDeclaration field) {
        String type = field.getElementType().asString();
        // Map common types to fully qualified names
        switch (type) {
            case "String":    return "java.lang.String";
            case "Long":      return "java.lang.Long";
            case "long":      return "long";
            case "Integer":   return "java.lang.Integer";
            case "int":       return "int";
            case "Boolean":   return "java.lang.Boolean";
            case "boolean":   return "boolean";
            case "Double":    return "java.lang.Double";
            case "Float":     return "java.lang.Float";
            case "List":      return "java.util.List";
            case "Map":       return "java.util.Map";
            case "Set":       return "java.util.Set";
            default:          return type;
        }
    }

    private int parseIntValue(Expression expr) {
        if (expr.isIntegerLiteralExpr()) return expr.asIntegerLiteralExpr().asInt();
        try { return Integer.parseInt(expr.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private String[] parseStringArray(Expression expr) {
        if (expr.isArrayInitializerExpr()) {
            List<Expression> values = expr.asArrayInitializerExpr().getValues();
            String[] result = new String[values.size()];
            for (int i = 0; i < values.size(); i++) {
                Expression v = values.get(i);
                result[i] = v.isStringLiteralExpr() ? v.asStringLiteralExpr().getValue() : v.toString();
            }
            return result;
        }
        return new String[0];
    }

    private long[] parseLongArray(Expression expr) {
        if (expr.isArrayInitializerExpr()) {
            List<Expression> values = expr.asArrayInitializerExpr().getValues();
            long[] result = new long[values.size()];
            for (int i = 0; i < values.size(); i++) {
                try { result[i] = Long.parseLong(values.get(i).toString().replaceAll("[Ll]$", "")); }
                catch (NumberFormatException e) { result[i] = 0; }
            }
            return result;
        }
        return new long[0];
    }

    private List<String> parseStringList(Expression expr) {
        List<String> result = new ArrayList<>();
        if (expr.isArrayInitializerExpr()) {
            for (Expression v : expr.asArrayInitializerExpr().getValues()) {
                if (v.isStringLiteralExpr()) result.add(v.asStringLiteralExpr().getValue());
            }
        }
        return result;
    }
}
