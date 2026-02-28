package scripts

import org.zstack.header.query.APIQueryMessage
import org.zstack.header.rest.RestRequest
import org.zstack.rest.sdk.SdkFile
import org.zstack.rest.sdk.SdkTemplate
import org.zstack.header.rest.SDK
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger
import org.zstack.header.rest.RestResponse
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Go SDK API Template Generator
 */
class GoApiTemplate implements SdkTemplate {
    private static final CLogger logger = Utils.getLogger(GoApiTemplate.class)
    private Class<?> apiMsgClazz
    private RestRequest at
    private String path
    private Class responseClass
    private String allTo;
    private String replyName
    private SdkTemplate inventoryGenerator

    private String actionType
    private String resourceName
    private String clzName

    // For Query APIs, store the inventory class
    private Class<?> queryInventoryClass
    // Store the field name of the inventory in the response class (e.g. "inventory", "vmInventory")
    private String inventoryFieldName

    // Track if different parts of the API have been grouped into consolidated files
    boolean isActionGrouped = false
    boolean isViewGrouped = false
    boolean isParamGrouped = false

    // Track generated files to avoid duplicates
    private static Set<String> generatedParamFiles = new HashSet<>()
    private static Set<String> generatedActionFiles = new HashSet<>()
    private static Set<String> generatedViewFiles = new HashSet<>()

    // Track known inventory classes for validation
    private static Set<Class<?>> knownInventoryClasses = null

    // Track APIs that have been grouped by GoInventory to avoid individual file generation
    static Set<String> groupedApiNames = new HashSet<>()

    // LongJob mappings (provided by GoInventory)
    private static Map<Class, Class> longJobMappings = new HashMap<>()

    // Track APIs that should be skipped during generation
    private static Set<String> skippedApis = new HashSet<>()

    // Flag to indicate if the template was successfully initialized
    private boolean valid = false

    GoApiTemplate(Class apiMsgClass, SdkTemplate inventoryGenerator) {
        try {
            apiMsgClazz = apiMsgClass
            this.inventoryGenerator = inventoryGenerator
            at = apiMsgClazz.getAnnotation(RestRequest.class)
            if (at == null) {
                logger.warn("[GoSDK] Class ${apiMsgClazz.name} is missing @RestRequest annotation")
                return
            }

            String rawPath = at.path()
            if (rawPath == null || rawPath == "null") {
                String[] opts = at.optionalPaths()
                if (opts != null && opts.length > 0) {
                    path = opts[0]
                } else {
                    logger.warn("[GoSDK] API ${apiMsgClazz.name} has no path or optionalPaths")
                    path = "/unknown"
                }
            } else {
                path = rawPath
            }

            responseClass = at.responseClass()
            if (responseClass == null || responseClass == org.zstack.header.rest.RestResponse.class) {
                org.zstack.header.rest.RestResponse res = apiMsgClazz.getAnnotation(org.zstack.header.rest.RestResponse.class)
                if (res != null) {
                    responseClass = res.value()
                }
            }
            allTo = ""
            if (responseClass != null) {
                replyName = responseClass.simpleName.replaceAll('^API', '').replaceAll('Reply$', '').replaceAll('Event$', '')
                RestResponse restResponse = responseClass.getAnnotation(RestResponse)
                allTo = restResponse != null ? restResponse.allTo() : ""
            } else {
                logger.warn("[GoSDK] Could not determine responseClass for " + apiMsgClazz.name)
                replyName = "UnknownResponse"
            }

            // Only strip API prefix and Msg suffix, keep Action to avoid name collisions
            // e.g. APIQueryMonitorTriggerActionMsg -> QueryMonitorTriggerAction
            clzName = apiMsgClazz.simpleName.replaceAll('^API', '').replaceAll('Msg$', '')

            parseActionAndResource()
            logger.warn("[GoSDK] Parsed API: ${apiMsgClazz.simpleName} -> Action=${actionType}, Resource=${resourceName}")

            // Find the inventory class if available (for both Query and Action APIs)
            queryInventoryClass = findInventoryClass()

            logger.warn("[GoSDK] Processing API: " + clzName + " -> action=" + actionType + ", resource=" + resourceName + ", response=" + responseClass?.simpleName)

            // Mark as successfully initialized
            valid = true
        } catch (Throwable e) {
            logger.error("[GoSDK] CRITICAL ERROR constructing GoApiTemplate for ${apiMsgClass.name}: ${e.class.name}: ${e.message}", e)
            throw e
        }
    }

    /**
     * Check if the template was successfully initialized.
     * Templates without @RestRequest annotation are invalid.
     */
    boolean isValid() {
        return valid
    }

    RestRequest getAt() {
        return at
    }

    String getActionType() {
        return actionType
    }

    String getResourceName() {
        return resourceName
    }

    Class<?> getQueryInventoryClass() {
        return queryInventoryClass
    }

    Class<?> getResponseClass() {
        return responseClass
    }

    /**
     * Return all client method names this template may emit, for deduplication.
     */
    Set<String> getGeneratedMethodNames() {
        def names = new LinkedHashSet<String>()
        names.add(clzName)

        // Query APIs always generate both Get and Page helpers
        if (isQueryMessage()) {
            String getMethodName = clzName.replaceFirst('^Query', 'Get')
            names.add(getMethodName)

            String pageMethodName = clzName.replaceFirst('^Query', 'Page')
            names.add(pageMethodName)
        }

        if (supportsAsync() && shouldGenerateAsync()) {
            names.add("${clzName}Async")
        }

        return names
    }

    boolean isQueryMessage() {
        return APIQueryMessage.class.isAssignableFrom(apiMsgClazz)
    }

    Class<?> getApiMsgClazz() {
        return apiMsgClazz
    }

    String getParamStructName() {
        return clzName + "Param"
    }

    String getDetailParamStructName() {
        return clzName + "ParamDetail"
    }

    /**
     * Reset all static state for clean re-generation.
     * Should be called at the beginning of generate() or before each SDK generation run.
     */
    static void reset() {
        generatedParamFiles.clear()
        generatedActionFiles.clear()
        generatedViewFiles.clear()
        knownInventoryClasses = null
        groupedApiNames.clear()
        longJobMappings.clear()
        skippedApis.clear()
        logger.warn("[GoSDK] Reset all static state")
    }

    /**
     * Set known inventory classes for validation
     */
    static void setKnownInventoryClasses(Set<Class<?>> inventories) {
        knownInventoryClasses = inventories
        logger.warn("[GoSDK] Registered " + (inventories?.size() ?: 0) + " inventory classes")
    }

    /**
     * Set LongJob mappings (called by GoInventory)
     */
    static void setLongJobMappings(Map<Class, Class> mappings) {
        longJobMappings = mappings
        logger.warn("[GoSDK] Registered ${mappings.size()} LongJob mappings")
    }

    /**
     * Check if current API supports async operation
     */
    private boolean supportsAsync() {
        if (apiMsgClazz == null) return false
        return longJobMappings.containsKey(apiMsgClazz)
    }

