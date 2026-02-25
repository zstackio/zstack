package scripts

import org.zstack.core.Platform
import org.zstack.header.longjob.LongJobFor
import org.zstack.header.message.APIParam
import org.zstack.header.message.OverriddenApiParam
import org.zstack.header.message.OverriddenApiParams
import org.zstack.header.rest.APINoSee
import org.zstack.header.rest.RestRequest
import org.zstack.header.rest.RestResponse
import org.zstack.header.search.Inventory
import org.zstack.rest.sdk.SdkFile
import org.zstack.rest.sdk.SdkTemplate
import org.zstack.utils.FieldUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.stream.Collectors

/**
 * Go SDK Inventory/View generator
 */
class GoInventory implements SdkTemplate {
    private static final CLogger logger = Utils.getLogger(GoInventory.class)
    private Set<Class> inventories = new HashSet<>()
    private Set<Class> markedInventories = new HashSet<>()
    private Map<Class, String> viewStructNameMap = new HashMap<>()

    // Track additional classes that need View generation (referenced but not @Inventory annotated)
    private Set<Class> additionalClasses = new HashSet<>()
    // Track all generated view struct names to avoid duplicates
    private Set<String> generatedViewStructs = new HashSet<>()
    // Track generated view files to avoid duplicates
    private Set<String> generatedViewFiles = new HashSet<>()

    // Track LongJob mappings: API class -> LongJob class
    private static Map<Class, Class> longJobMappings = new HashMap<>()

    // Track param nested types (types used in API request params)
    private Set<Class> paramNestedTypes = new HashSet<>()
    // Track generated param struct names to avoid duplicates
    private Set<String> generatedParamStructs = new HashSet<>()

    // Track generated client method names to avoid duplicates across all action files
    private Set<String> generatedClientMethods = new HashSet<>()

    // Flag to indicate we're generating for param (not view)
    private boolean generatingForParam = false

    // Track current class being generated (for detecting self-references/recursive types)
    private Class currentGeneratingClass = null

    // Centralized list of all pre-parsed API templates
    private List<GoApiTemplate> allApiTemplates = new ArrayList<>()

    /**
     * Get all registered inventory classes
     */
    Set<Class> getInventories() {
        if (inventories.isEmpty()) {
            inventories.addAll(Platform.reflections.getTypesAnnotatedWith(Inventory.class))
        }
        return inventories
    }

    /**
     * Scan all @LongJobFor annotations and build API -> LongJob mappings
     */
    private void scanLongJobMappings() {
        logger.warn("[GoSDK] Scanning @LongJobFor annotations...")

        try {
            // Fetch all classes annotated with @LongJobFor
            Set<Class<?>> longJobClasses = Platform.reflections.getTypesAnnotatedWith(LongJobFor.class)

            longJobClasses.each { Class longJobClass ->
                try {
                    // Read the target API class from the annotation
                    LongJobFor annotation = longJobClass.getAnnotation(LongJobFor.class)
                    Class targetApiClass = annotation.value()

                    // Store the mapping
                    longJobMappings.put(targetApiClass, longJobClass)

                    logger.debug("[GoSDK] Found LongJob: ${longJobClass.simpleName} for API: ${targetApiClass.simpleName}")
                } catch (Exception e) {
                    logger.warn("[GoSDK] Failed to process LongJob class ${longJobClass.name}: ${e.message}")
                }
            }

            logger.warn("[GoSDK] Total LongJob mappings: ${longJobMappings.size()}")
        } catch (Exception e) {
            logger.error("[GoSDK] Failed to scan LongJob mappings: ${e.message}")
        }
    }

    /**
     * Get LongJob mappings (for GoApiTemplate)
     */
    static Map<Class, Class> getLongJobMappings() {
        return longJobMappings
    }

    /**
     * Pre-analyze all API classes once and cache metadata.
     * This avoids expensive re-instantiation of GoApiTemplate and redundant logging.
     */
    private void prepareApiTemplates() {
        logger.warn("[GoSDK] Pre-parsing all API classes...")
        GoApiTemplate.setKnownInventoryClasses(getInventories())
        GoApiTemplate.setLongJobMappings(getLongJobMappings())
        Set<Class<?>> allApiClasses = Platform.reflections.getTypesAnnotatedWith(RestRequest.class)

        allApiClasses.each { Class<?> apiClass ->
            if (apiClass.isInterface() || Modifier.isAbstract(apiClass.getModifiers())) {
                return
            }

            try {
                GoApiTemplate template = new GoApiTemplate(apiClass, this)
                // If it's a valid template (has @RestRequest)
                if (template.at != null) {
                    allApiTemplates.add(template)
                }
            } catch (Throwable e) {
                logger.warn("[GoSDK] Error pre-parsing API class ${apiClass.name}: ${e.class.name}: ${e.message}", e)
            }
        }
        logger.warn("[GoSDK] Pre-parsing complete. Cached ${allApiTemplates.size()} API templates.")
    }

    /**
     * Validate generated views against referenced response views
     */
    private void validateGeneratedViews() {
        int missingCount = 0
        allApiTemplates.each { GoApiTemplate template ->
            Class<?> responseClass = template.getResponseClass()
            if (responseClass != null) {
                String viewName = getViewStructName(responseClass)
                if (!generatedViewStructs.contains(viewName)) {
                    logger.warn("[GoSDK] Reference to missing view: ${viewName} (referenced by ${template.getApiMsgClazz().simpleName})")
                    missingCount++
                }
            }
        }
        if (missingCount > 0) {
            logger.warn("[GoSDK] Total missing response views: ${missingCount}. These APIs might fail to compile in Go.")
        } else {
            logger.warn("[GoSDK] View validation passed. All referenced response views are generated.")
        }
    }

    @Override
    List<SdkFile> generate() {
        def files = []

        logger.warn("[GoSDK] ===== GoInventory.generate() START =====")
        logger.warn("[GoSDK] GoInventory.generate() starting...")

        // 0. Scan LongJob mappings
        scanLongJobMappings()
        logger.warn("[GoSDK] Scanned ${longJobMappings.size()} LongJob mappings")

        // Ensure inventories are loaded early
        getInventories()
        logger.warn("[GoSDK] Loaded " + inventories.size() + " inventories")

        // 1. Pre-parse all APIs once to avoid O(N*M) processing and redundant logging
        prepareApiTemplates()
        logger.warn("[GoSDK] prepareApiTemplates complete. Found " + allApiTemplates.size() + " valid API templates")

        // 1. Generate view files (Resource Grouping)
        logger.warn("[GoSDK] Starting generateViewFiles()...")
        files.addAll(generateViewFiles())
        logger.warn("[GoSDK] Completed generateViewFiles(). Generated " + generatedViewStructs.size() + " view structs")

        // 1b. Generate action and param files (Resource Grouping)
        files.addAll(generateActionFiles())
        files.addAll(generateParamFiles())

        // 2. Generate catch-all other views (for APIs without a matching @Inventory class)
        logger.warn("[GoSDK] Before other_views.go, generatedViewStructs size: " + generatedViewStructs.size())
        files.add(generateOtherViewsFile())
        logger.warn("[GoSDK] After other_views.go, generatedViewStructs size: " + generatedViewStructs.size())


        // 4. Generate other params file
        files.add(generateOtherParamsFile())

        // 5. Generate other actions file
        files.add(generateOtherActionsFile())

        // 6. Generate view files for additional referenced classes (iterative discovery)
        files.addAll(generateAdditionalViewFiles())

        // 7. Base files
        files.add(generateBaseViewFile())

        // Load session additional views from template (special case for WebUISessionView)
        files.add(loadSessionViewsTemplate())

        files.add(loadBaseParamTemplate())

        // Generate param nested types file from template
        files.add(loadBaseParamTypesTemplate())

        // Generate login params from template (special case)
        files.add(loadLoginParamsTemplate())

        // Note: client.go is manually maintained, not auto-generated

        // 8. Generate test files (unit tests + integration tests)
        def testTemplate = new GoTestTemplate(this, allApiTemplates, inventories)
        def testFiles = testTemplate.generate()
        files.addAll(testFiles)
        logger.warn("[GoSDK] Generated ${testFiles.size()} test files")

        // 9. Validate that all referenced response views were generated
        validateGeneratedViews()

        logger.warn("[GoSDK] GoInventory.generate() complete. Total files: " + files.size())
        logger.warn("[GoSDK] ===== GoInventory.generate() END =====")
        return files
    }

    /**
     * Generate view files for additional classes (referenced but not @Inventory annotated)
     * These are classes like SnapshotLeafInventory, ServiceStatus, etc.
     * Uses iterative approach to handle newly discovered types during generation.
     */
    private List<SdkFile> generateAdditionalViewFiles() {
        def files = []

        if (additionalClasses.isEmpty()) {
            return files
        }

        logger.warn("[GoSDK] Generating additional view classes, initial count: " + additionalClasses.size())

        // Use a list to iterate and allow adding new classes during iteration
        def classesToProcess = new ArrayList<Class>(additionalClasses)
        def discoveredClasses = new HashSet<Class>()  // Track discovered classes (not generated yet)
        int index = 0

        // First pass: discover all classes and their dependencies
        // Don't add to generatedViewStructs here - just collect classes
        while (index < classesToProcess.size()) {
            Class clz = classesToProcess.get(index)
            String structName = getViewStructName(clz)

            // Skip if already in @Inventory or already discovered
            if (!discoveredClasses.contains(clz) && !generatedViewStructs.contains(structName)) {
                discoveredClasses.add(clz)

                // Call generateViewStruct to trigger dependency discovery
                // (it adds new types to additionalClasses)
                generateViewStruct(clz, structName)

                // Check if new classes were added during struct generation
                additionalClasses.each { Class newClz ->
                    String newStructName = getViewStructName(newClz)
                    if (!classesToProcess.contains(newClz) &&
                            !discoveredClasses.contains(newClz) &&
                            !generatedViewStructs.contains(newStructName)) {
                        classesToProcess.add(newClz)
                    }
                }
            }
            index++
        }

        logger.warn("[GoSDK] Total additional classes after discovery: " + discoveredClasses.size())

        // Group discovered classes by simple name prefix for file organization
        def grouped = new HashMap<String, Set<Class>>()
        discoveredClasses.each { Class clz ->
            String prefix = clz.simpleName.replaceAll('Inventory$', '').replaceAll('VO$', '')
            if (!grouped.containsKey(prefix)) {
                grouped.put(prefix, new HashSet<Class>())
            }
            grouped.get(prefix).add(clz)
        }

        grouped.each { String prefix, Set<Class> classes ->
            def sdkFile = new SdkFile()
            sdkFile.subPath = "/pkg/view/"
            sdkFile.fileName = "${toSnakeCase(prefix)}_additional_views.go"

            def content = new StringBuilder()
            content.append("// Copyright (c) ZStack.io, Inc.\n\n")
            content.append("package view\n\n")
            content.append("import \"time\"\n\n")
            content.append("var _ = time.Now // avoid unused import\n\n")

            classes.each { Class clz ->
                String structName = getViewStructName(clz)
                if (!generatedViewStructs.contains(structName)) {
                    generatedViewStructs.add(structName)
                    content.append(generateViewStruct(clz, structName))
                    logger.debug("[GoSDK] Generated additional view: " + structName)
                }
            }

            sdkFile.content = content.toString()
            files.add(sdkFile)
        }

        return files
    }

    /**
     * Generate ZSClient base file
     * @deprecated client.go is manually maintained, this method should not be used
     */
    @Deprecated
    private SdkFile generateClientFile() {
        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/client/"
        sdkFile.fileName = "client.go"

        def content = new StringBuilder()
        content.append("// Copyright (c) ZStack.io, Inc.\n\n")
        content.append("package client\n\n")
        content.append("import (\n")
        content.append("\t\"bytes\"\n")
        content.append("\t\"crypto/sha512\"\n")
        content.append("\t\"encoding/hex\"\n")
        content.append("\t\"encoding/json\"\n")
        content.append("\t\"fmt\"\n")
        content.append("\t\"io\"\n")
        content.append("\t\"net/http\"\n")
        content.append("\t\"net/url\"\n")
        content.append("\t\"strconv\"\n")
        content.append("\t\"strings\"\n")
        content.append("\t\"github.com/zstackio/zstack-sdk-go-v2/pkg/param\"\n")
        content.append("\t\"time\"\n")
        content.append(")\n\n")
        content.append("// AuthType authentication type\n")
        content.append("type AuthType string\n\n")
        content.append("const (\n")
        content.append("\tAuthTypeAccessKey AuthType = \"accesskey\"\n")
        content.append("\tAuthTypeLogin    AuthType = \"login\"\n")
        content.append(")\n\n")
        content.append("const (\n")
        content.append("\tdefaultZStackPort        = 8080\n")
        content.append(")\n\n")
        content.append("// ZSConfig client configuration\n")
        content.append("type ZSConfig struct {\n")
        content.append("\thostname        string\n")
        content.append("\tport            int\n")
        content.append("\tcontextPath     string\n")
        content.append("\taccessKeyId     string\n")
        content.append("\taccessKeySecret string\n")
        content.append("\tusername        string\n")
        content.append("\tpassword        string\n")
        content.append("\tauthType        AuthType\n")
        content.append("\tdebug           bool\n")
        content.append("\ttimeout         time.Duration\n")
        content.append("}\n\n")
        content.append("// NewZSConfig creates a new configuration\n")
        content.append("func NewZSConfig(hostname string, port int, contextPath string) *ZSConfig {\n")
        content.append("\treturn &ZSConfig{\n")
        content.append("\t\thostname:    hostname,\n")
        content.append("\t\tport:        port,\n")
        content.append("\t\tcontextPath: contextPath,\n")
        content.append("\t\ttimeout:     30 * time.Second,\n")
        content.append("\t}\n")
        content.append("}\n\n")
        content.append("// DefaultZSConfig creates a default configuration\n")
        content.append("func DefaultZSConfig(hostname, contextPath string) *ZSConfig {\n")
        content.append("\treturn NewZSConfig(hostname, defaultZStackPort, contextPath)\n")
        content.append("}\n\n")
        content.append("// AccessKey sets access key authentication\n")
        content.append("func (config *ZSConfig) AccessKey(id, secret string) *ZSConfig {\n")
        content.append("\tconfig.accessKeyId = id\n")
        content.append("\tconfig.accessKeySecret = secret\n")
        content.append("\tconfig.authType = AuthTypeAccessKey\n")
        content.append("\treturn config\n")
        content.append("}\n\n")
        content.append("// Login sets login authentication\n")
        content.append("func (config *ZSConfig) Login(username, password string) *ZSConfig {\n")
        content.append("\tconfig.username = username\n")
        content.append("\tconfig.password = password\n")
        content.append("\tconfig.authType = AuthTypeLogin\n")
        content.append("\treturn config\n")
        content.append("}\n\n")
        content.append("// Debug enables debug mode\n")
        content.append("func (config *ZSConfig) Debug(debug bool) *ZSConfig {\n")
        content.append("\tconfig.debug = debug\n")
        content.append("\treturn config\n")
        content.append("}\n\n")
        content.append("// ZSClient ZStack API client\n")
        content.append("type ZSClient struct {\n")
        content.append("\tconfig     *ZSConfig\n")
        content.append("\thttpClient *http.Client\n")
        content.append("\tsessionId  string\n")
        content.append("}\n\n")
        content.append("// JobView job inventory view\n")
        content.append("type JobView struct {\n")
        content.append("\tUUID       string      `json:\"uuid\"`\n")
        content.append("\tState      string      `json:\"state\"`\n")
        content.append("\tResult     interface{} `json:\"result,omitempty\"`\n")
        content.append("\tError      interface{} `json:\"error,omitempty\"`\n")
        content.append("\tCreateDate string      `json:\"createDate\"`\n")
        content.append("}\n\n")
        content.append("const (\n")
        content.append("\tJobStateProcessing = \"Processing\"\n")
        content.append("\tJobStateSucceeded  = \"Succeeded\"\n")
        content.append("\tJobStateFailed     = \"Failed\"\n")
        content.append(")\n\n")
        content.append("// NewZSClient creates a new ZStack client\n")
        content.append("func NewZSClient(config *ZSConfig) *ZSClient {\n")
        content.append("\t// Auto-encrypt password for login authentication\n")
        content.append("\tif config.authType == AuthTypeLogin && config.password != \"\" {\n")
        content.append("\t\tconfig.password = hashPasswordSHA512(config.password)\n")
        content.append("\t\tif config.debug {\n")
        content.append("\t\t\tfmt.Printf(\"[DEBUG] Password hashed: %s...\\n\", config.password[:16])\n")
        content.append("\t\t}\n")
        content.append("\t}\n")
        content.append("\treturn &ZSClient{\n")
        content.append("\t\tconfig: config,\n")
        content.append("\t\thttpClient: &http.Client{\n")
        content.append("\t\t\tTimeout: config.timeout,\n")
        content.append("\t\t},\n")
        content.append("\t}\n")
        content.append("}\n\n")
        content.append("// hashPasswordSHA512 encrypts password using SHA512\n")
        content.append("func hashPasswordSHA512(password string) string {\n")
        content.append("\thash := sha512.Sum512([]byte(password))\n")
        content.append("\treturn hex.EncodeToString(hash[:])\n")
        content.append("}\n\n")
        content.append("func (cli *ZSClient) baseURL() string {\n")
        content.append("\treturn fmt.Sprintf(\"http://%s:%d%s\", cli.config.hostname, cli.config.port, cli.config.contextPath)\n")
        content.append("}\n\n")
        content.append("// Get performs a GET request\n")
        content.append("func (cli *ZSClient) Get(path string, uuid string, params interface{}, result interface{}) error {\n")
        content.append("\turl := fmt.Sprintf(\"%s/%s\", cli.baseURL(), path)\n")
        content.append("\tif uuid != \"\" {\n")
        content.append("\t\turl = fmt.Sprintf(\"%s/%s\", url, uuid)\n")
        content.append("\t}\n")
        content.append("\treturn cli.doRequest(\"GET\", url, nil, result)\n")
        content.append("}\n\n")
        content.append("func (cli *ZSClient) QueryJob(uuid string) (*JobView, error) {\n")
        content.append("\tvar resp JobView\n")
        content.append("\turl := fmt.Sprintf(\"%s/v1/api-jobs/%s\", cli.baseURL(), uuid)\n")
        content.append("\terr := cli.doRequest(\"GET\", url, nil, &resp)\n")
        content.append("\treturn &resp, err\n")
        content.append("}\n\n")
        content.append("// List performs a list query\n")
        content.append("func (cli *ZSClient) List(path string, params interface{}, result interface{}) error {\n")
        content.append("\tbaseURL := cli.baseURL()\n")
        content.append("\trequestURL := fmt.Sprintf(\"%s/%s\", baseURL, path)\n")
        content.append("\n")
        content.append("\tif params != nil {\n")
        content.append("\t\tif queryParam, ok := params.(*param.QueryParam); ok {\n")
        content.append("\t\t\tqueryString := cli.buildQueryString(queryParam)\n")
        content.append("\t\t\tif queryString != \"\" {\n")
        content.append("\t\t\t\trequestURL = fmt.Sprintf(\"%s?%s\", requestURL, queryString)\n")
        content.append("\t\t\t}\n")
        content.append("\t\t}\n")
        content.append("\t}\n")
        content.append("\n")
        content.append("\t// Unmarshal response into wrapper with inventories field\n")
        content.append("\tvar wrapper struct {\n")
        content.append("\t\tInventories interface{} `json:\"inventories\"`\n")
        content.append("\t\tInventory   interface{} `json:\"inventory\"`\n")
        content.append("\t}\n")
        content.append("\n")
        content.append("\tif err := cli.doRequest(\"GET\", requestURL, nil, &wrapper); err != nil {\n")
        content.append("\t\treturn err\n")
        content.append("\t}\n")
        content.append("\n")
        content.append("\t// Try inventories first (plural), then inventory (singular)\n")
        content.append("\tvar data interface{}\n")
        content.append("\tif wrapper.Inventories != nil {\n")
        content.append("\t\tdata = wrapper.Inventories\n")
        content.append("\t} else if wrapper.Inventory != nil {\n")
        content.append("\t\tdata = wrapper.Inventory\n")
        content.append("\t}\n")
        content.append("\n")
        content.append("\t// Re-marshal and unmarshal into the actual result type\n")
        content.append("\tif data != nil {\n")
        content.append("\t\tdataBytes, err := json.Marshal(data)\n")
        content.append("\t\tif err != nil {\n")
        content.append("\t\t\treturn fmt.Errorf(\"failed to marshal data: %v\", err)\n")
        content.append("\t\t}\n")
        content.append("\t\tif cli.config.debug {\n")
        content.append("\t\t\tfmt.Printf(\"[DEBUG] Received %d bytes of inventory data\\n\", len(dataBytes))\n")
        content.append("\t\t}\n")
        content.append("\t\terr = json.Unmarshal(dataBytes, result)\n")
        content.append("\t\tif err != nil {\n")
        content.append("\t\t\treturn fmt.Errorf(\"failed to unmarshal data into result: %v\", err)\n")
        content.append("\t\t}\n")
        content.append("\t\treturn nil\n")
        content.append("\t}\n")
        content.append("\tif cli.config.debug {\n")
        content.append("\t\tfmt.Println(\"[DEBUG] Both inventories and inventory are nil, returning empty result\")\n")
        content.append("\t}\n")
        content.append("\treturn nil\n")
        content.append("}\n\n")
        content.append("// Post performs a POST request\n")
        content.append("func (cli *ZSClient) Post(path string, params interface{}, result interface{}) error {\n")
        content.append("\turl := fmt.Sprintf(\"%s/%s\", cli.baseURL(), path)\n")
        content.append("\treturn cli.doRequest(\"POST\", url, params, result)\n")
        content.append("}\n\n")
        content.append("// Put performs a PUT request\n")
        content.append("func (cli *ZSClient) Put(path string, uuid string, params interface{}, result interface{}) error {\n")
        content.append("\turl := fmt.Sprintf(\"%s/%s/%s\", cli.baseURL(), path, uuid)\n")
        content.append("\treturn cli.doRequest(\"PUT\", url, params, result)\n")
        content.append("}\n\n")
        content.append("// Delete performs a DELETE request\n")
        content.append("func (cli *ZSClient) Delete(path string, uuid string, deleteMode string) error {\n")
        content.append("\turl := fmt.Sprintf(\"%s/%s/%s?deleteMode=%s\", cli.baseURL(), path, uuid, deleteMode)\n")
        content.append("\treturn cli.doRequest(\"DELETE\", url, nil, nil)\n")
        content.append("}\n\n")
        content.append("func (cli *ZSClient) doRequest(method, url string, body interface{}, result interface{}) error {\n")
        content.append("\t// Auto-login if using login auth and no session yet\n")
        content.append("\tif cli.config.authType == AuthTypeLogin && cli.sessionId == \"\" && !strings.HasSuffix(url, \"/accounts/login\") {\n")
        content.append("\t\terr := cli.Login(cli.config.username, cli.config.password)\n")
        content.append("\t\tif err != nil {\n")
        content.append("\t\t\treturn fmt.Errorf(\"auto-login failed: %v\", err)\n")
        content.append("\t\t}\n")
        content.append("\t}\n\n")
        content.append("\tvar bodyReader io.Reader\n")
        content.append("\tvar bodyBytes []byte\n")
        content.append("\tif body != nil {\n")
        content.append("\t\tvar err error\n")
        content.append("\t\tbodyBytes, err = json.Marshal(body)\n")
        content.append("\t\tif err != nil {\n")
        content.append("\t\t\treturn err\n")
        content.append("\t\t}\n")
        content.append("\t\tbodyReader = bytes.NewBuffer(bodyBytes)\n")
        content.append("\t}\n\n")
        content.append("\treq, err := http.NewRequest(method, url, bodyReader)\n")
        content.append("\tif err != nil {\n")
        content.append("\t\treturn err\n")
        content.append("\t}\n\n")
        content.append("\treq.Header.Set(\"Content-Type\", \"application/json\")\n")
        content.append("\tcli.addAuthHeaders(req)\n\n")
        content.append("\tif cli.config.debug && bodyBytes != nil {\n")
        content.append("\t\tfmt.Printf(\"[DEBUG] %s %s\\n\", method, url)\n")
        content.append("\t\tfmt.Printf(\"[DEBUG] Body: %s\\n\", string(bodyBytes))\n")
        content.append("\t\tfmt.Printf(\"[DEBUG] Headers: Authorization=%s\\n\", req.Header.Get(\"Authorization\"))\n")
        content.append("\t}\n\n")
        content.append("\tresp, err := cli.httpClient.Do(req)\n")
        content.append("\tif err != nil {\n")
        content.append("\t\treturn err\n")
        content.append("\t}\n")
        content.append("\tdefer resp.Body.Close()\n\n")
        content.append("\tif resp.StatusCode == 202 {\n")
        content.append("\t\tvar location struct {\n")
        content.append("\t\t\tLocation string `json:\"location\"`\n")
        content.append("\t\t\tUuid     string `json:\"org.zstack.header.rest.APIEvent/uuid\"`\n")
        content.append("\t\t}\n")
        content.append("\t\tif err := json.NewDecoder(resp.Body).Decode(&location); err != nil {\n")
        content.append("\t\t\treturn fmt.Errorf(\"failed to decode 202 response: %v\", err)\n")
        content.append("\t\t}\n")
        content.append("\t\tjobUUID := location.Uuid\n")
        content.append("\t\tif jobUUID == \"\" {\n")
        content.append("\t\t\tparts := bytes.Split([]byte(location.Location), []byte(\"/\"))\n")
        content.append("\t\t\tif len(parts) > 0 {\n")
        content.append("\t\t\t\tjobUUID = string(parts[len(parts)-1])\n")
        content.append("\t\t\t}\n")
        content.append("\t\t}\n")
        content.append("\n")
        content.append("\t\tif jobUUID == \"\" {\n")
        content.append("\t\t\treturn fmt.Errorf(\"failed to extract job uuid from 202 response\")\n")
        content.append("\t\t}\n")
        content.append("\n")
        content.append("\t\treturn cli.waitForJob(jobUUID, result)\n")
        content.append("\t}\n\n")
        content.append("\tif resp.StatusCode >= 400 {\n")
        content.append("\t\trespBody, _ := io.ReadAll(resp.Body)\n")
        content.append("\t\terrMsg := fmt.Sprintf(\"API error: %s %s returned status code %d\\n\", method, url, resp.StatusCode)\n")
        content.append("\t\terrMsg += fmt.Sprintf(\"Authorization: %s\\n\", req.Header.Get(\"Authorization\"))\n")
        content.append("\t\terrMsg += fmt.Sprintf(\"Response: %s\", string(respBody))\n")
        content.append("\t\treturn fmt.Errorf(errMsg)\n")
        content.append("\t}\n\n")
        content.append("\tif result != nil {\n")
        content.append("\t\treturn json.NewDecoder(resp.Body).Decode(result)\n")
        content.append("\t}\n")
        content.append("\treturn nil\n")
        content.append("}\n\n")
        content.append("func (cli *ZSClient) waitForJob(jobUUID string, result interface{}) error {\n")
        content.append("\tticker := time.NewTicker(500 * time.Millisecond)\n")
        content.append("\tdefer ticker.Stop()\n")
        content.append("\n")
        content.append("\ttimeout := time.After(30 * time.Minute)\n")
        content.append("\n")
        content.append("\tfor {\n")
        content.append("\t\tselect {\n")
        content.append("\t\tcase <-timeout:\n")
        content.append("\t\t\treturn fmt.Errorf(\"job %s timeout\", jobUUID)\n")
        content.append("\t\tcase <-ticker.C:\n")
        content.append("\t\t\tjob, err := cli.QueryJob(jobUUID)\n")
        content.append("\t\t\tif err != nil {\n")
        content.append("\t\t\t\tcontinue\n")
        content.append("\t\t\t}\n")
        content.append("\n")
        content.append("\t\t\tif job.State == JobStateSucceeded {\n")
        content.append("\t\t\t\tif result != nil && job.Result != nil {\n")
        content.append("\t\t\t\t\tdata, err := json.Marshal(job.Result)\n")
        content.append("\t\t\t\t\tif err != nil {\n")
        content.append("\t\t\t\t\t\treturn fmt.Errorf(\"failed to marshal job result: %v\", err)\n")
        content.append("\t\t\t\t\t}\n")
        content.append("\t\t\t\t\treturn json.Unmarshal(data, result)\n")
        content.append("\t\t\t\t}\n")
        content.append("\t\t\t\treturn nil\n")
        content.append("\t\t\t}\n")
        content.append("\n")
        content.append("\t\t\tif job.State == JobStateFailed {\n")
        content.append("\t\t\t\treturn fmt.Errorf(\"job failed: %v\", job.Error)\n")
        content.append("\t\t\t}\n")
        content.append("\t\t}\n")
        content.append("\t}\n")
        content.append("}\n\n")
        content.append("func (cli *ZSClient) buildQueryString(params *param.QueryParam) string {\n")
        content.append("\tif params == nil {\n")
        content.append("\t\treturn \"\"\n")
        content.append("\t}\n")
        content.append("\tu := url.Values{}\n")
        content.append("\n")
        content.append("\tfor _, q := range params.Conditions {\n")
        content.append("\t\tif q.Name != \"\" && q.Op != \"\" {\n")
        content.append("\t\t\tu.Add(\"q\", fmt.Sprintf(\"%s%s%s\", q.Name, q.Op, q.Value))\n")
        content.append("\t\t} else if q.Value != \"\" {\n")
        content.append("\t\t\tu.Add(\"q\", q.Value)\n")
        content.append("\t\t}\n")
        content.append("\t}\n")
        content.append("\n")
        content.append("\tif params.LimitNum != nil {\n")
        content.append("\t\tu.Set(\"limit\", strconv.Itoa(*params.LimitNum))\n")
        content.append("\t}\n")
        content.append("\tif params.StartNum != nil {\n")
        content.append("\t\tu.Set(\"start\", strconv.Itoa(*params.StartNum))\n")
        content.append("\t}\n")
        content.append("\tif params.Count {\n")
        content.append("\t\tu.Set(\"count\", \"true\")\n")
        content.append("\t}\n")
        content.append("\tif params.ReplyWithCount {\n")
        content.append("\t\tu.Set(\"replyWithCount\", \"true\")\n")
        content.append("\t}\n")
        content.append("\tif params.GroupBy != \"\" {\n")
        content.append("\t\tu.Set(\"groupBy\", params.GroupBy)\n")
        content.append("\t}\n")
        content.append("\tif params.SortBy != \"\" {\n")
        content.append("\t\tu.Set(\"sortBy\", params.SortBy)\n")
        content.append("\t}\n")
        content.append("\tif params.SortDirection != \"\" {\n")
        content.append("\t\tu.Set(\"sortDirection\", params.SortDirection)\n")
        content.append("\t}\n")
        content.append("\tfor _, f := range params.Fields {\n")
        content.append("\t\tu.Add(\"fields\", f)\n")
        content.append("\t}\n")
        content.append("\n")
        content.append("\treturn u.Encode()\n")
        content.append("}\n\n")
        content.append("func (cli *ZSClient) addAuthHeaders(req *http.Request) {\n")
        content.append("\tif cli.config.authType == AuthTypeAccessKey {\n")
        content.append("\t\treq.Header.Set(\"X-Access-Key-Id\", cli.config.accessKeyId)\n")
        content.append("\t\treq.Header.Set(\"X-Access-Key-Secret\", cli.config.accessKeySecret)\n")
        content.append("\t} else if cli.sessionId != \"\" {\n")
        content.append("\t\treq.Header.Set(\"Authorization\", \"OAuth \"+cli.sessionId)\n")
        content.append("\t}\n")
        content.append("}\n\n")
        content.append("// Login authenticates with username and password\n")
        content.append("func (cli *ZSClient) Login(username, password string) error {\n")
        content.append("\tif cli.config.authType != AuthTypeLogin {\n")
        content.append("\t\treturn fmt.Errorf(\"client is not configured for login authentication\")\n")
        content.append("\t}\n\n")
        content.append("\tvar loginReq = map[string]map[string]string{\n")
        content.append("\t\t\"logInByAccount\": {\n")
        content.append("\t\t\t\"accountName\": username,\n")
        content.append("\t\t\t\"password\":    password, // Already hashed in NewZSClient\n")
        content.append("\t\t},\n")
        content.append("\t}\n\n")
        content.append("\tvar loginResp struct {\n")
        content.append("\t\tInventory struct {\n")
        content.append("\t\t\tUUID string `json:\"uuid\"`\n")
        content.append("\t\t} `json:\"inventory\"`\n")
        content.append("\t}\n\n")
        content.append("\turl := fmt.Sprintf(\"%s/v1/accounts/login\", cli.baseURL())\n")
        content.append("\terr := cli.doRequest(\"PUT\", url, loginReq, &loginResp)\n")
        content.append("\tif err != nil {\n")
        content.append("\t\treturn fmt.Errorf(\"login failed: %v\", err)\n")
        content.append("\t}\n\n")
        content.append("\tcli.sessionId = loginResp.Inventory.UUID\n")
        content.append("\tif cli.config.debug {\n")
        content.append("\t\tfmt.Printf(\"[DEBUG] Login successful, sessionId=%s\\n\", cli.sessionId)\n")
        content.append("\t}\n")
        content.append("\treturn nil\n")
        content.append("}\n\n")
        content.append("func (cli *ZSClient) Logout() error {\n")
        content.append("\tif cli.sessionId == \"\" {\n")
        content.append("\t\treturn nil\n")
        content.append("\t}\n\n")
        content.append("\turl := fmt.Sprintf(\"%s/v1/accounts/sessions/%s\", cli.baseURL(), cli.sessionId)\n")
        content.append("\terr := cli.doRequest(\"DELETE\", url, nil, nil)\n")
        content.append("\tcli.sessionId = \"\"\n")
        content.append("\treturn err\n")
        content.append("}\n")

        sdkFile.content = content.toString()
        return sdkFile
    }