    /**
     * Get list of skipped APIs
     */
    static Set<String> getSkippedApis() {
        return skippedApis
    }

    /**
     * Check if a view class is available
     */
    private boolean isViewAvailable(Class<?> viewClass) {
        if (viewClass == null) {
            return false
        }
        // Check if it's an Inventory class or a Reply class
        if (knownInventoryClasses != null && knownInventoryClasses.contains(viewClass)) {
            return true
        }
        // Reply classes should always be available as we generate them
        if (viewClass.simpleName.endsWith("Reply") || viewClass.simpleName.endsWith("Event")) {
            return true
        }
        return false
    }

    /**
     * Check if API class has valid parameter fields (excluding inherited base fields)
     */
    private boolean hasApiParams() {
        if (apiMsgClazz == null) {
            return false
        }
        try {
            def fields = apiMsgClazz.getDeclaredFields()
            // Filter out synthetic, static fields and common inherited fields
            def validFields = fields.findAll { field ->
                !java.lang.reflect.Modifier.isStatic(field.modifiers) &&
                        !field.synthetic &&
                        !field.name.startsWith('__') &&
                        field.name != 'session' &&
                        field.name != 'timeout' &&
                        field.name != 'commandTimeout'
            }
            boolean result = validFields.size() > 0
            if (!result) {
                logger.warn("[GoSDK] ${apiMsgClazz.simpleName} has NO valid parameter fields")
            }
            return result
        } catch (Exception e) {
            logger.warn("[GoSDK] Error checking params for ${apiMsgClazz.simpleName}: ${e.message}")
            return true  // Default to true if can't determine
        }
    }

    private Class<?> findInventoryClass() {
        inventoryFieldName = null
        if (responseClass == null) return null

        logger.debug("[GoSDK] Finding inventory class for API: " + clzName + " (response=" + responseClass?.simpleName + ")")

        try {
            // 1. Try to find 'inventory' field (for single resource return)
            try {
                Field inventoryField = responseClass.getDeclaredField("inventory")
                if (inventoryField != null) {
                    Class<?> fieldType = inventoryField.getType()
                    // If inventory is a collection or map, skip unwrap to avoid List/MapView pointer mismatch
                    if (Collection.class.isAssignableFrom(fieldType) || Map.class.isAssignableFrom(fieldType)) {
                        logger.warn("[GoSDK] Inventory field for " + clzName + " is a collection/map, skip unwrap")
                        inventoryFieldName = null
                        // Try to unwrap generic element/value only if it's a concrete Inventory class
                        if (inventoryField.getGenericType() instanceof ParameterizedType) {
                            ParameterizedType pt = (ParameterizedType) inventoryField.getGenericType()
                            Type[] args = pt.getActualTypeArguments()
                            if (args != null && args.length > 0) {
                                Type candidate = args.length > 1 ? args[1] : args[0]
                                if (candidate instanceof Class && ((Class<?>) candidate).isAnnotationPresent(org.zstack.header.search.Inventory.class)) {
                                    return (Class<?>) candidate
                                }
                            }
                        }
                        return null
                    }
                    logger.warn("[GoSDK] Found inventory field for " + clzName + ": " + fieldType.simpleName)
                    inventoryFieldName = "inventory"
                    return fieldType
                }
            } catch (NoSuchFieldException e) {
                // ignore
            }

            // 2. Try to find 'inventories' field (for query/list return)
            try {
                Field inventoriesField = responseClass.getDeclaredField("inventories")
                if (inventoriesField != null) {
                    def genericType = inventoriesField.getGenericType()
                    if (genericType != null && genericType instanceof ParameterizedType) {
                        def paramType = (ParameterizedType) genericType
                        def actualTypes = paramType.getActualTypeArguments()
                        if (actualTypes != null && actualTypes.length > 0) {
                            def typeArg = actualTypes[0]
                            if (typeArg instanceof Class) {
                                logger.warn("[GoSDK] Found inventories element class for " + clzName + ": " + ((Class<?>) typeArg).simpleName)
                                return (Class<?>) typeArg
                            }
                        }
                    }
                }
            } catch (NoSuchFieldException e) {
                // ignore
            }
        } catch (NoSuchFieldException e) {
            logger.debug("[GoSDK] No 'inventories' field in " + responseClass?.simpleName)
        } catch (Exception e) {
            logger.warn("[GoSDK] Error finding inventory class for " + clzName + ": " + e.message)
        }

        // Fallback: try to find in known inventories by name
        if (resourceName != null && !resourceName.isEmpty() && knownInventoryClasses != null) {
            String expectedInventoryName = resourceName + "Inventory"
            for (Class<?> inv : knownInventoryClasses) {
                if (inv.simpleName == expectedInventoryName) {
                    logger.warn("[GoSDK] Found inventory by name matching for " + clzName + ": " + inv.simpleName)
                    return inv
                }
            }
        }

        logger.debug("[GoSDK] Could not find inventory class for API: " + clzName)
        return null
    }

    private void parseActionAndResource() {
        def actionPrefixes = ["Create", "Query", "Get", "Update", "Delete", "Destroy",
                              "Start", "Stop", "Reboot", "Attach", "Detach", "Change",
                              "Expunge", "Recover", "Migrate", "Clone", "Resize", "Add", "Remove",
                              "Allocate", "Apply", "Release", "Deallocate", "Validate", "Sync", "Reconnect",
                              "Set", "Reset", "Search", "Calculate", "Check", "Refresh",
                              "Batch", "Login", "Logout", "Register", "Unregister", "Security", "Ack", "Clean", "Bootstrap", "Inspect"]

        for (String prefix : actionPrefixes) {
            if (clzName.startsWith(prefix)) {
                actionType = prefix
                resourceName = clzName.substring(prefix.length())

                // Further clean resourceName
                resourceName = resourceName.replaceAll('Action$', '').replaceAll('Msg$', '')
                return
            }
        }

        // Extended prefixes for better grouping
        def extendedPrefixes = ["SNS", "Sns", "Resume", "Migrate", "Locate", "Generate", "Export", "SelfTest",
                                "Calculate", "Check", "Refresh", "Sync", "Reconnect", "Archive", "Backup", "Revert"]
        for (String prefix : extendedPrefixes) {
            if (clzName.startsWith(prefix)) {
                actionType = prefix
                resourceName = clzName.substring(prefix.length())
                resourceName = resourceName.replaceAll('Action$', '').replaceAll('Msg$', '')
                return
            }
        }

        actionType = ""
        resourceName = clzName.replaceAll('Action$', '').replaceAll('Msg$', '')
    }

    private String toSnakeCase(String name) {
        if (name == null || name.isEmpty()) {
            return "unknown"
        }
        return name.replaceAll('([a-z])([A-Z])', '$1_$2').toLowerCase()
    }

    private String getApiPath() {
        if (path.startsWith("/")) {
            return "v1" + path
        }
        return "v1/" + path
    }

    private String getApiOptPath(String optPath) {
        if (optPath.startsWith("/")) {
            return "v1" + optPath
        }
        return "v1/" + optPath
    }