    /**
     * Generate base view file
     */
    private SdkFile generateBaseViewFile() {
        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/view/"
        sdkFile.fileName = "base_views.go"

        def content = new StringBuilder()
        content.append("// Copyright (c) ZStack.io, Inc.\n\n")
        content.append("package view\n\n")
        content.append("import \"time\"\n\n")
        content.append("// BaseInfoView holds common identity fields\n")
        content.append("type BaseInfoView struct {\n")
        content.append("\tUUID string `json:\"uuid\"`           // Unique resource identifier\n")
        content.append("\tName string `json:\"name,omitempty\"` // Resource name\n")
        content.append("}\n\n")

        content.append("type BaseTimeView struct {\n")
        content.append("\tCreateDate time.Time `json:\"createDate,omitempty\"` // Creation time\n")
        content.append("\tLastOpDate time.Time `json:\"lastOpDate,omitempty\"` // Last operation time\n")
        content.append("}\n\n")

        // Add generic wrapper types for simple return values
        content.append("// Generic wrapper types for APIs that return simple data types\n\n")
        content.append("// MapView wraps map return values\n")
        content.append("type MapView map[string]interface{}\n\n")
        content.append("// ListView wraps list/array return values\n")
        content.append("type ListView []interface{}\n\n")
        content.append("// StringView wraps string return values\n")
        content.append("type StringView string\n\n")
        content.append("// BooleanView wraps boolean return values\n")
        content.append("type BooleanView bool\n\n")
        content.append("// IntView wraps integer return values\n")
        content.append("type IntView int\n\n")
        content.append("// LongView wraps long integer return values\n")
        content.append("type LongView int64\n\n")
        content.append("// SuccessView represents successful operation with no data return\n")
        content.append("type SuccessView struct {\n")
        content.append("\tSuccess bool `json:\"success\"`\n")
        content.append("}\n")

        sdkFile.content = content.toString()
        return sdkFile
    }

    /**
     * Load base_params.go from template file
     */
    private SdkFile loadBaseParamTemplate() {
        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/param/"
        sdkFile.fileName = "base_params.go"

        try {
            def templateStream = this.class.getResourceAsStream("/scripts/templates/base_params.go.template")
            if (templateStream != null) {
                sdkFile.content = templateStream.text
                logger.warn("[GoSDK] Loaded base_params.go from template file")
            } else {
                logger.error("[GoSDK] base_params.go.template not found in classpath")
                throw new RuntimeException("base_params.go.template not found")
            }
        } catch (Exception e) {
            logger.error("[GoSDK] Failed to load base_params.go template: ${e.message}")
            throw e
        }

        return sdkFile
    }

    /**
     * Generate errors file
     */
    private SdkFile generateErrorsFile() {
        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/errors/"
        sdkFile.fileName = "errors.go"

        def content = new StringBuilder()
        content.append("// Copyright (c) ZStack.io, Inc.\n\n")
        content.append("package errors\n\n")
        content.append("import \"fmt\"\n\n")
        content.append("// Error custom error type\n")
        content.append("type Error string\n\n")
        content.append("func (e Error) Error() string {\n")
        content.append("\treturn string(e)\n")
        content.append("}\n\n")
        content.append("const (\n")
        content.append("\tErrNotFound    = Error(\"NotFoundError\")\n")
        content.append("\tErrDuplicateId = Error(\"DuplicateIdError\")\n")
        content.append("\tErrParameter   = Error(\"ParameterError\")\n")
        content.append("\tErrAuth        = Error(\"AuthError\")\n")
        content.append("\tErrPermission  = Error(\"PermissionError\")\n")
        content.append("\tErrInternal    = Error(\"InternalError\")\n")
        content.append(")\n\n")
        content.append("// Wrap wraps an error with a message\n")
        content.append("func Wrap(err error, message string) error {\n")
        content.append("\tif err == nil {\n")
        content.append("\t\treturn nil\n")
        content.append("\t}\n")
        content.append("\treturn fmt.Errorf(\"%s: %w\", message, err)\n")
        content.append("}\n\n")
        content.append("// ErrorCode API error code structure\n")
        content.append("type ErrorCode struct {\n")
        content.append("\tCode        string     `json:\"code\"`\n")
        content.append("\tDescription string     `json:\"description\"`\n")
        content.append("\tDetails     string     `json:\"details\"`\n")
        content.append("\tElaboration string     `json:\"elaboration\"`\n")
        content.append("\tCause       *ErrorCode `json:\"cause\"`\n")
        content.append("}\n")

        sdkFile.content = content.toString()
        return sdkFile
    }

    /**
     * Generate view files
     */

    /**
     * Group inventories by resource
     */

    /**
     * Get view struct name
     */
    String getViewStructName(Class clz) {
        if (viewStructNameMap.containsKey(clz)) {
            return viewStructNameMap.get(clz)
        }

        String name = clz.simpleName
        String structName

        if (name.endsWith("Inventory")) {
            structName = name.replace("Inventory", "InventoryView")
        } else if (name.endsWith("Reply")) {
            structName = name.replace("Reply", "View").replaceAll('^API', '')
        } else {
            structName = name + "View"
        }

        if (structName.startsWith("API")) {
            structName = structName.substring(3)
        }

        viewStructNameMap.put(clz, structName)
        return structName
    }

    /**
     * Convert to snake_case
     */
    private String toSnakeCase(String name) {
        return name.replaceAll('([a-z])([A-Z])', '$1_$2').toLowerCase()
    }