    List<SdkFile> generate() {
        return []
    }

    /**
     * Generate code for the response view struct (Event or Reply).
     */
    String generateResponseViewCode() {
        if (responseClass == null) return ""

        String viewStructName = inventoryGenerator.getViewStructName(responseClass)
        return inventoryGenerator.generateViewStruct(responseClass, viewStructName)
    }

    private String generateMethodImplementation(String apiPath, String httpMethod, String viewStructName, boolean isQueryMessage, Set<String> skipNames) {
        def builder = new StringBuilder()
        boolean skipMain = skipNames?.contains(clzName)

        builder.append("// ${clzName} ${getMethodDescription()}\n")

        // http_client unwraps inventory fields for POST/PUT requests
        // Therefore Create/Add/Update/Change calls can return the Inventory View directly
        // Only GET calls may need manual unwrapping
        boolean unwrapForGet = !isQueryMessage &&
                queryInventoryClass != null &&
                viewStructName == inventoryGenerator.getViewStructName(queryInventoryClass) &&
                responseClass != queryInventoryClass &&
                inventoryFieldName != null &&
                (actionType == "Get" || httpMethod == "GET")

        String responseStructName = inventoryGenerator.getViewStructName(responseClass)
        String goInventoryFieldName = inventoryFieldName != null ? inventoryFieldName.substring(0, 1).toUpperCase() + inventoryFieldName.substring(1) : "Inventory"

        if (isQueryMessage) {
            // Query APIs generate a list method
            if (!skipMain) {
                builder.append(generateQueryMethod(apiPath, viewStructName))
            }

            // Always generate Get(single) for Query APIs to avoid compile gaps
            String getMethodName = clzName.replaceFirst('^Query', 'Get')
            String[] opts = at.optionalPaths()
            String optPath = ""
            if (opts.length != 0) {
                optPath = getApiOptPath(opts[0])
            }
            // Emit unless explicitly skipped (no clzName equality check)
            if (optPath != "" && !skipNames.contains(getMethodName)) {
                builder.append(generateGetMethodForQuery(optPath, viewStructName))
            }

            // Generate Page pagination helper for Query APIs
            String pageMethodName = clzName.replaceFirst('^Query', 'Page')
            if (!skipNames.contains(pageMethodName)) {
                builder.append(generatePageMethod(apiPath, viewStructName))
            }
        } else {
            if (!skipMain) {
                // CRITICAL: Prioritize @RestRequest.method over actionType derived from class name
                // This fixes issues like APIGetVersionMsg (actionType="Get") with method=PUT

                // First check HTTP method from annotation, then fall back to actionType-based logic
                if (httpMethod == "POST") {
                    // POST operations (Create/Add)
                    builder.append(generateCreateMethod(apiPath, viewStructName, false, responseStructName, goInventoryFieldName))
                } else if (httpMethod == "GET") {
                    // GET operations (Get/Query)
                    builder.append(generateGetMethod(apiPath, viewStructName, unwrapForGet, responseStructName, goInventoryFieldName))
                    String[] optPaths = at.optionalPaths()
                    for (String optPath : optPaths) {
                        if (apiPath.endsWith(optPath)) {
                            continue
                        }
                        builder.append(generateGetMethodForOptPath(getApiOptPath(optPath), viewStructName))
                    }
                } else if (httpMethod == "PUT") {
                    // PUT operations (Update/Change/Action)
                    // Special case: Expunge actions return void
                    if (actionType == "Expunge") {
                        builder.append(generateExpungeMethod(apiPath))
                    } else {
                        builder.append(generateUpdateMethod(apiPath, viewStructName, false, responseStructName, goInventoryFieldName))
                    }
                } else if (httpMethod == "DELETE") {
                    // DELETE operations
                    builder.append(generateDeleteMethod(apiPath))
                } else {
                    // Fallback for unknown HTTP methods (should rarely happen)
                    logger.warn("[GoSDK] Unknown HTTP method ${httpMethod} for ${clzName}, using generic method")
                    boolean unwrapGeneric = (httpMethod == "GET") ? unwrapForGet : false
                    builder.append(generateGenericMethod(apiPath, httpMethod, viewStructName, unwrapGeneric, responseStructName, goInventoryFieldName))
                }
            }

            String asyncMethodName = "${clzName}Async"
            if (supportsAsync() && shouldGenerateAsync() && !skipNames.contains(asyncMethodName)) {
                builder.append(generateAsyncMethod(apiPath, httpMethod, viewStructName))
            }
        }

        return builder.toString()
    }

    /**
     * Public method to generate just the method implementation code.
     * Used by GoInventory to consolidate "Other" actions.
     */
    String generateMethodCode() {
        return generateMethodCode(Collections.emptySet())
    }

    /**
     * Generate client method code while skipping provided method names.
     */
    String generateMethodCode(Set<String> skipNames) {
        String apiPath = getApiPath()
        String httpMethod = at.method().toString()
        boolean isQuery = isQueryMessage()

        Class<?> targetViewClass = queryInventoryClass != null ? queryInventoryClass : responseClass
        String viewStructName = inventoryGenerator.getViewStructName(targetViewClass)

        return generateMethodImplementation(apiPath, httpMethod, viewStructName, isQuery, skipNames ?: Collections.emptySet())
    }

    private String getMethodDescription() {
        switch (actionType) {
            case "Create": return "creates ${resourceName}"
            case "Query": return "queries ${resourceName} list"
            case "Get": return "gets ${resourceName} by uuid"
            case "Update": return "updates ${resourceName}"
            case "Delete": return "deletes ${resourceName}"
            case "Destroy": return "destroys ${resourceName}"
            case "Start": return "starts ${resourceName}"
            case "Stop": return "stops ${resourceName}"
            case "Change": return "changes ${resourceName}"
            case "Add": return "adds ${resourceName}"
            case "Remove": return "removes ${resourceName}"
            default: return "operates on ${resourceName}"
        }
    }