    /**
     * Find a field in the class hierarchy (including parent classes)
     */
    private Field findFieldInHierarchy(Class<?> clazz, String fieldName) {
        if (clazz == null || fieldName == null) {
            return null
        }

        // Search through all fields including inherited ones
        for (Field f : FieldUtils.getAllFields(clazz)) {
            if (f.name == fieldName) {
                return f
            }
        }

        // Manual search through hierarchy as fallback
        Class<?> current = clazz
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName)
            } catch (NoSuchFieldException e) {
                current = current.superclass
            }
        }

        return null
    }

    /**
     * Generate view struct
     */
    String generateViewStruct(Class<?> inventoryClazz, String structName) {
        if (inventoryClazz == null || structName == null) {
            return ""
        }

        logger.warn("[GoSDK] generateViewStruct called: inventoryClass=${inventoryClazz.simpleName}, structName=${structName}")

        // Track current class for detecting self-references
        Class previousClass = currentGeneratingClass
        currentGeneratingClass = inventoryClazz

        def apiParamMap = new HashMap<String, APIParam>()
        if (inventoryClazz.isAnnotationPresent(OverriddenApiParams.class)) {
            for (OverriddenApiParam oap : inventoryClazz.getAnnotation(OverriddenApiParams.class).value()) {
                apiParamMap.put(oap.field(), oap.param())
            }
        }

        def fieldMap = new LinkedHashMap<String, Field>()
        FieldUtils.getAllFields(inventoryClazz).each { Field f ->
            if (!Modifier.isStatic(f.modifiers)) {
                fieldMap.put(f.name, f)
            }
        }

        if (inventoryClazz.isAnnotationPresent(RestResponse.class)) {
            def at = inventoryClazz.getAnnotation(RestResponse.class)
            if (at.allTo() != "") {
                fieldMap = new LinkedHashMap<String, Field>()
                Field targetField = findFieldInHierarchy(inventoryClazz, at.allTo())
                if (targetField != null) {
                    fieldMap.put(at.allTo(), targetField)
                } else {
                    logger.warn("[GoSDK] Field '" + at.allTo() + "' not found in class " + inventoryClazz.simpleName + " or its parents")
                }
            } else if (at.fieldsTo().size() != 0 && at.fieldsTo()[0] != "all") {
                fieldMap = new LinkedHashMap<String, Field>()
                for (String fieldsTo : at.fieldsTo()) {
                    def split = fieldsTo.split("=")
                    String fieldName = split.size() == 1 ? split[0] : split[1]
                    String outputName = split[0]
                    Field targetField = findFieldInHierarchy(inventoryClazz, fieldName)
                    if (targetField != null) {
                        fieldMap.put(outputName, targetField)
                    } else {
                        logger.warn("[GoSDK] Field '" + fieldName + "' not found in class " + inventoryClazz.simpleName + " or its parents")
                    }
                }
            }
        }

        def builder = new StringBuilder()
        builder.append("// ${structName} ${getStructDescription(inventoryClazz)}\n")
        builder.append("type ${structName} struct {\n")

        // InventoryView embeds BaseInfoView and BaseTimeView
        if (structName.endsWith('InventoryView')) {
            builder.append("\tBaseInfoView\n")
            builder.append("\tBaseTimeView\n")
        }

        boolean hasFields = false
        fieldMap.each { String fieldName, Field field ->
            if (field.isAnnotationPresent(APINoSee.class)) {
                return
            }
            if ('error' == fieldName && structName.endsWith('View')) {
                return
            }

            // Skip fields already covered by BaseInfoView and BaseTimeView
            if (structName.endsWith('InventoryView')) {
                if (fieldName in ['uuid', 'name', 'createDate', 'lastOpDate']) {
                    return
                }
            }

            String goFieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1)
            String baseFieldType = generateFieldGeneric(field)

            // View structs avoid pointer fields; omitempty handles optional zero values
            String fieldType = baseFieldType
            String jsonTag = generateJsonTag(fieldName, field, apiParamMap)

            builder.append("\t${goFieldName} ${fieldType} ${jsonTag}\n")
            hasFields = true
        }

        // For empty Event/Reply classes (e.g., success-only responses), add a Success field
        if (!hasFields && structName.endsWith('View') && !structName.endsWith('InventoryView')) {
            builder.append("\t// Empty response - operation succeeded\n")
        }

        builder.append("}\n\n")

        // Restore previous class context
        currentGeneratingClass = previousClass

        String result = builder.toString()
        logger.warn("[GoSDK] generateViewStruct result for ${structName}: length=${result.length()}")
        return result
    }

    /**
     * Generate param struct (called by GoApiTemplate)
     * Nested types are collected but generated separately in base_param_types.go
     */
    String generateParamStruct(Class<?> apiMsgClazz, String paramStructName, String detailParamStructName, RestRequest restRequest) {
        if (apiMsgClazz == null) {
            return ""
        }

        // Enable param mode - will use different type naming and collect nested types
        generatingForParam = true

        def apiParamMap = new HashMap<String, APIParam>()
        if (apiMsgClazz.isAnnotationPresent(OverriddenApiParams.class)) {
            for (OverriddenApiParam oap : apiMsgClazz.getAnnotation(OverriddenApiParams.class).value()) {
                apiParamMap.put(oap.field(), oap.param())
            }
        }

        def fieldMap = new LinkedHashMap<String, Field>()
        FieldUtils.getAllFields(apiMsgClazz).each { Field f ->
            if (!Modifier.isStatic(f.modifiers) && !f.isAnnotationPresent(APINoSee.class)) {
                fieldMap.put(f.name, f)
            }
        }

        def builder = new StringBuilder()

        // Generate detail param struct
        builder.append("// ${detailParamStructName} ${getStructDescription(apiMsgClazz)} detail param\n")
        builder.append("type ${detailParamStructName} struct {\n")

        // Extract path parameters from @RestRequest to skip them
        // Path parameters come from URL, not request body
        def pathParams = []
        if (restRequest != null && restRequest.path() != null) {
            def matcher = (restRequest.path() =~ /\{([^}]+)\}/)
            while (matcher.find()) {
                pathParams.add(matcher.group(1))
            }
        }

        // Skip fields that should be in BaseParam or QueryParam
        // Also skip path parameters (they come from URL, not request body)
        def skipFields = ['systemTags', 'userTags', 'requestIp', 'session', 'timeout', 'id', 'serviceId', 'creatingAccountUuid'] + pathParams

        fieldMap.each { String fieldName, Field field ->
            if (field.isAnnotationPresent(APINoSee.class)) {
                return
            }
            if (skipFields.contains(fieldName)) {
                return
            }

            String goFieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1)
            boolean isOptional = isOptionalField(field, apiParamMap)
            String fieldType = generateParamFieldGeneric(field, apiParamMap, isOptional)
            String jsonTag = generateJsonTag(fieldName, field, apiParamMap)

            builder.append("\t${goFieldName} ${fieldType} ${jsonTag}\n")
        }

        builder.append("}\n\n")

        // Generate wrapper param struct
        // Prefer @RestRequest.parameterName; if missing or "null", derive it from the class name
        String jsonKey
        if (restRequest != null && restRequest.parameterName() != null &&
                !restRequest.parameterName().isEmpty() && !restRequest.parameterName().equals("null")) {
            jsonKey = restRequest.parameterName()
        } else {
            // Derive action name from API class, e.g., APIUpdateImageMsg -> updateImage
            String apiClassName = apiMsgClazz.simpleName
            String actionName = apiClassName.replaceAll('^API', '').replaceAll('Msg$', '')
            jsonKey = actionName.substring(0, 1).toLowerCase() + actionName.substring(1)
        }
        // Use a consistent field name Params for convenience
        String fieldName = "Params"

        builder.append("// ${paramStructName} ${getStructDescription(apiMsgClazz)} request param\n")
        builder.append("type ${paramStructName} struct {\n")
        builder.append("\tBaseParam\n")
        builder.append("\t${fieldName} ${detailParamStructName} `json:\"${jsonKey}\"`\n")
        builder.append("}\n")

        generatingForParam = false

        return builder.toString()
    }

    /**
     * Generate field type for param (uses different naming - no View suffix)
     */
    private String generateParamFieldGeneric(Field field, Map<String, APIParam> apiParamMap, boolean isOptional) {
        if (!(field.getGenericType() instanceof ParameterizedType)) {
            return generateParamFieldType(field, null, apiParamMap, isOptional)
        }
        String typeName = ""
        if (Collection.class.isAssignableFrom(field.type)) {
            Type type = ((ParameterizedType) field.getGenericType()).actualTypeArguments[0]
            // Slices and maps stay non-pointer because they are reference types
            typeName = "[]" + generateParamFieldType(null, type, apiParamMap, false)
        }
        if (Map.class.isAssignableFrom(field.type)) {
            Type value = ((ParameterizedType) field.getGenericType()).actualTypeArguments[1]
            // Slices and maps stay non-pointer because they are reference types
            typeName = "map[string]" + generateParamFieldType(null, value, apiParamMap, false)
        }
        return typeName
    }

    /**
     * Generate field type for param - complex types go to param package
     */
    private String generateParamFieldType(Field field, Type type, Map<String, APIParam> apiParamMap, boolean isOptional) {
        String baseType = null

        if (field != null) {
            Class fieldType = field.type
            String goType = mapJavaTypeToGoType(fieldType)
            if (goType != null) {
                baseType = goType
            } else if (isGeneratableClass(fieldType)) {
                // For complex types in param, add to paramNestedTypes for generation in param package
                paramNestedTypes.add(fieldType)
                logger.debug("[GoSDK] Added param nested type: " + fieldType.simpleName)
                baseType = getParamStructName(fieldType)
            } else if (!fieldType.isPrimitive() && !fieldType.isEnum()) {
                baseType = "interface{}"
            }
        }

        if (type != null && baseType == null) {
            if (type instanceof Class) {
                Class clz = (Class) type
                String goType = mapJavaTypeToGoType(clz)
                if (goType != null) {
                    baseType = goType
                } else if (isGeneratableClass(clz)) {
                    // For complex types in param, add to paramNestedTypes
                    paramNestedTypes.add(clz)
                    logger.debug("[GoSDK] Added param nested type: " + clz.simpleName)
                    baseType = getParamStructName(clz)
                } else {
                    baseType = "interface{}"
                }
            } else if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type
                Class clz = (Class) pt.rawType
                if (isGeneratableClass(clz)) {
                    paramNestedTypes.add(clz)
                    baseType = getParamStructName(clz)
                } else {
                    baseType = "interface{}"
                }
            } else {
                baseType = "interface{}"
            }
        }

        if (baseType == null) {
            baseType = "interface{}"
        }

        // Optional basic types (string, int, int64, bool, etc.) use pointers; interface{}, slices, and maps stay as non-pointers because they already carry reference/nil semantics
        def basicTypes = ["string", "int", "int64", "int32", "float64", "float32", "bool"] as Set
        if (isOptional && !baseType.startsWith("[") && !baseType.startsWith("map[") &&
                !baseType.equals("interface{}") && basicTypes.contains(baseType)) {
            return "*" + baseType
        }

        return baseType
    }

    /**
     * Get param struct name (without View suffix)
     */
    private String getParamStructName(Class clz) {
        String name = clz.simpleName
        if (clz.enclosingClass != null) {
            String outerName = clz.enclosingClass.simpleName.replaceAll('^API', '').replaceAll('Msg$', '').replaceAll('Action$', '')
            // For nested inner classes, include parent for uniqueness
            name = outerName + "_" + name
            logger.debug("[GoSDK] Identified inner class: ${clz.name} -> ${name}Param")
        }
        if (name.endsWith("Inventory")) {
            return name.replace("Inventory", "") + "Param"
        }
        return name + "Param"
    }

    /**
     * Generate param nested types content
     */
    private String generateParamNestedTypes() {
        if (paramNestedTypes.isEmpty()) {
            return ""
        }

        def builder = new StringBuilder()

        // Use a queue-like approach to handle newly discovered types during generation
        def typesToProcess = new ArrayList<Class>(paramNestedTypes)
        int index = 0

        // Enable param mode for nested type generation
        generatingForParam = true

        while (index < typesToProcess.size()) {
            Class clz = typesToProcess.get(index)
            String structName = getParamStructName(clz)
            if (!generatedParamStructs.contains(structName)) {
                generatedParamStructs.add(structName)
                builder.append(generateParamNestedStruct(clz, structName))

                // Check if new types were added during generation
                paramNestedTypes.each { Class newClz ->
                    if (!typesToProcess.contains(newClz)) {
                        typesToProcess.add(newClz)
                    }
                }
            }
            index++
        }

        generatingForParam = false

        return builder.toString()
    }

    /**
     * Load base_param_types.go from template file and append dynamically generated param types
     */
    private SdkFile loadBaseParamTypesTemplate() {
        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/param/"
        sdkFile.fileName = "base_param_types.go"

        def content = new StringBuilder()

        try {
            // Load template with fixed base param types
            def templateStream = this.class.getResourceAsStream("/scripts/templates/base_param_types.go.template")
            if (templateStream != null) {
                content.append(templateStream.text)
                logger.warn("[GoSDK] Loaded base_param_types.go from template file")
            } else {
                logger.error("[GoSDK] base_param_types.go.template not found in classpath")
                throw new RuntimeException("base_param_types.go.template not found")
            }

            // Append dynamically generated param nested types
            if (!paramNestedTypes.isEmpty()) {
                logger.warn("[GoSDK] Appending ${paramNestedTypes.size()} dynamically generated param types")
                content.append("\n// ========== Dynamically Generated Param Types ==========\n\n")
                String nestedTypes = generateParamNestedTypes()
                content.append(nestedTypes)
            } else {
                logger.warn("[GoSDK] No dynamic param nested types to append")
            }

            sdkFile.content = content.toString()
        } catch (Exception e) {
            logger.error("[GoSDK] Failed to load base_param_types.go template: ${e.message}")
            throw e
        }

        return sdkFile
    }

    /**
     * Load login_params.go from template file
     */
    private SdkFile loadLoginParamsTemplate() {
        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/param/"
        sdkFile.fileName = "login_params.go"

        try {
            def templateStream = this.class.getResourceAsStream("/scripts/templates/login_params.go.template")
            if (templateStream != null) {
                sdkFile.content = templateStream.text
                logger.warn("[GoSDK] Loaded login_params.go from template file")

                // Mark Login-related params as generated to avoid duplicates
                generatedParamStructs.add("LoginByAccountParam")
                generatedParamStructs.add("LoginByAccountDetailParam")
                generatedParamStructs.add("LogInByUserParam")
                generatedParamStructs.add("LogInByUserDetailParam")
                generatedParamStructs.add("LoginIAM2VirtualIDWithLdapParam")
                generatedParamStructs.add("LoginIAM2VirtualIDWithLdapDetailParam")
                generatedParamStructs.add("LoginIAM2PlatformParam")
                generatedParamStructs.add("LoginIAM2PlatformDetailParam")
                generatedParamStructs.add("ValidateSessionParam")
            } else {
                logger.error("[GoSDK] login_params.go.template not found in classpath")
                throw new RuntimeException("login_params.go.template not found")
            }
        } catch (Exception e) {
            logger.error("[GoSDK] Failed to load login_params.go template: ${e.message}")
            throw e
        }

        return sdkFile
    }

    /**
     * Load session_additional_views.go from template file
     */
    private SdkFile loadSessionViewsTemplate() {
        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/view/"
        sdkFile.fileName = "session_additional_views.go"

        try {
            def templateStream = this.class.getResourceAsStream("/scripts/templates/session_additional_views.go.template")
            if (templateStream != null) {
                sdkFile.content = templateStream.text
                logger.warn("[GoSDK] Loaded session_additional_views.go from template file")

                // Mark Session-related views as generated to avoid duplicates
                generatedViewStructs.add("SessionInventoryView")
                generatedViewStructs.add("WebUISessionView")
            } else {
                logger.error("[GoSDK] session_additional_views.go.template not found in classpath")
                throw new RuntimeException("session_additional_views.go.template not found")
            }
        } catch (Exception e) {
            logger.error("[GoSDK] Failed to load session_additional_views.go template: ${e.message}")
            throw e
        }

        return sdkFile
    }

    /**
     * Generate a nested struct for param package
     */
    private String generateParamNestedStruct(Class<?> clazz, String structName) {
        if (clazz == null) {
            return ""
        }

        // Build APIParam map for this class
        def apiParamMap = new HashMap<String, APIParam>()
        if (clazz.isAnnotationPresent(OverriddenApiParams.class)) {
            for (OverriddenApiParam oap : clazz.getAnnotation(OverriddenApiParams.class).value()) {
                apiParamMap.put(oap.field(), oap.param())
            }
        }

        def fieldMap = new LinkedHashMap<String, Field>()
        FieldUtils.getAllFields(clazz).each { Field f ->
            if (!Modifier.isStatic(f.modifiers)) {
                fieldMap.put(f.name, f)
            }
        }

        def builder = new StringBuilder()
        builder.append("// ${structName} ${clazz.simpleName} param struct\n")
        builder.append("type ${structName} struct {\n")

        fieldMap.each { String fieldName, Field field ->
            if (field.isAnnotationPresent(APINoSee.class)) {
                return
            }

            String goFieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1)
            // Nested types are typically optional (used in request params)
            boolean isOptional = isOptionalField(field, apiParamMap)
            String fieldType = generateParamFieldGeneric(field, apiParamMap, isOptional)
            String jsonTag = "`json:\"${fieldName},omitempty\"`"

            builder.append("\t${goFieldName} ${fieldType} ${jsonTag}\n")
        }

        builder.append("}\n\n")
        return builder.toString()
    }

    /**
     * Check if there are param nested types to generate
     */
    boolean hasParamNestedTypes() {
        return !paramNestedTypes.isEmpty()
    }

    /**
     * For backward compatibility
     */
    String generateStruct(Class<?> clazz, String structName) {
        return generateViewStruct(clazz, structName)
    }

    private String getStructDescription(Class clz) {
        String name = clz.simpleName
        return name.replaceAll("API", "")
                .replaceAll('Msg$', "")
                .replaceAll('Inventory$', "")
                .replaceAll('Reply$', "")
    }

    private String generateJsonTag(String fieldName, Field field, Map<String, APIParam> apiParamMap) {
        def tags = new StringBuilder()
        tags.append('`json:"')
        tags.append(fieldName)

        boolean required = false
        if (field.isAnnotationPresent(APIParam.class)) {
            APIParam param = apiParamMap.containsKey(fieldName) ?
                    apiParamMap.get(fieldName) : field.getAnnotation(APIParam.class)
            required = param.required()
        }

        if (!required) {
            tags.append(',omitempty')
        }

        tags.append('"')

        if (required) {
            tags.append(' validate:"required"')
        }

        tags.append('`')
        return tags.toString()
    }

    /**
     * Determine whether a field is optional (and should use a pointer type)
     */
    private boolean isOptionalField(Field field, Map<String, APIParam> apiParamMap) {
        if (field == null) return false

        // 0. uuid and name always have values; keep them non-pointer
        if (field.name in ["uuid", "name"]) {
            return false
        }

        // 1. Check APIParam.required flag
        if (field.isAnnotationPresent(APIParam.class)) {
            APIParam param = apiParamMap.containsKey(field.name) ?
                    apiParamMap.get(field.name) : field.getAnnotation(APIParam.class)
            if (!param.required()) {
                return true
            }
        }

        // 2. Certain fields are optional by default
        if (field.name in ["description", "lastOpDate", "expiredDate"]) {
            return true
        }

        // 3. Non-primitive and non-String types default to optional unless required
        Class fieldType = field.type
        if (fieldType in [String.class, Integer.class, Long.class, Short.class, Byte.class,
                          Float.class, Double.class, Boolean.class, Date.class, java.sql.Timestamp.class]) {
            // If no APIParam annotation is present, treat as optional
            if (!field.isAnnotationPresent(APIParam.class)) {
                return true
            }
        }

        return false
    }

    private String generateFieldGeneric(Field field) {
        if (!(field.getGenericType() instanceof ParameterizedType)) {
            return generateFieldType(field, null)
        }
        String typeName = ""
        if (Collection.class.isAssignableFrom(field.type)) {
            Type type = ((ParameterizedType) field.getGenericType()).actualTypeArguments[0]
            typeName = "[]" + generateFieldType(null, type)
        }
        if (Map.class.isAssignableFrom(field.type)) {
            Type value = ((ParameterizedType) field.getGenericType()).actualTypeArguments[1]
            typeName = "map[string]" + generateFieldType(null, value)
        }
        return typeName
    }

    private String generateFieldType(Field field, Type type) {
        if (field != null) {
            Class fieldType = field.type
            // Special-case top-level inventory fields that are maps or collections to keep Go types aligned with client expectations
            if ("inventory" == field.name || "inventories" == field.name) {
                if (Map.class.isAssignableFrom(fieldType)) {
                    // Prefer MapView wrapper for arbitrary maps
                    return "MapView"
                }
                if (Collection.class.isAssignableFrom(fieldType)) {
                    // Try to resolve element type; otherwise fall back to ListView
                    if (field.getGenericType() instanceof ParameterizedType) {
                        ParameterizedType pt = (ParameterizedType) field.getGenericType()
                        Type[] args = pt.getActualTypeArguments()
                        if (args != null && args.length > 0 && args[0] instanceof Class) {
                            Class elemClz = (Class) args[0]
                            String goType = mapJavaTypeToGoType(elemClz, true)
                            if (goType != null) {
                                return "[]" + goType
                            }
                            if (elemClz.isAnnotationPresent(Inventory.class)) {
                                inventories.add(elemClz)
                                return "[]" + getViewStructName(elemClz)
                            }
                            if (isGeneratableClass(elemClz)) {
                                additionalClasses.add(elemClz)
                                return "[]" + getViewStructName(elemClz)
                            }
                        }
                    }
                    return "ListView"
                }
            }

            String goType = mapJavaTypeToGoType(fieldType, true)
            if (goType != null) {
                return goType
            }

            // Check for self-reference (recursive type) - use pointer to break cycle
            boolean isSelfReference = (currentGeneratingClass != null && fieldType == currentGeneratingClass)
            String pointerPrefix = isSelfReference ? "*" : ""

            if (isSelfReference) {
                logger.debug("[GoSDK] Detected self-reference in " + currentGeneratingClass.simpleName + " -> " + fieldType.simpleName)
            }

            // Check if the class has @Inventory annotation
            if (fieldType.isAnnotationPresent(Inventory.class)) {
                inventories.add(fieldType)
                return pointerPrefix + getViewStructName(fieldType)
            }

            // For non-Inventory complex classes, add to additional classes for generation
            if (isGeneratableClass(fieldType)) {
                additionalClasses.add(fieldType)
                logger.debug("[GoSDK] Added additional class for generation: " + fieldType.simpleName)
                return pointerPrefix + getViewStructName(fieldType)
            }

            // For Java built-in types or interfaces, use interface{}
            if (!fieldType.isPrimitive() && !fieldType.isEnum()) {
                return "interface{}"
            }
        }

        if (type != null) {
            if (type instanceof Class) {
                Class clz = (Class) type
                String goType = mapJavaTypeToGoType(clz, true)
                if (goType != null) {
                    return goType
                }

                // Check for self-reference (recursive type) - use pointer to break cycle
                boolean isSelfReference = (currentGeneratingClass != null && clz == currentGeneratingClass)
                String pointerPrefix = isSelfReference ? "*" : ""

                // Check if the class has @Inventory annotation
                if (clz.isAnnotationPresent(Inventory.class)) {
                    inventories.add(clz)
                    return pointerPrefix + getViewStructName(clz)
                }

                // For non-Inventory complex classes, add to additional classes for generation
                if (isGeneratableClass(clz)) {
                    additionalClasses.add(clz)
                    logger.debug("[GoSDK] Added additional class for generation: " + clz.simpleName)
                    return pointerPrefix + getViewStructName(clz)
                }

                // For Java built-in types or interfaces, use interface{}
                return "interface{}"
            }

            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type
                Class clz = (Class) pt.rawType

                // Check for self-reference
                boolean isSelfReference = (currentGeneratingClass != null && clz == currentGeneratingClass)
                String pointerPrefix = isSelfReference ? "*" : ""

                if (clz.isAnnotationPresent(Inventory.class)) {
                    inventories.add(clz)
                    return pointerPrefix + getViewStructName(clz)
                }

                // For non-Inventory complex classes, add to additional classes
                if (isGeneratableClass(clz)) {
                    additionalClasses.add(clz)
                    return pointerPrefix + getViewStructName(clz)
                }
            }
        }

        return "interface{}"
    }

    /**
     * Check if a class is a generatable complex class (not primitive, not array, not Java built-in)
     */
    private boolean isGeneratableClass(Class clz) {
        if (clz == null) return false
        if (clz.isPrimitive()) return false
        if (clz.isEnum()) return false
        if (clz.isInterface()) return false
        if (clz.isArray()) return false  // Exclude array types like byte[]
        if (clz.name.startsWith("java.")) return false
        if (clz.name.startsWith("javax.")) return false
        if (clz.name.startsWith("[")) return false  // Array internal representation
        return true
    }

    private String mapJavaTypeToGoType(Class javaType) {
        return mapJavaTypeToGoType(javaType, false)
    }

    private String mapJavaTypeToGoType(Class javaType, boolean forView) {
        if (javaType == null) return null

        // Handle array types
        if (javaType.isArray()) {
            Class componentType = javaType.getComponentType()
            if (componentType == byte.class || componentType == Byte.class) {
                // Java byte is signed (-128 to 127), use []int8 instead of []byte (which is []uint8)
                return "[]int8"
            }
            String elementType = mapJavaTypeToGoType(componentType, forView)
            if (elementType != null) {
                return "[]" + elementType
            }
            return "[]interface{}"
        }

        switch (javaType) {
            case String.class:
            case Character.class:
            case char.class:
                return "string"
            case Integer.class:
            case int.class:
                return "int"
            case Long.class:
            case long.class:
                return "int64"
            case Short.class:
            case short.class:
                return "int16"
            case Byte.class:
            case byte.class:
                return "int8"
            case Float.class:
            case float.class:
                return "float32"
            case Double.class:
            case double.class:
                return "float64"
            case Boolean.class:
            case boolean.class:
                return "bool"
            case Date.class:
            case java.sql.Timestamp.class:
                // Normalize to time.Time (replace legacy ZStackTime)
                return "time.Time"
            default:
                if (javaType.isEnum()) {
                    return "string"
                }
                return null
        }
    }

    /**
     * Generate other_actions.go for miscellaneous APIs
     */
    private SdkFile generateOtherActionsFile() {
        logger.warn("[GoSDK] Generating other_actions.go...")

        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/client/"
        sdkFile.fileName = "other_actions.go"

        // Collect all method code first to check if fmt is needed
        StringBuilder methodsContent = new StringBuilder()

        allApiTemplates.each { GoApiTemplate template ->
            if (template.isActionGrouped) return

            // Skip methods that are manually maintained in client.go
            String apiName = template.getApiMsgClazz()?.simpleName ?: ""
            if (apiName.contains("ValidateSession") || apiName.contains("LogIn") || apiName.contains("Login")) {
                logger.debug("[GoSDK] Skipping manually maintained API in other_actions: " + apiName)
                template.isActionGrouped = true
                return
            }

            // Check for duplicate method names (including Get methods from Query APIs)
            Set<String> methodNames = template.getGeneratedMethodNames()
            boolean hasDuplicate = methodNames.any { generatedClientMethods.contains(it) }

            if (hasDuplicate) {
                logger.warn("[GoSDK] Skipping duplicate method in other_actions: ${template.clzName} (methods ${methodNames} already generated)")
                template.isActionGrouped = true
                return
            }

            String code = template.generateMethodCode()
            if (code != "" && code != null) {
                methodsContent.append(code).append("\n")
                methodNames.each { generatedClientMethods.add(it) }
                template.isActionGrouped = true
                GoApiTemplate.groupedApiNames.add(template.clzName)
                logger.debug("[GoSDK] Grouped orphaned action: " + template.clzName)
            }
        }

        // Check if fmt is needed (for fmt.Sprintf in multi-placeholder paths)
        boolean needsFmt = methodsContent.toString().contains("fmt.Sprintf")

        def content = new StringBuilder()
        content.append("// Copyright (c) ZStack.io, Inc.\n\n")
        content.append("package client\n\n")
        content.append("import (\n")
        if (needsFmt) {
            content.append("\t\"fmt\"\n")
        }
        content.append("\t\"github.com/zstackio/zstack-sdk-go-v2/pkg/param\"\n")
        content.append("\t\"github.com/zstackio/zstack-sdk-go-v2/pkg/view\"\n")
        content.append(")\n\n")
        content.append("var _ = param.BaseParam{} // avoid unused import\n")
        content.append("var _ = view.MapView{} // avoid unused import\n\n")

        // Append the collected methods
        content.append(methodsContent.toString())

        sdkFile.content = content.toString()
        return sdkFile
    }

    /**
     * Generate other_views.go for miscellaneous APIs (Catch-all)
     * This catches ALL response classes that haven't been generated yet, regardless of resource grouping
     */
    private SdkFile generateOtherViewsFile() {
        logger.warn("[GoSDK] Generating other_views.go...")
        logger.warn("[GoSDK] Total API templates to check: " + allApiTemplates.size())

        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/view/"
        sdkFile.fileName = "other_views.go"

        def content = new StringBuilder()
        content.append("// Copyright (c) ZStack.io, Inc.\n\n")
        content.append("package view\n\n")
        content.append("import \"time\"\n\n")
        content.append("var _ = time.Now // avoid unused import\n\n")

        int addedCount = 0
        Set<String> processedViews = new HashSet<>()

        // Generate response views for ALL APIs, catching anything missed
        allApiTemplates.each { GoApiTemplate template ->
            Class<?> responseClass = template.getResponseClass()
            if (responseClass == null) {
                return
            }

            String viewStructName = getViewStructName(responseClass)

            // Skip if already processed in this pass
            if (processedViews.contains(viewStructName)) {
                return
            }
            processedViews.add(viewStructName)

            // Skip if already generated in resource views
            if (generatedViewStructs.contains(viewStructName)) {
                logger.debug("[GoSDK] View ${viewStructName} already generated (from ${template.clzName})")
                return
            }

            // Generate the response view
            String structCode = template.generateResponseViewCode()
            if (structCode != null && structCode != "") {
                generatedViewStructs.add(viewStructName)
                content.append(structCode)
                addedCount++
                logger.warn("[GoSDK] Added view to other_views.go: " + viewStructName + " (from ${template.clzName})")
            } else {
                logger.warn("[GoSDK] Failed to generate view: " + viewStructName + " (from ${template.clzName})")
            }
        }

        logger.warn("[GoSDK] Finished other_views.go, added ${addedCount} views")

        sdkFile.content = content.toString()
        return sdkFile
    }

    /**
     * Generate other_params.go for miscellaneous APIs
     */
    private SdkFile generateOtherParamsFile() {
        logger.warn("[GoSDK] Generating other_params.go...")

        def sdkFile = new SdkFile()
        sdkFile.subPath = "/pkg/param/"
        sdkFile.fileName = "other_params.go"

        def content = new StringBuilder()
        content.append("// Copyright (c) ZStack.io, Inc.\n\n")
        content.append("package param\n\n")

        Set<Class<?>> apiClasses = Platform.reflections.getTypesAnnotatedWith(RestRequest.class)
        logger.warn("[GoSDK] Total APIs found for other_params: " + apiClasses.size())

        int addedCount = 0
        int skippedCount = 0
        allApiTemplates.each { GoApiTemplate template ->
            // Skip if already grouped in a resource-specific param file
            if (template.isParamGrouped) {
                skippedCount++
                return
            }

            // Skip Login-related APIs (handled by login_params.go template)
            String apiName = template.getApiMsgClazz()?.simpleName ?: ""
            if (apiName.contains("LogIn") || apiName.contains("Login") || apiName.contains("ValidateSession")) {
                logger.debug("[GoSDK] Skipping Login/Session API in other_params: " + apiName)
                template.isParamGrouped = true
                skippedCount++
                return
            }

            // If it's not a query message AND it hasn't been generated individually
            if (!template.isQueryMessage()) {
                String paramStructName = template.getParamStructName()
                String detailParamName = template.getDetailParamStructName()

                // Double-check: skip if already in generatedParamStructs (from grouped files)
                if (!generatedParamStructs.contains(paramStructName)) {
                    String structCode = generateParamStruct(template.getApiMsgClazz(), paramStructName, detailParamName, template.getAt())
                    if (structCode != "") {
                        generatedParamStructs.add(paramStructName)
                        generatedParamStructs.add(detailParamName)
                        content.append(structCode)
                        addedCount++
                        logger.warn("[GoSDK] Added orphaned param to other_params.go: " + paramStructName + " (from ${template.getApiMsgClazz().simpleName})")
                    }
                    template.isParamGrouped = true
                } else {
                    skippedCount++
                    logger.debug("[GoSDK] Skipping already generated param: " + paramStructName)
                    template.isParamGrouped = true
                }
            }
        }
        logger.warn("[GoSDK] Finished other_params.go, added ${addedCount} params, skipped ${skippedCount}")

        sdkFile.content = content.toString()
        return sdkFile
    }

    /**
     * Revised generateViewFiles to include response Events/Replies
     */
    private List<SdkFile> generateViewFiles() {
        def files = []
        def fileMap = [:]  // Map fileName -> SdkFile for merging multiple inventories

        // Deduplicate inventories first - use LinkedHashSet to maintain order and remove duplicates
        def uniqueInventories = new LinkedHashSet<Class>(inventories)
        logger.warn("[GoSDK] Starting generateViewFiles: ${inventories.size()} inventories (${uniqueInventories.size()} unique)")

        // Use a list and index to handle dynamically added inventories
        def activeInventories = new ArrayList<Class>(uniqueInventories)
        int index = 0

        while (index < activeInventories.size()) {
            Class<?> inventoryClass = activeInventories.get(index)
            String prefix = inventoryClass.simpleName.replaceAll('Inventory$', '')
            String structName = getViewStructName(inventoryClass)
            String fileName = "${toSnakeCase(prefix)}_views.go"

            logger.warn("[GoSDK] Processing inventory [${index + 1}/${activeInventories.size()}]: ${inventoryClass.simpleName} -> ${prefix}")

            // Check if file already exists in map - if so, merge; if not, create new
            def sdkFile
            def content
            boolean isNewFile = false

            if (fileMap.containsKey(fileName)) {
                // File already exists, merge into it
                sdkFile = fileMap[fileName]
                content = new StringBuilder(sdkFile.content)
                logger.warn("[GoSDK] Merging ${structName} into existing ${fileName}")
            } else {
                // Create new file
                isNewFile = true
                sdkFile = new SdkFile()
                sdkFile.subPath = "/pkg/view/"
                sdkFile.fileName = fileName

                content = new StringBuilder()
                content.append("// Copyright (c) ZStack.io, Inc.\n\n")
                content.append("package view\n\n")
                content.append("import \"time\"\n\n")
                content.append("var _ = time.Now // avoid unused import\n\n")
                logger.warn("[GoSDK] Generating new view file for: ${structName} (${fileName})")
            }

            // Add Inventory Struct (if not already generated) - do this for BOTH new and merged files
            if (!generatedViewStructs.contains(structName)) {
                generatedViewStructs.add(structName)
                content.append(generateViewStruct(inventoryClass, structName))
                logger.warn("[GoSDK] Added inventory struct: ${structName}")
            } else {
                logger.warn("[GoSDK] Inventory struct ${structName} already exists, skipping duplicate")
            }

            // Collect and group response views for this resource using the cache
            allApiTemplates.each { GoApiTemplate template ->
                if (template.isViewGrouped) return

                String resName = template.getResourceName()
                // queryInventoryClass is populated for ALL APIs that return an inventory (not just Query APIs)
                Class<?> returnedInventory = template.getQueryInventoryClass()

                // Match by resourceName OR by returned inventory type
                // Use exact match to avoid CdpPolicy matching Policy
                boolean matchesByName = resName != null && resName == prefix
                boolean matchesByInventory = returnedInventory != null && returnedInventory == inventoryClass

                if (matchesByName || matchesByInventory) {
                    if (matchesByInventory && !matchesByName) {
                        logger.warn("[GoSDK] Matched API ${template.clzName} to ${prefix} by returned inventory type (${returnedInventory.simpleName})")
                    }

                    logger.warn("[GoSDK] Resource match: API ${template.clzName} (Resource: ${resName}, ActionType: '${template.getActionType()}') matches Inventory prefix: ${prefix}")
                    // Only standard actions (non-empty actionType) go into resource view files
                    if (template.getActionType() != "") {
                        Class<?> responseClass = template.getResponseClass()
                        logger.warn("[GoSDK] Checking responseClass for ${template.clzName}: ${responseClass?.simpleName ?: 'null'}")
                        if (responseClass != null) {
                            String viewName = getViewStructName(responseClass)
                            logger.warn("[GoSDK] ViewName for ${template.clzName}: ${viewName}, already generated: ${generatedViewStructs.contains(viewName)}")
                            if (!generatedViewStructs.contains(viewName)) {
                                logger.warn("[GoSDK] Calling generateResponseViewCode for ${template.clzName}...")
                                String structCode = template.generateResponseViewCode()
                                logger.warn("[GoSDK] generateResponseViewCode returned ${structCode.length()} chars for ${viewName}")
                                if (structCode != "") {
                                    generatedViewStructs.add(viewName)
                                    content.append(structCode)
                                    template.isViewGrouped = true
                                    logger.warn("[GoSDK] Grouped ${viewName} into ${fileName} (from ${template.clzName}), content now ${content.length()} chars")
                                } else {
                                    logger.warn("[GoSDK] Skipping ${viewName} - no view code generated (will be caught by other_views.go if needed)")
                                }
                            } else {
                                logger.warn("[GoSDK] ${viewName} already generated, marking template as grouped")
                                template.isViewGrouped = true
                            }
                        }
                    } else {
                        logger.warn("[GoSDK] Skipping ${template.clzName} - empty actionType")
                    }
                }
            }

            // Update file content and store in map
            sdkFile.content = content.toString()
            fileMap[fileName] = sdkFile
            logger.warn("[GoSDK] Updated ${fileName}: content.length=${content.length()}, generatedViewStructs.size=${generatedViewStructs.size()}")

            // If this is a new file, add to output list
            if (isNewFile) {
                files.add(sdkFile)
                logger.warn("[GoSDK] Added new file ${fileName} to output (${files.size()} total files)")
            } else {
                logger.warn("[GoSDK] Merged ${inventoryClass.simpleName} into existing ${fileName}")
            }

            // Re-sync activeInventories if more were discovered - but avoid duplicates
            inventories.each { Class<?> newClz ->
                if (!activeInventories.contains(newClz)) {
                    activeInventories.add(newClz)
                    logger.warn("[GoSDK] Discovered new inventory during processing: ${newClz.simpleName}")
                }
            }
            index++
        }

        logger.warn("[GoSDK] Finished generateViewFiles: generated ${files.size()} view files")
        return files
    }

    /**
     * Generate action files grouped by resource
     */
    private List<SdkFile> generateActionFiles() {
        def files = []
        def activeInventories = new ArrayList<Class>(inventories)

        activeInventories.each { Class<?> inventoryClass ->
            String prefix = inventoryClass.simpleName.replaceAll('Inventory$', '')
            String fileName = "${toSnakeCase(prefix)}_actions.go"

            // Collect all method code first to check if fmt is needed
            StringBuilder methodsContent = new StringBuilder()
            boolean hasActions = false

            allApiTemplates.each { GoApiTemplate template ->
                if (template.isActionGrouped) return

                String resName = template.getResourceName()
                // Use exact match to avoid CdpPolicy matching Policy
                if (resName != null && resName == prefix) {
                    // Check for duplicate method names (including Get methods from Query APIs)
                    Set<String> methodNames = template.getGeneratedMethodNames()
                    boolean hasDuplicate = methodNames.any { generatedClientMethods.contains(it) }

                    if (hasDuplicate) {
                        logger.warn("[GoSDK] Skipping duplicate method: ${template.clzName} (methods ${methodNames} already generated)")
                        template.isActionGrouped = true
                        return
                    }

                    methodsContent.append(template.generateMethodCode())
                    methodNames.each { generatedClientMethods.add(it) }
                    template.isActionGrouped = true
                    GoApiTemplate.groupedApiNames.add(template.clzName)
                    hasActions = true
                }
            }

            if (hasActions) {
                // Check if fmt is needed (for fmt.Sprintf in multi-placeholder paths)
                boolean needsFmt = methodsContent.toString().contains("fmt.Sprintf")

                def content = new StringBuilder()
                content.append("// Copyright (c) ZStack.io, Inc.\n\n")
                content.append("package client\n\n")
                content.append("import (\n")
                if (needsFmt) {
                    content.append("\t\"fmt\"\n")
                }
                content.append("\t\"github.com/zstackio/zstack-sdk-go-v2/pkg/param\"\n")
                content.append("\t\"github.com/zstackio/zstack-sdk-go-v2/pkg/view\"\n")
                content.append(")\n\n")
                content.append("var _ = param.BaseParam{} // avoid unused import\n")
                content.append("var _ = view.MapView{} // avoid unused import\n\n")

                // Append the collected methods
                content.append(methodsContent.toString())

                def sdkFile = new SdkFile()
                sdkFile.subPath = "/pkg/client/"
                sdkFile.fileName = fileName
                sdkFile.content = content.toString()
                files.add(sdkFile)
                logger.warn("[GoSDK] Generated grouped action file: " + fileName)
            }
        }
        return files
    }

    /**
     * Generate param files grouped by resource
     */
    private List<SdkFile> generateParamFiles() {
        def files = []
        def activeInventories = new ArrayList<Class>(inventories)

        activeInventories.each { Class<?> inventoryClass ->
            String prefix = inventoryClass.simpleName.replaceAll('Inventory$', '')
            String fileName = "${toSnakeCase(prefix)}_params.go"

            def content = new StringBuilder()
            content.append("// Copyright (c) ZStack.io, Inc.\n\n")
            content.append("package param\n\n")
            content.append("import \"time\"\n\n")
            content.append("var _ = time.Now // avoid unused import\n\n")

            boolean hasParams = false
            allApiTemplates.each { GoApiTemplate template ->
                if (template.isParamGrouped) return

                // Standard actions generate their params here
                // We use isGrouped logic differently for params, or just check the resource
                String resName = template.getResourceName()
                // Use exact match to avoid CdpPolicy matching Policy
                if (resName == prefix) {
                    if (!template.isQueryMessage()) {
                        String paramStructName = template.getParamStructName()
                        String detailParamName = template.getDetailParamStructName()

                        if (!generatedParamStructs.contains(paramStructName)) {
                            content.append(generateParamStruct(template.getApiMsgClazz(), paramStructName, detailParamName, template.getAt()))
                            generatedParamStructs.add(paramStructName)
                            generatedParamStructs.add(detailParamName)
                            hasParams = true
                            template.isParamGrouped = true
                        }
                    }
                }
            }

            if (hasParams) {
                def sdkFile = new SdkFile()
                sdkFile.subPath = "/pkg/param/"
                sdkFile.fileName = fileName
                sdkFile.content = content.toString()
                files.add(sdkFile)
                logger.warn("[GoSDK] Generated grouped param file: " + fileName)
            }
        }
        return files
    }
}