    private String generateCreateMethod(String apiPath, String viewStructName, boolean unwrap, String responseStructName, String fieldName) {
        boolean hasParams = hasApiParams()

        if (!hasParams) {
            // No params: don't require user to pass params, use empty map internally
            if (unwrap) {
                return """func (cli *ZSClient) ${clzName}(ctx context.Context) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\tif err := cli.Post(ctx, "${apiPath}", map[string]interface{}{}, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
            } else {
                return """func (cli *ZSClient) ${clzName}(ctx context.Context) (*view.${viewStructName}, error) {
\tresp := view.${viewStructName}{}
\tif err := cli.Post(ctx, "${apiPath}", map[string]interface{}{}, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
            }
        }

        // Has params: require user to pass params
        if (unwrap) {
            return """func (cli *ZSClient) ${clzName}(ctx context.Context, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\tif err := cli.Post(ctx, "${apiPath}", params, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
        } else {
            def placeholders = extractUrlPlaceholders(apiPath)
            def cleanAPIPath = apiPath
            def allPlaceholders = ""
            for (String placeholder : placeholders) {
                cleanAPIPath = cleanAPIPath.replace("{" + placeholder + "}", "%s")
                if (allPlaceholders == "") {
                    allPlaceholders = placeholder
                } else {
                    allPlaceholders += ", " + placeholder
                }
            }
            def inputParams = ","
            def formatParams = ""
            if (allPlaceholders != "") {
                inputParams = ", " + allPlaceholders + " string,"
                formatParams = ", " + allPlaceholders
            }


            return """func (cli *ZSClient) ${clzName}(ctx context.Context${inputParams} params param.${clzName}Param) (*view.${viewStructName}, error) {
\tresp := view.${viewStructName}{}
\tif err := cli.PostWithRespKey(ctx, fmt.Sprintf("${cleanAPIPath}"${formatParams}), "${allTo}", params, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
        }
    }

    private String generateQueryMethod(String apiPath, String viewStructName) {
        return """func (cli *ZSClient) ${clzName}(ctx context.Context, params *param.QueryParam) ([]view.${viewStructName}, error) {
\tvar resp []view.${viewStructName}
\treturn resp, cli.List(ctx, "${apiPath}", params, &resp)
}
"""
    }

    /**
     * Generate Page pagination method for Query APIs.
     * Derive PageXxx from the QueryXxx resource name.
     */
    private String generatePageMethod(String apiPath, String viewStructName) {
        String pageMethodName = clzName.replaceFirst('^Query', 'Page')
        String varName = resourceName.substring(0, 1).toLowerCase() + resourceName.substring(1)
        // Only replace y with ies if preceded by a consonant
        if (varName.endsWith("y") && varName.length() > 1 && !"aeiou".contains(varName.charAt(varName.length() - 2).toString())) {
            varName = varName.substring(0, varName.length() - 1) + "ies"
        } else if (!varName.endsWith("s")) {
            varName = varName + "s"
        }

        return """
// ${pageMethodName} Pagination
func (cli *ZSClient) ${pageMethodName}(ctx context.Context, params *param.QueryParam) ([]view.${viewStructName}, int, error) {
\tvar ${varName} []view.${viewStructName}
\ttotal, err := cli.Page(ctx, "${apiPath}", params, &${varName})
\treturn ${varName}, total, err
}
"""
    }

    /**
     * Generate Get(uuid) single-resource method for Query APIs.
     * Extract the resource portion from Query{Resource}.
     */
    private String generateGetMethodForQuery(String apiPath, String viewStructName) {
        // Extract {Resource} from Query{Resource}
        String getMethodName = clzName.replaceFirst("^Query", "Get")

        // Extract URL placeholders to see if special handling is required
        def placeholders = extractUrlPlaceholders(apiPath)
        String cleanPath = removePlaceholders(apiPath)

        // If the path has multiple placeholders (for example {category}/{name}), use GetWithSpec
        if (placeholders.size() >= 2) {
            String params = placeholders.collect { "${toSafeGoParamName(it)} string" }.join(", ")
            String firstParam = toSafeGoParamName(placeholders[0])
            def remainingPlaceholders = placeholders.drop(1)
            String spec = buildSpecPath(remainingPlaceholders)

            return """
func (cli *ZSClient) ${getMethodName}(ctx context.Context, ${params}) (*view.${viewStructName}, error) {
\tvar resp view.${viewStructName}
\terr := cli.GetWithSpec(ctx, "${cleanPath}", ${firstParam}, ${spec}, "${allTo}", nil, &resp)
\tif err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
        }

        // Standard case: single uuid parameter
        return """
func (cli *ZSClient) ${getMethodName}(ctx context.Context, uuid string) (*view.${viewStructName}, error) {
\tvar resp view.${viewStructName}
\tif err := cli.Get(ctx, "${cleanPath}", uuid, nil, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
    }

    private String generateGetMethodForOptPath(String apiPath, String viewStructName) {
        String getMethodName = ""
        org.zstack.header.rest.SDK sdk = apiMsgClazz.getAnnotation(org.zstack.header.rest.SDK.class)
        if (sdk != null) {
            for (String actionMapping : sdk.actionsMapping()) {
                String[] actionUrl = actionMapping.split('=')
                if (actionUrl.length != 2 || !apiPath.endsWith(actionUrl[1])) {
                    continue
                }
                getMethodName = actionUrl[0]
                break
            }
        }
        if (getMethodName == "") {
            return ""
        }
        // Extract URL placeholders to see if special handling is required
        def placeholders = extractUrlPlaceholders(apiPath)
        String cleanPath = removePlaceholders(apiPath)

        if (placeholders.size() != 1) {
            return ""
        }
        String returnTypePointerOrArray = allTo == "inventories" ? "[]" : "*"
        String varTypePointerOrArray = allTo == "inventories" ? "[]" : ""
        String returnUsingRef = allTo == "inventories" ? "" : "&"
        String suffix = apiPath.substring(apiPath.indexOf("{" + placeholders[0] + "}") + 2 + placeholders[0].length())
        suffix = suffix.startsWith("/") ? suffix.substring(1) : suffix
        // Standard case: single uuid parameter
        return """
func (cli *ZSClient) ${getMethodName}(ctx context.Context, uuid string) (${returnTypePointerOrArray}view.${viewStructName}, error) {
\tvar resp ${varTypePointerOrArray}view.${viewStructName}
\tif err := cli.GetWithSpec(ctx, "${cleanPath}", uuid, "${suffix}", "${allTo}", nil, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn ${returnUsingRef}resp, nil
}
"""
    }

    private String generateGetMethod(String apiPath, String viewStructName, boolean unwrap, String responseStructName, String fieldName) {
        // Extract URL placeholders
        def placeholders = extractUrlPlaceholders(apiPath)
        String cleanPath = removePlaceholders(apiPath)

        // Only use GetWithSpec when there are two or more placeholders
        boolean useSpec = placeholders.size() >= 2

        if (unwrap) {
            if (!useSpec) {
                // Check if there are any placeholders
                if (placeholders.size() == 0) {
                    // No placeholder: no uuid parameter needed
                    // Use GetWithRespKey to extract the inventory field
                    return """func (cli *ZSClient) ${clzName}(ctx context.Context) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\tif err := cli.GetWithRespKey(ctx, "${cleanPath}", "", "", nil, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
                } else {
                    // Single placeholder: use GetWithRespKey with uuid to extract inventory
                    return """func (cli *ZSClient) ${clzName}(ctx context.Context, uuid string) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\tif err := cli.GetWithRespKey(ctx, "${cleanPath}", uuid, "", nil, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
                }
            } else {
                // Multiple placeholders: use GetWithSpec
                // First placeholder is the resourceId; the rest form the spec
                String firstParam = toSafeGoParamName(placeholders[0])
                def remainingPlaceholders = placeholders.drop(1)
                String params = placeholders.collect { "${toSafeGoParamName(it)} string" }.join(", ")
                String spec = buildSpecPath(remainingPlaceholders)

                return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${params}) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\terr := cli.GetWithSpec(ctx, "${cleanPath}", ${firstParam}, ${spec}, "", nil, &resp)
\tif err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
            }
        } else {
            if (!useSpec) {
                // Check if there are any placeholders
                if (placeholders.size() == 0) {
                    // No placeholder: no uuid parameter needed
                    // Use GetWithRespKey with empty responseKey to parse whole response
                    return """func (cli *ZSClient) ${clzName}(ctx context.Context) (*view.${viewStructName}, error) {
\tvar resp view.${viewStructName}
\tif err := cli.GetWithRespKey(ctx, "${cleanPath}", "", "", nil, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
                } else {
                    // Single placeholder: use GetWithRespKey with uuid
                    return """func (cli *ZSClient) ${clzName}(ctx context.Context, uuid string) (*view.${viewStructName}, error) {
\tvar resp view.${viewStructName}
\tif err := cli.GetWithRespKey(ctx, "${cleanPath}", uuid, "", nil, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
                }
            } else {
                // Multiple placeholders: use GetWithSpec
                // First placeholder is the resourceId; the rest form the spec
                String firstParam = toSafeGoParamName(placeholders[0])
                def remainingPlaceholders = placeholders.drop(1)
                String params = placeholders.collect { "${toSafeGoParamName(it)} string" }.join(", ")
                String spec = buildSpecPath(remainingPlaceholders)

                return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${params}) (*view.${viewStructName}, error) {
\tvar resp view.${viewStructName}
\terr := cli.GetWithSpec(ctx, "${cleanPath}", ${firstParam}, ${spec}, "", nil, &resp)
\tif err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
            }
        }
    }

    private String generateUpdateMethod(String apiPath, String viewStructName, boolean unwrap, String responseStructName, String fieldName) {
        // Extract URL placeholders
        def placeholders = extractUrlPlaceholders(apiPath)
        String cleanPath = removePlaceholders(apiPath)

        // Determine whether this is an Action API (isAction=true or path ends with /actions)
        boolean isActionApi = at.isAction() || apiPath.endsWith("/actions")

        // Only use PutWithSpec when there are two or more placeholders
        boolean useSpec = placeholders.size() >= 2

        // Check if API has parameters
        boolean hasParams = hasApiParams()

        // Resolve the action key (map key for Action APIs)
        // Prefer @RestRequest.parameterName, otherwise derive from class name
        String actionKey
        if (isActionApi) {
            if (at.parameterName() != null && !at.parameterName().isEmpty() && !at.parameterName().equals("null")) {
                actionKey = at.parameterName()
            } else {
                def apiClassName = apiMsgClazz.simpleName
                def actionName = apiClassName.replaceAll('^API', '').replaceAll('Msg$', '')
                actionKey = actionName.substring(0, 1).toLowerCase() + actionName.substring(1)
            }
        }

        if (unwrap) {
            if (!useSpec) {
                if (placeholders.size() == 1) {
                    // Single placeholder pulled from the path parameter
                    String paramName = toSafeGoParamName(placeholders[0])
                    if (isActionApi) {
                        // Action APIs wrap params.Params inside a map
                        return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${paramName} string, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\tif err := cli.Put(ctx, "${cleanPath}", ${paramName}, map[string]interface{}{
\t\t"${actionKey}": params.Params,
\t}, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
                    } else {
                        return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${paramName} string, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\tif err := cli.Put(ctx, "${cleanPath}", ${paramName}, params, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
                    }
                } else {
                    // No placeholder: use the standard Put method
                    if (!hasParams) {
                        // No params: don't require user input
                        if (isActionApi) {
                            return """func (cli *ZSClient) ${clzName}(ctx context.Context) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\tif err := cli.Put(ctx, "${cleanPath}", "", map[string]interface{}{
\t\t"${actionKey}": map[string]interface{}{},
\t}, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
                        } else {
                            return """func (cli *ZSClient) ${clzName}(ctx context.Context) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\tif err := cli.Put(ctx, "${cleanPath}", "", map[string]interface{}{}, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
                        }
                    } else if (isActionApi) {
                        return """func (cli *ZSClient) ${clzName}(ctx context.Context, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\tif err := cli.Put(ctx, "${cleanPath}", "", map[string]interface{}{
\t\t"${actionKey}": params.Params,
\t}, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
                    } else {
                        return """func (cli *ZSClient) ${clzName}(ctx context.Context, uuid string, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\tif err := cli.Put(ctx, "${cleanPath}", uuid, params, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
                    }
                }
            } else {
                // Multiple placeholders: use PutWithSpec
                // First placeholder is the resourceId; the rest form the spec
                String firstParam = toSafeGoParamName(placeholders[0])
                def remainingPlaceholders = placeholders.drop(1)
                String params = placeholders.collect { "${toSafeGoParamName(it)} string" }.join(", ")
                String spec = buildSpecPath(remainingPlaceholders)

                return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${params}, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tvar resp view.${responseStructName}
\terr := cli.PutWithSpec(ctx, "${cleanPath}", ${firstParam}, ${spec}, "", params, &resp)
\tif err != nil {
\t\treturn nil, err
\t}
\treturn &resp.${fieldName}, nil
}
"""
            }
        } else {
            // Variables already declared at method level, no need to redeclare

            if (!useSpec) {
                if (placeholders.size() == 1) {
                    // Single placeholder pulled from the path parameter
                    String paramName = toSafeGoParamName(placeholders[0])
                    if (isActionApi) {
                        String placeholder = placeholders[0]
                        int startIndex = apiPath.indexOf("{" + placeholder + "}") + 3 + placeholder.size()
                        String actionSuffix = ""
                        if (startIndex < apiPath.size()) {
                            actionSuffix = apiPath.substring(startIndex)
                        }
                        // Action APIs wrap params.Params inside a map
                        return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${paramName} string, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tresp := view.${viewStructName}{}
\tif err := cli.PutWithSpec(ctx, "${cleanPath}", ${paramName}, "${actionSuffix}", "${allTo}", map[string]interface{}{
\t\t"${actionKey}": params.Params,
\t}, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
                    } else {
                        return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${paramName} string, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tresp := view.${viewStructName}{}
\tif err := cli.PutWithRespKey(ctx, "${cleanPath}", ${paramName}, "${allTo}", params, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
                    }
                } else {
                    // No placeholder: use the standard Put method
                    if (!hasParams) {
                        // No params: don't require user input
                        if (isActionApi) {
                            return """func (cli *ZSClient) ${clzName}(ctx context.Context) (*view.${viewStructName}, error) {
\tresp := view.${viewStructName}{}
\tif err := cli.PutWithRespKey(ctx, "${cleanPath}", "", "${allTo}", map[string]interface{}{
\t\t"${actionKey}": map[string]interface{}{},
\t}, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
                        } else {
                            return """func (cli *ZSClient) ${clzName}(ctx context.Context) (*view.${viewStructName}, error) {
\tresp := view.${viewStructName}{}
\tif err := cli.PutWithRespKey(ctx, "${cleanPath}", "", "${allTo}", map[string]interface{}{}, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
                        }
                    } else if (isActionApi) {
                        return """func (cli *ZSClient) ${clzName}(ctx context.Context, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tresp := view.${viewStructName}{}
\tif err := cli.PutWithRespKey(ctx, "${cleanPath}", "", "${allTo}", map[string]interface{}{
\t\t"${actionKey}": params.Params,
\t}, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
                    } else {
                        return """func (cli *ZSClient) ${clzName}(ctx context.Context, uuid string, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tresp := view.${viewStructName}{}
\tif err := cli.PutWithRespKey(ctx, "${cleanPath}", uuid, "${allTo}", params, &resp); err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
                    }
                }
            } else {
                // Multiple placeholders: use PutWithSpec
                // First placeholder is the resourceId; the rest form the spec
                String firstParam = toSafeGoParamName(placeholders[0])
                def remainingPlaceholders = placeholders.drop(1)
                String params = placeholders.collect { "${toSafeGoParamName(it)} string" }.join(", ")
                String spec = buildSpecPath(remainingPlaceholders)

                return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${params}, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tresp := view.${viewStructName}{}
\terr := cli.PutWithSpec(ctx, "${cleanPath}", ${firstParam}, ${spec}, "${allTo}", params, &resp)
\tif err != nil {
\t\treturn nil, err
\t}
\treturn &resp, nil
}
"""
            }
        }
    }

    private String generateDeleteMethod(String apiPath) {
        // Extract URL placeholders
        def placeholders = extractUrlPlaceholders(apiPath)
        String cleanPath = removePlaceholders(apiPath)

        // Only use DeleteWithSpec when there are two or more placeholders
        boolean useSpec = placeholders.size() >= 2

        if (!useSpec) {
            // Single or no placeholder: use the standard Delete method
            return """func (cli *ZSClient) ${clzName}(ctx context.Context, uuid string, deleteMode param.DeleteMode) error {
\treturn cli.Delete(ctx, "${cleanPath}", uuid, string(deleteMode))
}
"""
        } else {
            // Multiple placeholders: use DeleteWithSpec
            // First placeholder is the resourceId; the rest form the spec
            String firstParam = toSafeGoParamName(placeholders[0])
            def remainingPlaceholders = placeholders.drop(1)
            String params = placeholders.collect { "${toSafeGoParamName(it)} string" }.join(", ")
            String spec = buildSpecPath(remainingPlaceholders)
            String paramsStr = "fmt.Sprintf(\"deleteMode=%s\", deleteMode)"

            return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${params}, deleteMode param.DeleteMode) error {
\treturn cli.DeleteWithSpec(ctx, "${cleanPath}", ${firstParam}, ${spec}, ${paramsStr}, nil)
}
"""
        }
    }

    /**
     * Generate Expunge method.
     * Expunge uses PUT at {resource}/{uuid}/actions and only returns an error.
     */
    private String generateExpungeMethod(String apiPath) {
        // Extract URL placeholders
        def placeholders = extractUrlPlaceholders(apiPath)
        String cleanPath = removePlaceholders(apiPath)

        // Expunge API typically uses /resource/{uuid}/actions; strip the /actions suffix
        if (cleanPath.endsWith("/actions")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 8)
        }

        // Build parameter key, for example expungeImage
        String paramKey = clzName.substring(0, 1).toLowerCase() + clzName.substring(1)

        return """func (cli *ZSClient) ${clzName}(ctx context.Context, uuid string, params param.${clzName}Param) error {
\treturn cli.Put(ctx, "${cleanPath}", uuid, map[string]interface{}{
\t\t"${paramKey}": params.Params,
\t}, nil)
}
"""
    }

    private String generateGenericMethod(String apiPath, String httpMethod, String viewStructName, boolean unwrap, String responseStructName, String fieldName) {
        String respType = unwrap ? "view.${responseStructName}" : "view.${viewStructName}"
        String returnStmt = unwrap ? "return &resp.${fieldName}, nil" : "return &resp, nil"
        String respDecl = unwrap ? "var resp ${respType}" : "resp := ${respType}{}"

        // Extract URL placeholders
        def placeholders = extractUrlPlaceholders(apiPath)
        String cleanPath = removePlaceholders(apiPath)

        // Determine whether this is an Action API (isAction=true or path ends with /actions)
        boolean isActionApi = at.isAction() || apiPath.endsWith("/actions")

        // Resolve the action key (map key for Action APIs)
        // Prefer @RestRequest.parameterName, otherwise derive from class name
        String actionKey
        if (isActionApi) {
            if (at.parameterName() != null && !at.parameterName().isEmpty() && !at.parameterName().equals("null")) {
                actionKey = at.parameterName()
            } else {
                def apiClassName = apiMsgClazz.simpleName
                def actionName = apiClassName.replaceAll('^API', '').replaceAll('Msg$', '')
                actionKey = actionName.substring(0, 1).toLowerCase() + actionName.substring(1)
            }
        }

        // Only use *WithSpec helpers when there are two or more placeholders
        boolean useSpec = placeholders.size() >= 2

        switch (httpMethod) {
            case "GET":
                if (!useSpec) {
                    return """func (cli *ZSClient) ${clzName}(ctx context.Context, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tvar resp ${respType}
\tif err := cli.Get(ctx, "${cleanPath}", "", params, &resp); err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                } else {
                    String params = placeholders.collect { "${toSafeGoParamName(it)} string" }.join(", ")
                    String pathSpec = buildPathSpec(placeholders)
                    return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${params}, params param.${clzName}Param) (*view.${viewStructName}, error) {
\tvar resp ${respType}
\terr := cli.GetWithSpec(ctx, "${cleanPath}", ${pathSpec}, "", "${allTo}", params, &resp)
\tif err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                }
            case "POST":
                if (!useSpec) {
                    return """func (cli *ZSClient) ${clzName}(ctx context.Context, params param.${clzName}Param) (*view.${viewStructName}, error) {
\t${respDecl}
\tif err := cli.Post(ctx, "${cleanPath}", params, &resp); err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                } else {
                    // POST lacks *WithSpec helpers; build the full URL manually
                    String params = placeholders.collect { "${toSafeGoParamName(it)} string" }.join(", ")
                    String fullPath = buildFullPath(placeholders)
                    return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${params}, params param.${clzName}Param) (*view.${viewStructName}, error) {
\t${respDecl}
\terr := cli.Post(ctx, ${fullPath}, params, &resp)
\tif err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                }
            case "PUT":
                boolean hasParams = hasApiParams()
                if (!useSpec) {
                    if (placeholders.size() == 1) {
                        // Single placeholder pulled from the path parameter
                        String paramName = toSafeGoParamName(placeholders[0])
                        if (!hasParams) {
                            // No params case
                            if (isActionApi) {
                                String actionSuffix = apiPath.endsWith("/actions") ? "actions" : ""
                                String putMethod = unwrap ? "cli.Put" : "cli.PutWithSpec"
                                String putArgs = unwrap ?
                                        """(ctx, "${cleanPath}", ${paramName}, map[string]interface{}{
\t\t"${actionKey}": map[string]interface{}{},
\t}, &resp)""" :
                                        """(ctx, "${cleanPath}", ${paramName}, "${actionSuffix}", "${allTo}", map[string]interface{}{
\t\t"${actionKey}": map[string]interface{}{},
\t}, &resp)"""
                                return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${paramName} string) (*view.${viewStructName}, error) {
\t${respDecl}
\tif err := ${putMethod}${putArgs}; err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                            } else {
                                String putMethod = unwrap ? "cli.Put" : "cli.PutWithRespKey"
                                String putArgs = unwrap ?
                                        """(ctx, "${cleanPath}", ${paramName}, map[string]interface{}{}, &resp)""" :
                                        """(ctx, "${cleanPath}", ${paramName}, "${allTo}", map[string]interface{}{}, &resp)"""
                                return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${paramName} string) (*view.${viewStructName}, error) {
\t${respDecl}
\tif err := ${putMethod}${putArgs}; err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                            }
                        } else if (isActionApi) {
                            // Action APIs wrap params.Params inside a map
                            String putMethod = unwrap ? "cli.Put" : "cli.PutWithRespKey"
                            String putArgs = unwrap ?
                                    """(ctx, "${cleanPath}", ${paramName}, map[string]interface{}{
\t\t"${actionKey}": params.Params,
\t}, &resp)""" :
                                    """(ctx, "${cleanPath}", ${paramName}, "${allTo}", map[string]interface{}{
\t\t"${actionKey}": params.Params,
\t}, &resp)"""
                            return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${paramName} string, params param.${clzName}Param) (*view.${viewStructName}, error) {
\t${respDecl}
\tif err := ${putMethod}${putArgs}; err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                        } else {
                            String putMethod = unwrap ? "cli.Put" : "cli.PutWithRespKey"
                            String putArgs = unwrap ?
                                    """(ctx, "${cleanPath}", ${paramName}, params, &resp)""" :
                                    """(ctx, "${cleanPath}", ${paramName}, "${allTo}", params, &resp)"""
                            return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${paramName} string, params param.${clzName}Param) (*view.${viewStructName}, error) {
\t${respDecl}
\tif err := ${putMethod}${putArgs}; err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                        }
                    } else {
                        // No placeholder: use the standard Put method
                        if (!hasParams) {
                            // No params: don't require user input
                            if (isActionApi) {
                                String putMethod = unwrap ? "cli.Put" : "cli.PutWithRespKey"
                                String putArgs = unwrap ?
                                        """(ctx, "${cleanPath}", "", map[string]interface{}{
\t\t"${actionKey}": map[string]interface{}{},
\t}, &resp)""" :
                                        """(ctx, "${cleanPath}", "", "${allTo}", map[string]interface{}{
\t\t"${actionKey}": map[string]interface{}{},
\t}, &resp)"""
                                return """func (cli *ZSClient) ${clzName}(ctx context.Context) (*view.${viewStructName}, error) {
\t${respDecl}
\tif err := ${putMethod}${putArgs}; err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                            } else {
                                String putMethod = unwrap ? "cli.Put" : "cli.PutWithRespKey"
                                String putArgs = unwrap ?
                                        """(ctx, "${cleanPath}", "", map[string]interface{}{}, &resp)""" :
                                        """(ctx, "${cleanPath}", "", "${allTo}", map[string]interface{}{}, &resp)"""
                                return """func (cli *ZSClient) ${clzName}(ctx context.Context) (*view.${viewStructName}, error) {
\t${respDecl}
\tif err := ${putMethod}${putArgs}; err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                            }
                        } else if (isActionApi) {
                            String putMethod = unwrap ? "cli.Put" : "cli.PutWithRespKey"
                            String putArgs = unwrap ?
                                    """(ctx, "${cleanPath}", "", map[string]interface{}{
\t\t"${actionKey}": params.Params,
\t}, &resp)""" :
                                    """(ctx, "${cleanPath}", "", "${allTo}", map[string]interface{}{
\t\t"${actionKey}": params.Params,
\t}, &resp)"""
                            return """func (cli *ZSClient) ${clzName}(ctx context.Context, params param.${clzName}Param) (*view.${viewStructName}, error) {
\t${respDecl}
\tif err := ${putMethod}${putArgs}; err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                        } else {
                            String putMethod = unwrap ? "cli.Put" : "cli.PutWithRespKey"
                            String putArgs = unwrap ?
                                    """(ctx, "${cleanPath}", uuid, params, &resp)""" :
                                    """(ctx, "${cleanPath}", uuid, "${allTo}", params, &resp)"""
                            return """func (cli *ZSClient) ${clzName}(ctx context.Context, uuid string, params param.${clzName}Param) (*view.${viewStructName}, error) {
\t${respDecl}
\tif err := ${putMethod}${putArgs}; err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                        }
                    }
                } else {
                    // Multiple placeholders: use PutWithSpec
                    String firstParam = toSafeGoParamName(placeholders[0])
                    def remainingPlaceholders = placeholders.drop(1)
                    String params = placeholders.collect { "${toSafeGoParamName(it)} string" }.join(", ")
                    String spec = buildSpecPath(remainingPlaceholders)

                    return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${params}, params param.${clzName}Param) (*view.${viewStructName}, error) {
\t${respDecl}
\terr := cli.PutWithSpec(ctx, "${cleanPath}", ${firstParam}, ${spec}, "${allTo}", params, &resp)
\tif err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                }
            case "DELETE":
                if (!useSpec) {
                    return """func (cli *ZSClient) ${clzName}(ctx context.Context, uuid string, deleteMode param.DeleteMode) error {
\treturn cli.Delete(ctx, "${cleanPath}", uuid, string(deleteMode))
}
"""
                } else {
                    // Multiple placeholders: use DeleteWithSpec
                    String firstParam = toSafeGoParamName(placeholders[0])
                    def remainingPlaceholders = placeholders.drop(1)
                    String params = placeholders.collect { "${toSafeGoParamName(it)} string" }.join(", ")
                    String spec = buildSpecPath(remainingPlaceholders)
                    String paramsStr = "fmt.Sprintf(\"deleteMode=%s\", deleteMode)"

                    return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${params}, deleteMode param.DeleteMode) error {
	return cli.DeleteWithSpec(ctx, "${cleanPath}", ${firstParam}, ${spec}, ${paramsStr}, nil)
}
"""
                }
            default:
                if (!useSpec) {
                    return """func (cli *ZSClient) ${clzName}(ctx context.Context, params param.${clzName}Param) (*view.${viewStructName}, error) {
\t${respDecl}
\tif err := cli.Post(ctx, "${cleanPath}", params, &resp); err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                } else {
                    // POST lacks *WithSpec helpers; build the full URL manually
                    String params = placeholders.collect { "${toSafeGoParamName(it)} string" }.join(", ")
                    String fullPath = buildFullPath(placeholders)
                    return """func (cli *ZSClient) ${clzName}(ctx context.Context, ${params}, params param.${clzName}Param) (*view.${viewStructName}, error) {
\t${respDecl}
\terr := cli.Post(ctx, ${fullPath}, params, &resp)
\tif err != nil {
\t\treturn nil, err
\t}
\t${returnStmt}
}
"""
                }
        }
    }

    /**
     * Decide whether an async method should be generated.
     */
    private boolean shouldGenerateAsync() {
        // Skip async generation for Query/Get/Delete operations
        if (actionType in ["Query", "Get", "Delete", "Destroy", "Remove", "Expunge"]) {
            return false
        }
        return true
    }

    /**
     * Generate async helper returning LongJob UUID for status polling.
     */
    private String generateAsyncMethod(String apiPath, String httpMethod, String viewStructName) {
        if (httpMethod != "POST") {
            logger.warn("[GoSDK] Skip async generation for non-POST API ${clzName}: ${httpMethod}")
            return ""
        }
        String asyncMethodName = "${clzName}Async"
        String resource = apiPath.replaceAll(/^v1\/?/, "")

        def builder = new StringBuilder()
        builder.append("\n// ${asyncMethodName} Async\n")
        builder.append("func (cli *ZSClient) ${asyncMethodName}(ctx context.Context, params param.${clzName}Param) (string, error) {\n")
        builder.append("\n")
        builder.append("\tresource := \"${resource}\"\n")
        builder.append("\tresponseKey := \"\"\n")
        builder.append("\tvar retVal interface{}\n")
        builder.append("\n")
        builder.append("\tapiId, err := cli.PostWithAsync(ctx, resource, responseKey, params, retVal, true)\n")
        builder.append("\tif err != nil {\n")
        builder.append("\t\treturn \"\", err\n")
        builder.append("\t}\n")
        builder.append("\n")
        builder.append("\treturn apiId, nil\n")
        builder.append("}\n")

        return builder.toString()
    }

    /**
     * Extract placeholders from a URL path.
     * Example: "/l3-networks/{l3NetworkUuid}/ip/{ip}/availability" -> ["l3NetworkUuid", "ip"]
     */
    private List<String> extractUrlPlaceholders(String apiPath) {
        def placeholders = []
        def matcher = (apiPath =~ /\{([^}]+)\}/)
        while (matcher.find()) {
            placeholders.add(matcher.group(1))
        }
        return placeholders
    }

    /**
     * Convert placeholder names to safe Go parameter names (avoid keyword clashes).
     */
    private String toSafeGoParamName(String name) {
        // Go keyword list
        def goKeywords = ["break", "case", "chan", "const", "continue", "default", "defer",
                          "else", "fallthrough", "for", "func", "go", "goto", "if", "import",
                          "interface", "map", "package", "range", "return", "select", "struct",
                          "switch", "type", "var"]

        if (goKeywords.contains(name)) {
            return name + "Param"
        }
        return name
    }

    /**
     * Build a spec path (excluding the first placeholder).
     * Example: ["ip", "availability"] with original path "/l3-networks/{l3NetworkUuid}/ip/{ip}/availability"
     * Returns: fmt.Sprintf("ip/%s/availability", ip)
     */
    private String buildSpecPath(List<String> remainingPlaceholders) {
        if (remainingPlaceholders.isEmpty()) {
            return '""'
        }

        // Locate the path segment after the first placeholder
        int firstPlaceholderEnd = path.indexOf('}') + 1
        if (firstPlaceholderEnd <= 0) {
            return '""'
        }

        String pathAfterFirst = path.substring(firstPlaceholderEnd)
        if (pathAfterFirst.startsWith('/')) {
            pathAfterFirst = pathAfterFirst.substring(1)
        }

        // Replace remaining {placeholder} segments with %s
        String formatStr = pathAfterFirst.replaceAll(/\{[^}]+\}/, '%s')

        if (remainingPlaceholders.isEmpty()) {
            return "\"${formatStr}\""
        }

        // Build the fmt.Sprintf call
        String params = remainingPlaceholders.collect { toSafeGoParamName(it) }.join(', ')
        return "fmt.Sprintf(\"${formatStr}\", ${params})"
    }

    /**
     * Build the full path (used when no *WithSpec helper exists, such as POST).
     * Example: ["eipUuid", "vmNicUuid"] with path "/eips/{eipUuid}/vm-instances/nics/{vmNicUuid}"
     * Returns: fmt.Sprintf("v1/eips/%s/vm-instances/nics/%s", eipUuid, vmNicUuid)
     */
    private String buildFullPath(List<String> placeholders) {
        if (placeholders.isEmpty()) {
            return "\"${path}\""
        }

        // Replace all {placeholder} tokens with %s
        String formatStr = path.replaceAll(/\{[^}]+\}/, '%s')

        // Add the v1 prefix
        if (!formatStr.startsWith("v1/")) {
            if (formatStr.startsWith("/")) {
                formatStr = "v1" + formatStr
            } else {
                formatStr = "v1/" + formatStr
            }
        }

        // Build the fmt.Sprintf call
        String params = placeholders.collect { toSafeGoParamName(it) }.join(', ')
        return "fmt.Sprintf(\"${formatStr}\", ${params})"
    }

    /**
     * Remove all placeholders from a URL.
     * Example: "/l3-networks/{l3NetworkUuid}/ip/{ip}/availability" -> "/l3-networks"
     * Keep the base path for GetWithSpec and similar helpers.
     */
    private String removePlaceholders(String apiPath) {
        // Find the position of the first '{'
        int firstPlaceholder = apiPath.indexOf('{')
        if (firstPlaceholder == -1) {
            return apiPath
        }

        // Return the path before the first placeholder, trimming the trailing slash
        String basePath = apiPath.substring(0, firstPlaceholder)
        if (basePath.endsWith('/')) {
            basePath = basePath.substring(0, basePath.length() - 1)
        }
        return basePath
    }

    /**
     * Build a path spec string for GetWithSpec and similar helpers.
     * Example: ["l3NetworkUuid", "ip"] -> 'fmt.Sprintf("%s/ip/%s/availability", l3NetworkUuid, ip)'
     *
     * Steps:
     * 1. Extract the path segment after placeholders.
     * 2. Replace placeholders with %s.
     * 3. Build fmt.Sprintf using safe parameter names.
     */
    private String buildPathSpec(List<String> placeholders) {
        if (placeholders.isEmpty()) {
            return '""'
        }

        // Re-parse the original path to build the portion after placeholders
        String pathAfterBase = path
        int firstPlaceholder = path.indexOf('{')
        if (firstPlaceholder != -1) {
            pathAfterBase = path.substring(firstPlaceholder)
        }

        // Replace {placeholder} with %s
        String formatStr = pathAfterBase.replaceAll(/\{[^}]+\}/, '%s')

        // Drop a leading slash if present
        if (formatStr.startsWith('/')) {
            formatStr = formatStr.substring(1)
        }

        // Build the fmt.Sprintf call with safe parameter names
        String params = placeholders.collect { toSafeGoParamName(it) }.join(', ')
        return "fmt.Sprintf(\"${formatStr}\", ${params})"
    }
}

