/*
 * Copyright 2017-2023 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.openapi.generator;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.samskivert.mustache.Mustache;
import io.micronaut.openapi.generator.Formatting.ReplaceDotsWithUnderscoreLambda;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.atteo.evo.inflector.English;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenDiscriminator;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenModelFactory;
import org.openapitools.codegen.CodegenModelType;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.DefaultCodegen;
import org.openapitools.codegen.IJsonSchemaValidationProperties;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.VendorExtension;
import org.openapitools.codegen.config.GlobalSettings;
import org.openapitools.codegen.languages.AbstractKotlinCodegen;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.meta.features.ClientModificationFeature;
import org.openapitools.codegen.meta.features.DocumentationFeature;
import org.openapitools.codegen.meta.features.GlobalFeature;
import org.openapitools.codegen.meta.features.SchemaSupportFeature;
import org.openapitools.codegen.meta.features.SecurityFeature;
import org.openapitools.codegen.meta.features.WireFormatFeature;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.micronaut.openapi.generator.MnSchemaTypeUtil.FORMAT_INT16;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.FORMAT_INT8;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.FORMAT_SHORT;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_BYTE;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_CHAR;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_CHARACTER;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_DOUBLE;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_FLOAT;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_INT;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_LONG;
import static io.micronaut.openapi.generator.MnSchemaTypeUtil.TYPE_SHORT;
import static io.micronaut.openapi.generator.Utils.DEFAULT_BODY_PARAM_NAME;
import static io.micronaut.openapi.generator.Utils.DIVIDE_OPERATIONS_BY_CONTENT_TYPE;
import static io.micronaut.openapi.generator.Utils.EXT_ANNOTATIONS_CLASS;
import static io.micronaut.openapi.generator.Utils.EXT_ANNOTATIONS_FIELD;
import static io.micronaut.openapi.generator.Utils.EXT_ANNOTATIONS_OPERATION;
import static io.micronaut.openapi.generator.Utils.EXT_ANNOTATIONS_SETTER;
import static io.micronaut.openapi.generator.Utils.addStrValueToEnum;
import static io.micronaut.openapi.generator.Utils.isDateType;
import static io.micronaut.openapi.generator.Utils.normalizeExtraAnnotations;
import static io.micronaut.openapi.generator.Utils.processGenericAnnotations;
import static io.micronaut.openapi.generator.Utils.readListOfStringsProperty;
import static io.swagger.v3.parser.util.SchemaTypeUtil.BYTE_FORMAT;
import static io.swagger.v3.parser.util.SchemaTypeUtil.INTEGER_TYPE;
import static org.openapitools.codegen.CodegenConstants.API_PACKAGE;
import static org.openapitools.codegen.CodegenConstants.INVOKER_PACKAGE;
import static org.openapitools.codegen.CodegenConstants.MODEL_PACKAGE;
import static org.openapitools.codegen.CodegenConstants.PACKAGE_NAME;
import static org.openapitools.codegen.languages.KotlinClientCodegen.DATE_LIBRARY;
import static org.openapitools.codegen.utils.OnceLogger.once;
import static org.openapitools.codegen.utils.StringUtils.camelize;
import static org.openapitools.codegen.utils.StringUtils.underscore;

/**
 * Base generator for Micronaut.
 *
 * @param <T> The generator options builder.
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public abstract class AbstractMicronautKotlinCodegen<T extends GeneratorOptionsBuilder> extends AbstractKotlinCodegen implements BeanValidationFeatures, MicronautCodeGenerator<T> {

    public static final String OPT_TITLE = "title";
    public static final String OPT_TEST = "test";
    public static final String OPT_TEST_JUNIT = "junit";
    public static final String OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR = "requiredPropertiesInConstructor";
    public static final String OPT_USE_AUTH = "useAuth";
    public static final String OPT_USE_PLURAL = "plural";
    public static final String OPT_FLUX_FOR_ARRAYS = "fluxForArrays";
    public static final String OPT_GENERATED_ANNOTATION = "generatedAnnotation";
    public static final String OPT_VISITABLE = "visitable";
    public static final String OPT_DATE_LIBRARY_ZONED_DATETIME = "ZONED_DATETIME";
    public static final String OPT_DATE_LIBRARY_OFFSET_DATETIME = "OFFSET_DATETIME";
    public static final String OPT_DATE_LIBRARY_LOCAL_DATETIME = "LOCAL_DATETIME";
    public static final String OPT_DATE_FORMAT = "dateFormat";
    public static final String OPT_DATE_TIME_FORMAT = "dateTimeFormat";
    public static final String OPT_REACTIVE = "reactive";
    public static final String OPT_GENERATE_HTTP_RESPONSE_ALWAYS = "generateHttpResponseAlways";
    public static final String OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED = "generateHttpResponseWhereRequired";
    public static final String OPT_APPLICATION_NAME = "applicationName";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS = "generateSwaggerAnnotations";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2 = "swagger2";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS_TRUE = "true";
    public static final String OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE = "false";
    public static final String OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG = "generateOperationOnlyForFirstTag";
    public static final String OPT_IMPLICIT_HEADERS = "implicitHeaders";
    public static final String OPT_IMPLICIT_HEADERS_REGEX = "implicitHeadersRegex";
    public static final String OPT_USE_ENUM_CASE_INSENSITIVE = "useEnumCaseInsensitive";
    public static final String OPT_KSP = "ksp";
    public static final String ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS = "additionalOneOfTypeAnnotations";
    public static final String ADDITIONAL_ENUM_TYPE_ANNOTATIONS = "additionalEnumTypeAnnotations";
    public static final String CONTENT_TYPE_APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    public static final String CONTENT_TYPE_MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String CONTENT_TYPE_ANY = "*/*";

    private static final String MONO_CLASS_NAME = "reactor.core.publisher.Mono";
    private static final String FLUX_CLASS_NAME = "reactor.core.publisher.Flux";

    protected SecureRandom random = new SecureRandom();
    protected String dateLibrary;
    protected String title;
    protected boolean useBeanValidation;
    protected boolean visitable;
    protected boolean plural = true;
    protected boolean fluxForArrays;
    protected boolean generatedAnnotation = true;
    protected String testTool;
    protected boolean requiredPropertiesInConstructor = true;
    protected boolean reactive;
    protected boolean generateHttpResponseAlways;
    protected boolean generateHttpResponseWhereRequired = true;
    protected boolean useEnumCaseInsensitive;
    protected boolean ksp;
    protected boolean implicitHeaders;
    protected String implicitHeadersRegex;
    protected String appName;
    protected String dateFormat;
    protected String dateTimeFormat;
    protected String generateSwaggerAnnotations;
    protected boolean generateOperationOnlyForFirstTag;
    protected String serializationLibrary = SerializationLibraryKind.MICRONAUT_SERDE_JACKSON.name();
    protected List<ParameterMapping> parameterMappings = new ArrayList<>();
    protected List<ResponseBodyMapping> responseBodyMappings = new ArrayList<>();
    protected Map<String, CodegenModel> allModels = new HashMap<>();
    protected List<String> additionalOneOfTypeAnnotations = new LinkedList<>();
    protected List<String> additionalEnumTypeAnnotations = new LinkedList<>();

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<String, String> schemaKeyToModelNameCache = new HashMap<>();

    protected AbstractMicronautKotlinCodegen() {

        languageSpecificPrimitives = Set.of(
            "Byte",
            "ByteArray",
            "Short",
            "Int",
            "Long",
            "Float",
            "Double",
            "Boolean",
            "Char",
            "String",
            "Array",
            "List",
            "MutableList",
            "Map",
            "MutableMap",
            "Set",
            "MutableSet",
            "Any"
        );

        defaultIncludes = Set.of(
            "Byte",
            "ByteArray",
            "Short",
            "Int",
            "Long",
            "Float",
            "Double",
            "Boolean",
            "Char",
            "Array",
            "List",
            "MutableList",
            "Set",
            "MutableSet",
            "Map",
            "MutableMap",
            "Any"
        );

        // CHECKSTYLE:OFF
        // Set all the fields
        useJakartaEe = true;
        useBeanValidation = true;
        visitable = false;
        hideGenerationTimestamp = false;
        testTool = OPT_TEST_JUNIT;
        outputFolder = this instanceof KotlinMicronautClientCodegen ?
            "generated-code/kotlin-micronaut-client" : "generated-code/kotlin-micronaut";
        apiPackage = "org.openapitools.api";
        modelPackage = "org.openapitools.model";
        packageName = "org.openapitools";
        artifactId = this instanceof KotlinMicronautClientCodegen ?
            "openapi-micronaut-client" : "openapi-micronaut";
        embeddedTemplateDir = templateDir = "templates/kotlin-micronaut";
        apiDocPath = "docs/apis";
        modelDocPath = "docs/models";
        dateLibrary = OPT_DATE_LIBRARY_ZONED_DATETIME;
        reactive = true;
        appName = artifactId;
        generateSwaggerAnnotations = this instanceof KotlinMicronautClientCodegen ? OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE : OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2;
        generateOperationOnlyForFirstTag = this instanceof KotlinMicronautServerCodegen;
        enumPropertyNaming = CodegenConstants.ENUM_PROPERTY_NAMING_TYPE.UPPERCASE;
        inlineSchemaOption.put("RESOLVE_INLINE_ENUMS", "true");
        useOneOfInterfaces = true;
        // CHECKSTYLE:ON

        GlobalSettings.setProperty(DIVIDE_OPERATIONS_BY_CONTENT_TYPE, "true");

        // Set implemented features for user information
        modifyFeatureSet(features -> features
            .wireFormatFeatures(EnumSet.of(WireFormatFeature.JSON, WireFormatFeature.XML))
            .includeDocumentationFeatures(
                DocumentationFeature.Readme
            )
            .securityFeatures(EnumSet.of(
                SecurityFeature.ApiKey,
                SecurityFeature.BearerToken,
                SecurityFeature.BasicAuth,
                SecurityFeature.OAuth2_Implicit,
                SecurityFeature.OAuth2_AuthorizationCode,
                SecurityFeature.OAuth2_ClientCredentials,
                SecurityFeature.OAuth2_Password,
                SecurityFeature.OpenIDConnect,
                SecurityFeature.SignatureAuth,
                SecurityFeature.AWSV4Signature
            ))
            .excludeGlobalFeatures(GlobalFeature.XMLStructureDefinitions, GlobalFeature.Callbacks, GlobalFeature.LinkObjects, GlobalFeature.ParameterStyling)
            .excludeSchemaSupportFeatures(SchemaSupportFeature.Polymorphism)
            .includeClientModificationFeatures(ClientModificationFeature.BasePath)
        );

        // Set additional properties
        additionalProperties.put("useOneOfInterfaces", useOneOfInterfaces);
        additionalProperties.put("openbrace", "{");
        additionalProperties.put("closebrace", "}");

        // Set client options that will be presented to user
        updateOption(INVOKER_PACKAGE, packageName);
        updateOption(PACKAGE_NAME, packageName);
        updateOption(CodegenConstants.ARTIFACT_ID, artifactId);
        updateOption(CodegenConstants.API_PACKAGE, apiPackage);
        updateOption(CodegenConstants.MODEL_PACKAGE, modelPackage);

        cliOptions.add(new CliOption(OPT_TITLE, "Client service name").defaultValue(title));
        cliOptions.add(new CliOption(OPT_APPLICATION_NAME, "Micronaut application name (Defaults to the " + CodegenConstants.ARTIFACT_ID + " value)").defaultValue(appName));
        cliOptions.add(CliOption.newString(ADDITIONAL_ENUM_TYPE_ANNOTATIONS, "Additional annotations for enum type (class level annotations)"));
        cliOptions.add(CliOption.newString(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS, "Additional annotations for oneOf interfaces (class level annotations). List separated by semicolon(;) or new line (Linux or Windows)"));
        cliOptions.add(CliOption.newBoolean(OPT_USE_PLURAL, "Whether or not to use plural for request body parameter name", plural));
        cliOptions.add(CliOption.newBoolean(OPT_FLUX_FOR_ARRAYS, "Whether or not to use Flux<?> instead Mono<List<?>> for arrays in generated code", fluxForArrays));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATED_ANNOTATION, "Generate code with \"@Generated\" annotation", generatedAnnotation));
        cliOptions.add(CliOption.newBoolean(OPT_KSP, "Generate code compatible only with KSP", ksp));
        cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations", useBeanValidation));
        cliOptions.add(CliOption.newBoolean(OPT_VISITABLE, "Generate visitor for subtypes with a discriminator", visitable));
        cliOptions.add(CliOption.newBoolean(OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "Allow only to create models with all the required properties provided in constructor", requiredPropertiesInConstructor));
        cliOptions.add(CliOption.newBoolean(OPT_REACTIVE, "Make the responses use Reactor Mono as wrapper", reactive));
        cliOptions.add(CliOption.newBoolean(OPT_IMPLICIT_HEADERS, "Skip header parameters in the generated API methods using @ApiImplicitParams annotation.", implicitHeaders));
        cliOptions.add(CliOption.newString(OPT_IMPLICIT_HEADERS_REGEX, "Skip header parameters that matches given regex in the generated API methods using @ApiImplicitParams annotation. Note: this parameter is ignored when implicitHeaders=true"));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "Always wrap the operations response in HttpResponse object", generateHttpResponseAlways));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED, "Wrap the operations response in HttpResponse object where non-200 HTTP status codes or additional headers are defined", generateHttpResponseWhereRequired));
        cliOptions.add(CliOption.newBoolean(CodegenConstants.HIDE_GENERATION_TIMESTAMP, CodegenConstants.HIDE_GENERATION_TIMESTAMP_DESC, isHideGenerationTimestamp()));
        cliOptions.add(CliOption.newBoolean(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG, "When false, the operation method will be duplicated in each of the tags if multiple tags are assigned to this operation. " +
            "If true, each operation will be generated only once in the first assigned tag.", generateOperationOnlyForFirstTag));
        cliOptions.add(CliOption.newBoolean(OPT_USE_ENUM_CASE_INSENSITIVE, "Use `equalsIgnoreCase` when String for enum comparison", useEnumCaseInsensitive));

        var testToolOption = new CliOption(OPT_TEST, "Specify which test tool to generate files for").defaultValue(testTool);
        var testToolOptionMap = new HashMap<String, String>();
        testToolOptionMap.put(OPT_TEST_JUNIT, "Use JUnit as test tool");
        testToolOption.setEnum(testToolOptionMap);
        cliOptions.add(testToolOption);

        var generateSwaggerAnnotationsOption = new CliOption(OPT_GENERATE_SWAGGER_ANNOTATIONS, "Specify if you want to generate swagger annotations and which version").defaultValue(generateSwaggerAnnotations);
        var generateSwaggerAnnotationsOptionMap = new HashMap<String, String>();
        generateSwaggerAnnotationsOptionMap.put(OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2, "Use io.swagger.core.v3:swagger-annotations for annotating operations and schemas");
        generateSwaggerAnnotationsOptionMap.put(OPT_GENERATE_SWAGGER_ANNOTATIONS_TRUE, "Equivalent to \"" + OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2 + "\"");
        generateSwaggerAnnotationsOptionMap.put(OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE, "Do not generate swagger annotations");
        generateSwaggerAnnotationsOption.setEnum(generateSwaggerAnnotationsOptionMap);
        cliOptions.add(generateSwaggerAnnotationsOption);

        cliOptions.add(new CliOption(OPT_DATE_FORMAT, "Specify the format pattern of date as a string"));
        cliOptions.add(new CliOption(OPT_DATE_TIME_FORMAT, "Specify the format pattern of date-time as a string"));

        // Modify the DATE_LIBRARY option to only have supported values
        cliOptions.stream()
            .filter(o -> o.getOpt().equals(DATE_LIBRARY))
            .findFirst()
            .ifPresent(opt -> {
                var valuesEnum = new HashMap<String, String>();
                valuesEnum.put(OPT_DATE_LIBRARY_OFFSET_DATETIME, opt.getEnum().get(OPT_DATE_LIBRARY_OFFSET_DATETIME));
                valuesEnum.put(OPT_DATE_LIBRARY_LOCAL_DATETIME, opt.getEnum().get(OPT_DATE_LIBRARY_LOCAL_DATETIME));
                opt.setEnum(valuesEnum);
            });

        final CliOption serializationLibraryOpt = CliOption.newString(CodegenConstants.SERIALIZATION_LIBRARY, "Serialization library for model");
        serializationLibraryOpt.defaultValue(SerializationLibraryKind.JACKSON.name());
        var serializationLibraryOptions = new HashMap<String, String>();
        serializationLibraryOptions.put(SerializationLibraryKind.JACKSON.name(), "Jackson as serialization library");
        serializationLibraryOptions.put(SerializationLibraryKind.MICRONAUT_SERDE_JACKSON.name(), "Use micronaut-serialization with Jackson annotations");
        serializationLibraryOpt.setEnum(serializationLibraryOptions);
        cliOptions.removeIf(opt -> opt.getOpt().equals(CodegenConstants.SERIALIZATION_LIBRARY));
        cliOptions.add(serializationLibraryOpt);

        // Add reserved words
        reservedWords.addAll(List.of(
            "Client",
            "Format",
            "QueryValue",
            "QueryParam",
            "PathVariable",
            "Header",
            "Cookie",
            "Authorization",
            "Body",
            "application"
        ));
//        reservedWords.remove("value");

        typeMapping.put("string", "String");
        typeMapping.put("boolean", "Boolean");
        typeMapping.put("integer", "Int");
        typeMapping.put("float", "Float");
        typeMapping.put("long", "Long");
        typeMapping.put("double", "Double");
        typeMapping.put("ByteArray", "ByteArray");
        typeMapping.put("number", "BigDecimal");
        typeMapping.put("decimal", "BigDecimal");
        typeMapping.put("file", "File");
        typeMapping.put("array", "List");
        typeMapping.put("list", "List");
        typeMapping.put("set", "Set");
        typeMapping.put("map", "Map");
        typeMapping.put("object", "Any");
        typeMapping.put("binary", "ByteArray");
        typeMapping.put("AnyType", "Any");
        typeMapping.put("DateTime", "Instant");
        typeMapping.put("date-time", "OffsetDateTime");
        typeMapping.put("date", "LocalDate");
        typeMapping.put("Date", "LocalDate");
        typeMapping.put("LocalDateTime", "LocalDateTime");
        typeMapping.put("OffsetDateTime", "OffsetDateTime");
        typeMapping.put("ZonedDateTime", "ZonedDateTime");
        typeMapping.put("LocalDate", "LocalDate");
        typeMapping.put("LocalTime", "LocalTime");

        instantiationTypes.put("array", "ArrayList");
        instantiationTypes.put("list", "ArrayList");
        instantiationTypes.put("map", "HashMap");

        importMapping.put("File", "java.io.File");
        importMapping.put("BigDecimal", "java.math.BigDecimal");
        importMapping.put("DateTime", "java.time.Instant");
        importMapping.put("LocalDateTime", "java.time.LocalDateTime");
        importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
        importMapping.put("ZonedDateTime", "java.time.ZonedDateTime");
        importMapping.put("LocalDate", "java.time.LocalDate");
        importMapping.put("LocalTime", "java.time.LocalTime");
    }

    public void setGenerateHttpResponseAlways(boolean generateHttpResponseAlways) {
        this.generateHttpResponseAlways = generateHttpResponseAlways;
    }

    public void setGenerateHttpResponseWhereRequired(boolean generateHttpResponseWhereRequired) {
        this.generateHttpResponseWhereRequired = generateHttpResponseWhereRequired;
    }

    public void setReactive(boolean reactive) {
        this.reactive = reactive;
    }

    public void setImplicitHeaders(boolean implicitHeaders) {
        this.implicitHeaders = implicitHeaders;
    }

    public void setImplicitHeadersRegex(String implicitHeadersRegex) {
        this.implicitHeadersRegex = implicitHeadersRegex;
    }

    public void setTestTool(String testTool) {
        this.testTool = testTool;
    }

    @Override
    public void setArtifactId(String artifactId) {
        super.setArtifactId(artifactId);
        updateOption(CodegenConstants.ARTIFACT_ID, artifactId);
    }

    @Override
    public void setModelPackage(String modelPackage) {
        super.setModelPackage(modelPackage);
        updateOption(CodegenConstants.MODEL_PACKAGE, modelPackage);
    }

    @Override
    public void setApiPackage(String apiPackage) {
        super.setApiPackage(apiPackage);
        updateOption(CodegenConstants.API_PACKAGE, apiPackage);
    }

    @Override
    public void setPackageName(String packageName) {
        super.setPackageName(packageName);
        updateOption(INVOKER_PACKAGE, packageName);
        updateOption(PACKAGE_NAME, packageName);
    }

    public void setPlural(boolean plural) {
        this.plural = plural;
    }

    public void setFluxForArrays(boolean fluxForArrays) {
        this.fluxForArrays = fluxForArrays;
    }

    public void setGeneratedAnnotation(boolean generatedAnnotation) {
        this.generatedAnnotation = generatedAnnotation;
    }

    public void setKsp(boolean ksp) {
        this.ksp = ksp;
    }

    @Override
    public void processOpts() {

        // need it to add ability to set List<String> in `additionalModelTypeAnnotations` property
        if (additionalProperties.containsKey(ADDITIONAL_MODEL_TYPE_ANNOTATIONS)) {
            setAdditionalModelTypeAnnotations(readListOfStringsProperty(ADDITIONAL_MODEL_TYPE_ANNOTATIONS, additionalProperties));
            additionalProperties.remove(ADDITIONAL_MODEL_TYPE_ANNOTATIONS);
        }
        if (additionalProperties.containsKey(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS)) {
            setAdditionalOneOfTypeAnnotations(readListOfStringsProperty(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS, additionalProperties));
            additionalProperties.remove(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS);
        }
        if (additionalProperties.containsKey(ADDITIONAL_ENUM_TYPE_ANNOTATIONS)) {
            setAdditionalEnumTypeAnnotations(readListOfStringsProperty(ADDITIONAL_ENUM_TYPE_ANNOTATIONS, additionalProperties));
            additionalProperties.remove(ADDITIONAL_ENUM_TYPE_ANNOTATIONS);
        }

        super.processOpts();

        // Get properties
        if (additionalProperties.containsKey(OPT_TITLE)) {
            title = (String) additionalProperties.get(OPT_TITLE);
        }

        if (additionalProperties.containsKey(INVOKER_PACKAGE)) {
            packageName = (String) additionalProperties.get(INVOKER_PACKAGE);
        } else {
            additionalProperties.put(INVOKER_PACKAGE, packageName);
        }

        if (additionalProperties.containsKey(API_PACKAGE)) {
            apiPackage = (String) additionalProperties.get(API_PACKAGE);
        } else {
            additionalProperties.put(API_PACKAGE, apiPackage);
        }

        if (additionalProperties.containsKey(MODEL_PACKAGE)) {
            modelPackage = (String) additionalProperties.get(MODEL_PACKAGE);
        } else {
            additionalProperties.put(MODEL_PACKAGE, modelPackage);
        }

        if (additionalProperties.containsKey(OPT_APPLICATION_NAME)) {
            appName = (String) additionalProperties.get(OPT_APPLICATION_NAME);
        } else {
            additionalProperties.put(OPT_APPLICATION_NAME, artifactId);
        }

        // Get boolean properties
        if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
            useBeanValidation = convertPropertyToBoolean(USE_BEANVALIDATION);
        }
        writePropertyBack(USE_BEANVALIDATION, useBeanValidation);

        if (additionalProperties.containsKey(OPT_USE_PLURAL)) {
            plural = convertPropertyToBoolean(OPT_USE_PLURAL);
        }
        writePropertyBack(OPT_USE_PLURAL, plural);

        if (additionalProperties.containsKey(OPT_FLUX_FOR_ARRAYS)) {
            fluxForArrays = convertPropertyToBoolean(OPT_FLUX_FOR_ARRAYS);
        }
        writePropertyBack(OPT_FLUX_FOR_ARRAYS, fluxForArrays);

        if (additionalProperties.containsKey(OPT_USE_ENUM_CASE_INSENSITIVE)) {
            useEnumCaseInsensitive = convertPropertyToBoolean(OPT_USE_ENUM_CASE_INSENSITIVE);
        }
        writePropertyBack(OPT_USE_ENUM_CASE_INSENSITIVE, useEnumCaseInsensitive);

        if (additionalProperties.containsKey(OPT_GENERATED_ANNOTATION)) {
            generatedAnnotation = convertPropertyToBoolean(OPT_GENERATED_ANNOTATION);
        }
        writePropertyBack(OPT_GENERATED_ANNOTATION, generatedAnnotation);

        if (additionalProperties.containsKey(OPT_KSP)) {
            ksp = convertPropertyToBoolean(OPT_KSP);
        }
        writePropertyBack(OPT_KSP, ksp);

        if (additionalProperties.containsKey(OPT_VISITABLE)) {
            visitable = convertPropertyToBoolean(OPT_VISITABLE);
        }
        writePropertyBack(OPT_VISITABLE, visitable);

        if (additionalProperties.containsKey(OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR)) {
            requiredPropertiesInConstructor = convertPropertyToBoolean(OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR);
        }
        writePropertyBack(OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, requiredPropertiesInConstructor);

        if (additionalProperties.containsKey(OPT_REACTIVE)) {
            reactive = convertPropertyToBoolean(OPT_REACTIVE);
        }
        writePropertyBack(OPT_REACTIVE, reactive);

        if (additionalProperties.containsKey(OPT_DATE_FORMAT)) {
            dateFormat = (String) additionalProperties.get(OPT_DATE_FORMAT);
        }
        writePropertyBack(OPT_DATE_FORMAT, dateFormat);
        if (additionalProperties.containsKey(OPT_DATE_TIME_FORMAT)) {
            dateTimeFormat = (String) additionalProperties.get(OPT_DATE_TIME_FORMAT);
        }
        writePropertyBack(OPT_DATE_TIME_FORMAT, dateTimeFormat);

        if (additionalProperties.containsKey(OPT_GENERATE_HTTP_RESPONSE_ALWAYS)) {
            generateHttpResponseAlways = convertPropertyToBoolean(OPT_GENERATE_HTTP_RESPONSE_ALWAYS);
        }
        writePropertyBack(OPT_GENERATE_HTTP_RESPONSE_ALWAYS, generateHttpResponseAlways);
        if (additionalProperties.containsKey(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED)) {
            generateHttpResponseWhereRequired = convertPropertyToBoolean(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED);
        }
        writePropertyBack(OPT_GENERATE_HTTP_RESPONSE_WHERE_REQUIRED, generateHttpResponseWhereRequired);

        if (additionalProperties.containsKey(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG)) {
            generateOperationOnlyForFirstTag = convertPropertyToBoolean(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG);
        }
        writePropertyBack(OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG, generateOperationOnlyForFirstTag);

        if (additionalProperties.containsKey(USE_JAKARTA_EE)) {
            setUseJakartaEe(convertPropertyToBoolean(USE_JAKARTA_EE));
        }
        writePropertyBack(USE_JAKARTA_EE, useJakartaEe);
        writePropertyBack(JAVAX_PACKAGE, useJakartaEe ? "jakarta" : "javax");

        maybeSetTestTool();
        writePropertyBack(OPT_TEST, testTool);
        if (testTool.equals(OPT_TEST_JUNIT)) {
            additionalProperties.put("isTestJunit", true);
        }

        convertPropertyToBooleanAndWriteBack(OPT_IMPLICIT_HEADERS, this::setImplicitHeaders);
        convertPropertyToStringAndWriteBack(OPT_IMPLICIT_HEADERS_REGEX, this::setImplicitHeadersRegex);

        maybeSetSwagger();
        if (OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2.equals(generateSwaggerAnnotations)) {
            additionalProperties.put("generateSwagger2Annotations", true);
        }

        if (additionalProperties.containsKey(CodegenConstants.SERIALIZATION_LIBRARY)) {
            setSerializationLibrary((String) additionalProperties.get(CodegenConstants.SERIALIZATION_LIBRARY));
        }
        additionalProperties.put(serializationLibrary.toLowerCase(Locale.US), true);
        if (SerializationLibraryKind.MICRONAUT_SERDE_JACKSON.name().equals(serializationLibrary)) {
            additionalProperties.put(SerializationLibraryKind.JACKSON.name().toLowerCase(Locale.US), true);
        }

        // Add all the supporting files
        supportingFiles.add(new SupportingFile("common/configuration/application.yml.mustache", resourcesFolder, "application.yml").doNotOverwrite());
        supportingFiles.add(new SupportingFile("common/configuration/logback.xml.mustache", resourcesFolder, "logback.xml").doNotOverwrite());

        // Use the default java time
        switch (dateLibrary) {
            case OPT_DATE_LIBRARY_OFFSET_DATETIME -> {
                typeMapping.put("DateTime", "OffsetDateTime");
                typeMapping.put("date", "LocalDate");
            }
            case OPT_DATE_LIBRARY_ZONED_DATETIME -> {
                typeMapping.put("DateTime", "ZonedDateTime");
                typeMapping.put("date", "LocalDate");
            }
            case OPT_DATE_LIBRARY_LOCAL_DATETIME -> {
                typeMapping.put("DateTime", "LocalDateTime");
                typeMapping.put("date", "LocalDate");
            }
            default -> {
            }
        }

        // Add documentation files
        modelDocTemplateFiles.clear();
        modelDocTemplateFiles.put("common/doc/model_doc.mustache", ".md");

        // Add model files
        modelTemplateFiles.clear();
        modelTemplateFiles.put("common/model/model.mustache", ".kt");

        // Add test files
        modelTestTemplateFiles.clear();
        if (testTool.equals(OPT_TEST_JUNIT)) {
            modelTestTemplateFiles.put("common/test/model_test.mustache", ".kt");
        }

        // Set properties for documentation
        final String invokerFolder = (sourceFolder + '/' + packageName).replace(".", "/");
        final String apiFolder = (sourceFolder + '/' + apiPackage()).replace('.', '/');
        final String modelFolder = (sourceFolder + '/' + modelPackage()).replace('.', '/');
        additionalProperties.put("invokerFolder", invokerFolder);
        additionalProperties.put("resourceFolder", resourcesFolder);
        additionalProperties.put("apiFolder", apiFolder);
        additionalProperties.put("modelFolder", modelFolder);

        additionalProperties.put("formatNoEmptyLines", new Formatting.LineFormatter(0));
        additionalProperties.put("formatOneEmptyLine", new Formatting.LineFormatter(1));
        additionalProperties.put("formatSingleLine", new Formatting.SingleLineFormatter());
        additionalProperties.put("indent", new Formatting.IndentFormatter(4));
    }

    public void addParameterMappings(List<ParameterMapping> parameterMappings) {
        this.parameterMappings.addAll(parameterMappings);
    }

    public void addResponseBodyMappings(List<ResponseBodyMapping> responseBodyMappings) {
        this.responseBodyMappings.addAll(responseBodyMappings);
    }

    public void addSchemaMapping(Map<String, String> schemaMapping) {
        this.schemaMapping.putAll(schemaMapping);
    }

    public void addImportMapping(Map<String, String> importMapping) {
        this.importMapping.putAll(importMapping);
    }

    public void addNameMapping(Map<String, String> nameMapping) {
        this.nameMapping.putAll(nameMapping);
    }

    public void addTypeMapping(Map<String, String> typeMapping) {
        this.typeMapping.putAll(typeMapping);
    }

    public void addEnumNameMapping(Map<String, String> enumNameMapping) {
        this.enumNameMapping.putAll(enumNameMapping);
    }

    public void addModelNameMapping(Map<String, String> modelNameMapping) {
        this.modelNameMapping.putAll(modelNameMapping);
    }

    public void addInlineSchemaNameMapping(Map<String, String> inlineSchemaNameMapping) {
        this.inlineSchemaNameMapping.putAll(inlineSchemaNameMapping);
    }

    public void addInlineSchemaOption(Map<String, String> inlineSchemaOption) {
        this.inlineSchemaOption.putAll(inlineSchemaOption);
    }

    public void addOpenapiNormalizer(Map<String, String> openapiNormalizer) {
        this.openapiNormalizer.putAll(openapiNormalizer);
    }

    // CHECKSTYLE:OFF
    private void maybeSetSwagger() {
        if (additionalProperties.containsKey(OPT_GENERATE_SWAGGER_ANNOTATIONS)) {
            String value = String.valueOf(additionalProperties.get(OPT_GENERATE_SWAGGER_ANNOTATIONS));
            switch (value) {
                case OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2, OPT_GENERATE_SWAGGER_ANNOTATIONS_TRUE -> generateSwaggerAnnotations = OPT_GENERATE_SWAGGER_ANNOTATIONS_SWAGGER_2;
                case OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE -> generateSwaggerAnnotations = OPT_GENERATE_SWAGGER_ANNOTATIONS_FALSE;
                default -> throw new RuntimeException("Value \"" + value + "\" for the " + OPT_GENERATE_SWAGGER_ANNOTATIONS + " parameter is unsupported or misspelled");
            }
        }
    }

    private void maybeSetTestTool() {
        if (additionalProperties.containsKey(OPT_TEST)) {
            if (additionalProperties.get(OPT_TEST).equals(OPT_TEST_JUNIT)) {
                testTool = (String) additionalProperties.get(OPT_TEST);
            } else {
                throw new RuntimeException("Test tool \"" + additionalProperties.get(OPT_TEST) + "\" is not supported or misspelled.");
            }
        }
    }
    // CHECKSTYLE:ON

    public String testFileFolder() {
        return (getOutputDir() + "/src/test/kotlin/").replace('/', File.separatorChar);
    }

    public abstract boolean isServer();

    @Override
    public String apiTestFileFolder() {
        return testFileFolder() + apiPackage().replace(".", "/");
    }

    @Override
    public String modelTestFileFolder() {
        return getOutputDir() + "/src/test/kotlin/" + modelPackage().replace('.', File.separatorChar);
    }

    @Override
    public String toApiTestFilename(String name) {
        return toApiName(name) + "Test";
    }

    @Override
    public String toModelTestFilename(String name) {
        return toModelName(name) + "Test";
    }

    @Override
    public void setUseBeanValidation(boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }

    public void setVisitable(boolean visitable) {
        this.visitable = visitable;
    }

    @Override
    protected CodegenDiscriminator createDiscriminator(String schemaName, Schema schema) {

        var discriminator = super.createDiscriminator(schemaName, schema);
        if (discriminator == null) {
            return null;
        }

        for (var entry : discriminator.getMapping().entrySet()) {
            String name;
            if (entry.getValue().indexOf('/') < 0) {
                continue;
            }
            name = ModelUtils.getSimpleRef(entry.getValue());
            var referencedSchema = ModelUtils.getSchema(openAPI, name);
            if (referencedSchema == null) {
                once(log).error("Failed to lookup the schema '{}' when processing the discriminator mapping of oneOf/anyOf. Please check to ensure it's defined properly.", name);
                continue;
            }
            if (referencedSchema.getProperties() == null || referencedSchema.getProperties().isEmpty()) {
                continue;
            }
            boolean isDiscriminatorPropTypeFound = false;
            var props = (Map<String, Schema>) referencedSchema.getProperties();
            for (var propEntry : props.entrySet()) {
                if (!propEntry.getKey().equals(discriminator.getPropertyName())) {
                    continue;
                }
                discriminator.setPropertyType(getTypeDeclaration(propEntry.getValue()));
                isDiscriminatorPropTypeFound = true;
                break;
            }
            if (isDiscriminatorPropTypeFound) {
                break;
            }
        }
        return discriminator;
    }

    @Override
    public String toApiVarName(String name) {
        String apiVarName = super.toApiVarName(name);
        if (reservedWords.contains(apiVarName)) {
            apiVarName = escapeReservedWord(apiVarName);
        }
        return apiVarName;
    }

    public boolean isVisitable() {
        return visitable;
    }

    @Override
    public String sanitizeTag(String tag) {
        // Skip sanitization to get the original tag name in the addOperationToGroup() method.
        // Inside that method tag is manually sanitized.
        return tag;
    }

    public String superSanitizeTag(String tag) {
        tag = camelize(underscore(sanitizeName(tag)));

        // tag starts with numbers
        if (tag.matches("^\\d.*")) {
            tag = "Class" + tag;
        }
        return tag;
    }

    @Override
    public String toApiName(String name) {
        return Utils.toApiName(name, apiNamePrefix, apiNameSuffix);
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co,
                                    Map<String, List<CodegenOperation>> operations) {
        if (generateOperationOnlyForFirstTag && !co.tags.get(0).getName().equals(tag)) {
            // This is not the first assigned to this operation tag;
            return;
        }

        super.addOperationToGroup(superSanitizeTag(tag), resourcePath, operation, co, operations);
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openApi) {

        if (openApi.getPaths() != null) {
            for (var path : openApi.getPaths().values()) {
                if (path.getParameters() == null || path.getParameters().isEmpty()) {
                    continue;
                }

                for (var op : path.readOperations()) {
                    if (op.getParameters() == null) {
                        op.setParameters(new ArrayList<>());
                    }
                    for (var param : path.getParameters()) {
                        var found = false;
                        for (var opParam : op.getParameters()) {
                            if (Objects.equals(opParam.getName(), param.getName())
                                && Objects.equals(opParam.get$ref(), param.get$ref())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            op.getParameters().add(param);
                        }
                    }
                }
            }
        }

        var inlineModelResolver = new MicronautInlineModelResolver(openApi);
        inlineModelResolver.setInlineSchemaNameMapping(inlineSchemaNameMapping);
        inlineModelResolver.setInlineSchemaOptions(inlineSchemaOption);
        inlineModelResolver.flatten();

        var pathItems = openApi.getPaths();
        // fix for ApiResponse with $ref
        if (pathItems != null) {
            for (var pathEntry : pathItems.entrySet()) {
                for (var opEntry : pathEntry.getValue().readOperationsMap().entrySet()) {
                    // process all response bodies
                    if (opEntry.getValue().getResponses() != null) {
                        for (var responseEntry : opEntry.getValue().getResponses().entrySet()) {
                            var response = responseEntry.getValue();
                            var refResponse = ModelUtils.getReferencedApiResponse(openApi, response);
                            if (response.getContent() == null) {
                                response.setContent(refResponse.getContent());
                            }
                            if (response.getDescription() == null) {
                                response.setDescription(refResponse.getDescription());
                            }
                            if (response.getHeaders() == null || response.getHeaders().isEmpty()) {
                                response.setHeaders(refResponse.getHeaders());
                            }
                            if (response.getExtensions() == null || response.getExtensions().isEmpty()) {
                                response.setExtensions(refResponse.getExtensions());
                            }
                            if (response.getLinks() == null || response.getLinks().isEmpty()) {
                                response.setLinks(refResponse.getLinks());
                            }
                        }
                    }
                }
            }
        }

        super.preprocessOpenAPI(openApi);

        if (openApi.getPaths() != null) {
            for (Map.Entry<String, PathItem> openAPIGetPathsEntry : openApi.getPaths().entrySet()) {
                PathItem path = openAPIGetPathsEntry.getValue();
                var ops = path.readOperations();
                if (ops == null || ops.isEmpty()) {
                    continue;
                }
                for (Operation operation : ops) {
                    log.info("Processing operation {}", operation.getOperationId());
                    if (hasBodyParameter(operation) || hasFormParameter(operation)) {
                        var defaultContentType = hasFormParameter(operation) ? CONTENT_TYPE_APPLICATION_FORM_URLENCODED : CONTENT_TYPE_APPLICATION_JSON;
                        var consumes = new ArrayList<>(getConsumesInfo(openApi, operation));
                        String contentType = consumes.isEmpty() ? defaultContentType : consumes.get(0);
                        operation.addExtension(VendorExtension.X_CONTENT_TYPE.getName(), contentType);
                    }
                    String[] accepts = getAccepts(openApi, operation);
                    operation.addExtension(VendorExtension.X_ACCEPTS.getName(), accepts);
                }
            }
        }
    }

    private String[] getAccepts(OpenAPI openAPIArg, Operation operation) {
        final Set<String> producesInfo = getProducesInfo(openAPIArg, operation);
        if (producesInfo != null && !producesInfo.isEmpty()) {
            return producesInfo.toArray(new String[] {});
        }
        return new String[] { CONTENT_TYPE_APPLICATION_JSON }; // default media type
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);

        Map<String, CodegenModel> models = allModels.stream()
            .map(ModelMap::getModel)
            .collect(Collectors.toMap(v -> v.classname, v -> v));
        OperationMap operations = objs.getOperations();
        List<CodegenOperation> operationList = operations.getOperation();

        for (CodegenOperation op : operationList) {

            handleImplicitHeaders(op);
            handleConstantParams(op);

            // Set whether body is supported in request
            op.vendorExtensions.put("methodAllowsBody", op.httpMethod.equals("PUT")
                || op.httpMethod.equals("POST")
                || op.httpMethod.equals("PATCH")
                || op.httpMethod.equals("OPTIONS")
                || op.httpMethod.equals("DELETE")
            );
            if (StringUtils.isNotEmpty(op.notes)) {
                op.notes = op.notes.strip();
            }
            if (StringUtils.isNotEmpty(op.summary)) {
                op.summary = op.summary.strip();
            }

            normalizeExtraAnnotations(EXT_ANNOTATIONS_OPERATION, true, op.vendorExtensions);

            // Set response example
            if (op.returnType != null) {
                String example;
                if (models.containsKey(op.returnType)) {
                    CodegenModel m = models.get(op.returnType);
                    List<Object> allowableValues = null;
                    if (m.allowableValues != null && m.allowableValues.containsKey("values")) {
                        allowableValues = (List<Object>) m.allowableValues.get("values");
                    }
                    example = getExampleValue(m.defaultValue, null, m.classname, true,
                        allowableValues, null, null, m.requiredVars, false);
                } else {
                    example = getExampleValue(null, null, op.returnType, false, null,
                        op.returnBaseType, null, null, false);
                }
                op.vendorExtensions.put("example", example);
            }

            // Remove the "*/*" contentType from operations as it is ambiguous
            if (CONTENT_TYPE_ANY.equals(op.vendorExtensions.get(VendorExtension.X_CONTENT_TYPE.getName()))) {
                op.vendorExtensions.put(VendorExtension.X_CONTENT_TYPE.getName(), CONTENT_TYPE_APPLICATION_JSON);
            }
            op.consumes = op.consumes == null ? null : op.consumes.stream()
                .filter(contentType -> !CONTENT_TYPE_ANY.equals(contentType.get("mediaType")))
                .toList();
            op.produces = op.produces == null ? null : op.produces.stream()
                .filter(contentType -> !CONTENT_TYPE_ANY.equals(contentType.get("mediaType")))
                .toList();

            // is only default "application/json" media type
            if (op.consumes == null
                || op.consumes.isEmpty()
                || op.consumes.size() == 1 && CONTENT_TYPE_APPLICATION_JSON.equals(op.consumes.get(0).get("mediaType"))) {
                op.vendorExtensions.put("onlyDefaultConsumeOrEmpty", true);
            }
            // is only default "application/json" media type
            if (op.produces == null
                || op.produces.isEmpty()
                || op.produces.size() == 1 && CONTENT_TYPE_APPLICATION_JSON.equals(op.produces.get(0).get("mediaType"))) {
                op.vendorExtensions.put("onlyDefaultProduceOrEmpty", true);
            }

            // Force form parameters are only set if the content-type is according
            // formParams correspond to urlencoded type
            // bodyParams correspond to multipart body
            if (CONTENT_TYPE_APPLICATION_FORM_URLENCODED.equals(op.vendorExtensions.get(VendorExtension.X_CONTENT_TYPE.getName()))) {
                op.formParams.addAll(op.bodyParams);
                op.bodyParams.forEach(p -> {
                    p.isBodyParam = false;
                    p.isFormParam = true;
                });
                op.bodyParams.clear();
            } else if (CONTENT_TYPE_MULTIPART_FORM_DATA.equals(op.vendorExtensions.get(VendorExtension.X_CONTENT_TYPE.getName()))) {
                op.bodyParams.addAll(op.formParams);
                for (var param : op.allParams) {
                    if (param.isFormParam) {
                        param.isBodyParam = true;
                        param.isFormParam = false;
                        param.vendorExtensions.put("isPart", true);
                    }
                }
                op.formParams.forEach(p -> {
                    p.isBodyParam = true;
                    p.isFormParam = false;
                    p.vendorExtensions.put("isPart", true);
                });
                op.formParams.clear();
            }

            for (var param : op.allParams) {
                processGenericAnnotations(param, useBeanValidation, false, param.isNullable || !param.required,
                    param.required, false, true);
                param.vendorExtensions.put("isString", "string".equalsIgnoreCase(param.dataType));
                param.vendorExtensions.put("withoutExample", param.example == null || param.example.equals("null"));
                if (useBeanValidation && ((!param.isContainer && param.isModel)
                    || (param.getIsArray() && param.getComplexType() != null && models.containsKey(param.getComplexType())))) {
                    param.vendorExtensions.put("withValid", true);
                }
                // check pattern property for date types: if set, need use this pattern as `@Format` annotation value
                if (isDateType(param.dataType) && StringUtils.isNotEmpty(param.pattern)) {
                    param.vendorExtensions.put("formatPattern", param.pattern);
                    param.pattern = null;
                }
            }

            if (op.returnProperty != null) {
                processGenericAnnotations(op.returnProperty, useBeanValidation, false, false, false, false, false);
                op.returnType = op.returnProperty.vendorExtensions.get("typeWithEnumWithGenericAnnotations").toString();
            }
        }

        return objs;
    }

    /**
     * This method removes all implicit header parameters from the list of parameters.
     *
     * @param operation - operation to be processed
     */
    protected void handleImplicitHeaders(CodegenOperation operation) {
        if (operation.allParams.isEmpty()) {
            return;
        }
        final ArrayList<CodegenParameter> copy = new ArrayList<>(operation.allParams);
        operation.allParams.clear();

        for (CodegenParameter p : copy) {
            if (p.isHeaderParam && (implicitHeaders || shouldBeImplicitHeader(p))) {
                operation.implicitHeadersParams.add(p);
                operation.headerParams.removeIf(header -> header.baseName.equals(p.baseName));
                once(log).info("Update operation [{}]. Remove header [{}] because it's marked to be implicit", operation.operationId, p.baseName);
            } else {
                operation.allParams.add(p);
            }
        }
        operation.hasParams = !operation.allParams.isEmpty();
    }

    private boolean shouldBeImplicitHeader(CodegenParameter parameter) {
        return StringUtils.isNotBlank(implicitHeadersRegex) && parameter.baseName.matches(implicitHeadersRegex);
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if ("Integer".equals(datatype)
            || "Double".equals(datatype)
            || "Int".equals(datatype)) {
            return value;
        } else if ("Long".equals(datatype)) {
            // add l to number, e.g. 2048 => 2048l
            return value + "L";
        } else if ("Float".equals(datatype)) {
            // add f to number, e.g. 3.14 => 3.14f
            return value + "F";
        } else if ("BigDecimal".equals(datatype)) {
            // use BigDecimal String constructor
            return "BigDecimal(\"" + value + "\")";
        } else if ("URI".equals(datatype)) {
            return "URI.create(\"" + escapeText(value) + "\")";
        } else {
            return "\"" + escapeText(value) + "\"";
        }
    }

    private void checkPrimitives(IJsonSchemaValidationProperties obj, Schema schema) {

        if (obj == null) {
            return;
        }

        CodegenModel model = null;
        CodegenParameter param = null;
        CodegenProperty prop = null;
        CodegenResponse response = null;
        if (obj instanceof CodegenModel m) {
            model = m;
        } else if (obj instanceof CodegenProperty p) {
            prop = p;
        } else if (obj instanceof CodegenParameter p) {
            param = p;
        } else if (obj instanceof CodegenResponse r) {
            response = r;
        }
        var extMap = model != null ? model.vendorExtensions : param != null ? param.vendorExtensions : prop != null ? prop.vendorExtensions : response.vendorExtensions;
        var dataType = model != null ? model.dataType : param != null ? param.dataType : prop != null ? prop.dataType : response.dataType;
        var dataTypeWithEnum = param != null ? param.datatypeWithEnum : prop != null ? prop.datatypeWithEnum : null;

        if (schema == null) {
            extMap.put("baseType", dataType);
            return;
        }

        var isNumeric = model != null ? model.isNumeric : param != null ? param.isNumeric : prop != null ? prop.isNumeric : response.isNumeric;
        var isNumber = model != null ? model.isNumber : param != null ? param.isNumber : prop != null ? prop.isNumber : response.isNumber;
        var isShort = model != null ? model.isShort : param != null ? param.isShort : prop != null ? prop.isShort : response.isShort;
        var isInteger = model != null ? model.isInteger : param != null ? param.isInteger : prop != null ? prop.isInteger : response.isInteger;
        var isLong = model != null ? model.isLong : param != null ? param.isLong : prop != null ? prop.isLong : response.isLong;
        var isFloat = model != null ? model.isFloat : param != null ? param.isFloat : prop != null ? prop.isFloat : response.isFloat;
        var isDouble = model != null ? model.isDouble : param != null ? param.isDouble : prop != null ? prop.isDouble : response.isDouble;

        var extensions = schema.getExtensions();
        var format = extensions != null ? extensions.get("x-format") : null;
        if (format == null) {
            format = schema.getFormat() == null ? "object" : schema.getFormat();
        }
        var schemaType = extensions != null ? extensions.get("x-type") : null;
        if (schemaType == null) {
            schemaType = schema.getType() == null ? "object" : schema.getType();
        }
        var baseType = dataType;

        if (TYPE_CHAR.equals(schemaType) || TYPE_CHARACTER.equals(schemaType)) {
            baseType = "char";
            dataType = "Char";
            dataTypeWithEnum = "Char";
        } else if (TYPE_BYTE.equals(schemaType)) {
            baseType = "byte";
            dataType = "Byte";
            dataTypeWithEnum = "Byte";
            isNumeric = true;
            isNumber = true;
            isInteger = true;
        } else if (INTEGER_TYPE.equals(schemaType) && (FORMAT_INT8.equals(format) || BYTE_FORMAT.equals(format))) {
            baseType = "Byte";
            dataType = "Byte";
            dataTypeWithEnum = "Byte";
            isNumeric = true;
            isNumber = true;
            isInteger = true;
        } else if (TYPE_SHORT.equals(schemaType)) {
            baseType = "short";
            dataType = "Short";
            dataTypeWithEnum = "Short";
            isNumeric = true;
            isNumber = true;
            isShort = true;
        } else if (INTEGER_TYPE.equals(schemaType) && (FORMAT_INT16.equals(format) || FORMAT_SHORT.equals(format))) {
            baseType = "Short";
            dataType = "Short";
            dataTypeWithEnum = "Short";
            isNumeric = true;
            isNumber = true;
            isShort = true;
        } else if (TYPE_INT.equals(schemaType)) {
            baseType = "int";
            dataType = "Int";
            dataTypeWithEnum = "Int";
            isNumeric = true;
            isNumber = true;
            isInteger = true;
        } else if (TYPE_LONG.equals(schemaType)) {
            baseType = "long";
            dataType = "Long";
            dataTypeWithEnum = "Long";
            isNumeric = true;
            isNumber = true;
            isLong = true;
        } else if (TYPE_FLOAT.equals(schemaType)) {
            baseType = "float";
            dataType = "Float";
            dataTypeWithEnum = "Float";
            isNumeric = true;
            isNumber = true;
            isFloat = true;
        } else if (TYPE_DOUBLE.equals(schemaType)) {
            baseType = "double";
            dataType = "Double";
            dataTypeWithEnum = "Double";
            isNumeric = true;
            isNumber = true;
            isDouble = true;
        }

        extMap.put("baseType", baseType);
        // kotlin has not primitive types
        extMap.put("isPrimitive", false);

        if (model != null) {
            model.dataType = dataType;
            model.isNumeric = isNumeric;
            model.isNumber = isNumber;
            model.isShort = isShort;
            model.isInteger = isInteger;
            model.isLong = isLong;
            model.isFloat = isFloat;
            model.isDouble = isDouble;
        } else if (prop != null) {
            prop.dataType = dataType;
            prop.datatypeWithEnum = dataTypeWithEnum;
            prop.isNumeric = isNumeric;
            prop.isNumber = isNumber;
            prop.isShort = isShort;
            prop.isInteger = isInteger;
            prop.isLong = isLong;
            prop.isFloat = isFloat;
            prop.isDouble = isDouble;
        } else if (param != null) {
            param.dataType = dataType;
            param.datatypeWithEnum = dataTypeWithEnum;
            param.isNumeric = isNumeric;
            param.isNumber = isNumber;
            param.isShort = isShort;
            param.isInteger = isInteger;
            param.isLong = isLong;
            param.isFloat = isFloat;
            param.isDouble = isDouble;
        } else {
            response.dataType = dataType;
            response.isNumeric = isNumeric;
            response.isNumber = isNumber;
            response.isShort = isShort;
            response.isInteger = isInteger;
            response.isLong = isLong;
            response.isFloat = isFloat;
            response.isDouble = isDouble;
        }
    }

    @Override
    public CodegenModel fromModel(String name, Schema schema) {
        CodegenModel model = super.fromModel(name, schema);
        checkPrimitives(model, unaliasSchema(schema));

        if (!model.oneOf.isEmpty()) {
            if (useOneOfInterfaces) {
                model.vendorExtensions.put("x-is-one-of-interface", true);
            }
            if (ModelUtils.isTypeObjectSchema(schema)) {
                CodegenModel m = CodegenModelFactory.newInstance(CodegenModelType.MODEL);
                updateModelForObject(m, schema);
                model.vars = m.vars;
                model.allVars = m.allVars;
                model.requiredVars = m.requiredVars;
                model.readWriteVars = m.readWriteVars;
                model.optionalVars = m.optionalVars;
                model.readOnlyVars = m.readOnlyVars;
                model.parentVars = m.parentVars;
                model.nonNullableVars = m.nonNullableVars;
                model.setRequiredVarsMap(m.getRequiredVarsMap());
                model.mandatory = m.mandatory;
                model.allMandatory = m.allMandatory;
            }
        }
        model.imports.remove("ApiModel");
        model.imports.remove("ApiModelProperty");
        if (importMapping.containsKey(model.dataType)
            && !model.imports.contains(model.dataType)) {
            model.imports.add(model.dataType);
        }
        allModels.put(name, model);
        return model;
    }

    /**
     * Output the type declaration of the property.
     *
     * @param p OpenAPI Property object
     * @return a string presentation of the property type
     */
    @Override
    public String getTypeDeclaration(Schema p) {
        Schema<?> schema = unaliasSchema(p);
        Schema<?> target = ModelUtils.isGenerateAliasAsModel() ? p : schema;
        if (ModelUtils.isArraySchema(target)) {
            Schema<?> items = ModelUtils.getSchemaItems(schema);
            return getSchemaType(target) + "<" + getTypeDeclaration(items) + ">";
        }

        if (ModelUtils.isMapSchema(target)) {
            // Note: ModelUtils.isMapSchema(p) returns true when p is a composed schema that also defines
            // additionalProperties: true
            Schema<?> inner = ModelUtils.getAdditionalProperties(target);
            if (inner == null) {
                log.error("`{}` (map property) does not have a proper inner type defined. Default to type:string", p.getName());
                inner = new StringSchema().description("TODO default missing map inner type to string");
                p.setAdditionalProperties(inner);
            }
            return getSchemaType(target) + "<String, " + getTypeDeclaration(inner) + ">";
        }
        return super.getTypeDeclaration(target);
    }

    @Override
    public String toModelName(final String name) {
        // obtain the name from modelNameMapping directly if provided
        if (modelNameMapping.containsKey(name)) {
            return modelNameMapping.get(name);
        }
        // memoization
        if (schemaKeyToModelNameCache.containsKey(name)) {
            return schemaKeyToModelNameCache.get(name);
        }

        // If schemaMapping contains name, assume this is a legitimate model name.
        if (schemaMapping.containsKey(name)) {
            return schemaMapping.get(name);
        }

        if (importMapping.containsKey(name)) {
            return name;
        }

        // Allow for explicitly configured kotlin.* and java.* types
        if (name.startsWith("kotlin.") || name.startsWith("java.")) {
            return name;
        }

        String modifiedName = name.replace(".", "")
            .replace("-", "_")
            // if it already escaped reserved word, need to remove quotes
            .replace("`", "");

        String nameWithPrefixSuffix = normalizeKotlinSpecificNames(modifiedName);
        if (!StringUtils.isEmpty(modelNamePrefix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = modelNamePrefix + "_" + nameWithPrefixSuffix;
        }

        if (!StringUtils.isEmpty(modelNameSuffix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = nameWithPrefixSuffix + "_" + modelNameSuffix;
        }

        // Camelize name of nested properties
        modifiedName = camelize(nameWithPrefixSuffix);

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(modifiedName)) {
            final String modelName = "Model" + modifiedName;
            log.warn("{} (reserved word) cannot be used as model name. Renamed to {}", modifiedName, modelName);
            return modelName;
        }

        // model name starts with number
        if (modifiedName.matches("^\\d.*")) {
            final String modelName = "Model" + modifiedName; // e.g. 200Response => Model200Response (after camelize)
            log.warn("{} (model name starts with number) cannot be used as model name. Renamed to {}", name, modelName);
            return modelName;
        }

        schemaKeyToModelNameCache.put(name, firstTitleCase(modifiedName));
        return schemaKeyToModelNameCache.get(name);
    }

    @Override
    public CodegenParameter fromParameter(Parameter p, Set<String> imports) {
        var parameter = super.fromParameter(p, imports);
        checkPrimitives(parameter, unaliasSchema(p.getSchema()));
        // if name is escaped
        var realName = parameter.paramName;
        if (realName.contains("`")) {
            realName = realName.replace("`", "");
        }
        parameter.vendorExtensions.put("realName", realName);

        Schema parameterSchema;
        if (p.getSchema() != null) {
            parameterSchema = unaliasSchema(p.getSchema());
        } else if (p.getContent() != null) {
            Content content = p.getContent();
            if (content.size() > 1) {
                once(log).warn("Multiple schemas found in content, returning only the first one");
            }
            Map.Entry<String, MediaType> entry = content.entrySet().iterator().next();
            parameterSchema = entry.getValue().getSchema();
        } else {
            parameterSchema = null;
        }
        if (parameterSchema != null && parameterSchema.get$ref() != null) {
            parameterSchema = openAPI.getComponents().getSchemas().get(parameterSchema.get$ref().substring("#/components/schemas/".length()));
        }

        String defaultValueInit;
        var items = parameter.items;
        if (items == null) {
            defaultValueInit = calcDefaultValues(null, null, false, parameterSchema).getLeft();
        } else {
            defaultValueInit = calcDefaultValues(items.datatypeWithEnum, items.dataType, items.getIsEnumOrRef(), parameterSchema).getLeft();
        }
        if (parameterSchema != null && ModelUtils.isEnumSchema(parameterSchema)) {
            var enumVarName = toEnumVarName(parameter.defaultValue, parameter.dataType);
            if (enumVarName != null) {
                defaultValueInit = parameter.dataType + "." + enumVarName;
            }
        }
        if (defaultValueInit != null) {
            parameter.vendorExtensions.put("defaultValueInit", defaultValueInit);
        }

        addStrValueToEnum(parameter.items);

        return parameter;
    }

    @Override
    public CodegenResponse fromResponse(String responseCode, ApiResponse response) {
        var resp = super.fromResponse(responseCode, response);
        checkPrimitives(resp, (Schema) resp.schema);
        return resp;
    }

    @Override
    public CodegenProperty fromProperty(String name, Schema schema, boolean required, boolean schemaIsFromAdditionalProperties) {
        var property = super.fromProperty(name, schema, required, schemaIsFromAdditionalProperties);
        checkPrimitives(property, unaliasSchema(schema));

        // if name is escaped
        var realName = property.name;
        if (realName.contains("`")) {
            realName = realName.replace("`", "");
            property.nameInCamelCase = camelize(realName);
            property.nameInSnakeCase = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, property.nameInCamelCase);
        }
        property.vendorExtensions.put("realName", realName);

        if (schema != null && schema.get$ref() != null) {
            schema = ModelUtils.getSchemaFromRefToSchemaWithProperties(openAPI, schema.get$ref());
        }

        String defaultValueInit;
        var items = property.items;
        if (items == null) {
            defaultValueInit = calcDefaultValues(null, null, false, schema).getLeft();
        } else {
            defaultValueInit = calcDefaultValues(items.datatypeWithEnum, items.dataType, items.getIsEnumOrRef(), schema).getLeft();
        }
        if (schema != null && ModelUtils.isEnumSchema(schema)) {
            var enumVarName = toEnumVarName(property.defaultValue, property.dataType);
            if (enumVarName != null) {
                defaultValueInit = property.dataType + "." + enumVarName;
            }
        }
        if (defaultValueInit != null) {
            property.vendorExtensions.put("defaultValueInit", defaultValueInit);
        }

        return property;
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        CodegenOperation op = super.fromOperation(path, httpMethod, operation, servers);

        if (op.isResponseFile) {
            op.returnType = typeMapping.get("responseFile");
            op.imports.add(op.returnType);
        }

        var paramsWithoutImplicitHeaders = new ArrayList<CodegenParameter>();
        var swaggerParams = new ArrayList<CodegenParameter>();
        var hasMultipleParams = false;
        var notBodyParamsSize = 0;
        for (var param : op.allParams) {
            if (!param.isHeaderParam || (!implicitHeaders && !shouldBeImplicitHeader(param))) {
                param.vendorExtensions.computeIfAbsent("realName", k -> param.paramName);
                paramsWithoutImplicitHeaders.add(param);
            }
            if (param.isBodyParam || param.isFormParam) {
                continue;
            }
            swaggerParams.add(param);
            notBodyParamsSize++;
            if (notBodyParamsSize > 1) {
                hasMultipleParams = true;
            }
        }
        op.vendorExtensions.put("swaggerParams", swaggerParams);
        op.vendorExtensions.put("originalParams", paramsWithoutImplicitHeaders);
        op.vendorExtensions.put("hasNotBodyParam", notBodyParamsSize > 0);
        op.vendorExtensions.put("hasMultipleParams", hasMultipleParams);
        for (var param : op.allParams) {
            param.vendorExtensions.put("hasNotBodyParam", notBodyParamsSize > 0);
            param.vendorExtensions.put("hasMultipleParams", hasMultipleParams);
        }
        op.vendorExtensions.put("originReturnProperty", op.returnProperty);
        if (op.responses != null && !op.responses.isEmpty()) {
            for (var resp : op.responses) {
                if (resp.isDefault) {
                    resp.code = "default";
                }
            }
        }

        processParametersWithAdditionalMappings(op.allParams, op.imports);
        processWithResponseBodyMapping(op);
        processOperationWithResponseWrappers(op);

        return op;
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        if (value == null) {
            return null;
        }
        String modified;
        if (value.isEmpty()) {
            modified = "EMPTY";
            return normalizeKotlinSpecificNames(modified);
        }

        String varName = value;

        // number
        if ("Int".equalsIgnoreCase(datatype)
            || "Byte".equalsIgnoreCase(datatype)
            || "Short".equalsIgnoreCase(datatype)
            || "Integer".equalsIgnoreCase(datatype)
            || "Long".equalsIgnoreCase(datatype)
            || "Float".equalsIgnoreCase(datatype)
            || "Double".equalsIgnoreCase(datatype)
            || "BigDecimal".equals(datatype)) {
            varName = "NUMBER_" + varName;
            varName = varName.replaceAll("-", "MINUS_");
            varName = varName.replaceAll("\\+", "PLUS_");
            varName = varName.replaceAll("\\.", "_DOT_");
        }
        varName = varName.replaceAll("[^a-zA-Z0-9_]", "_");

        if (" ".equals(varName)) {
            return "SPACE";
        }

        return super.toEnumVarName(varName, datatype);
    }

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    /**
     * Method that maps parameters if a corresponding mapping is specified.
     *
     * @param params The parameters to modify.
     * @param imports The operation imports.
     */
    private void processParametersWithAdditionalMappings(List<CodegenParameter> params, Set<String> imports) {
        var additionalMappings = new LinkedHashMap<String, ParameterMapping>();
        Iterator<CodegenParameter> iter = params.iterator();
        while (iter.hasNext()) {
            CodegenParameter param = iter.next();
            boolean paramWasMapped = false;
            for (ParameterMapping mapping : parameterMappings) {
                if (mapping.doesMatch(param)) {
                    additionalMappings.put(mapping.mappedName(), mapping);
                    paramWasMapped = true;
                }
            }
            if (paramWasMapped) {
                iter.remove();
            } else {
                if (plural && param.isArray && param.isBodyParam
                    && StringUtils.isEmpty(param.getRef())
                    && !DEFAULT_BODY_PARAM_NAME.equals(param.paramName)) {
                    param.paramName = English.plural(param.paramName);
                }
            }
        }

        for (ParameterMapping mapping : additionalMappings.values()) {
            if (mapping.mappedType() == null) {
                continue;
            }
            var newParam = new CodegenParameter();
            newParam.paramName = mapping.mappedName();
            newParam.required = true;
            newParam.isModel = mapping.isValidated();

            String typeName = makeSureImported(mapping.mappedType(), imports);
            newParam.dataType = typeName;

            // Set the paramName if required
            if (newParam.paramName == null) {
                newParam.paramName = toParamName(typeName);
            }

            params.add(newParam);
        }
    }

    /**
     * Method that changes the return type if the corresponding header is specified.
     *
     * @param op The operation to modify.
     */
    private void processWithResponseBodyMapping(CodegenOperation op) {
        ResponseBodyMapping bodyMapping = null;

        Iterator<CodegenProperty> iter = op.responseHeaders.iterator();
        while (iter.hasNext()) {
            CodegenProperty header = iter.next();
            boolean headerWasMapped = false;
            for (ResponseBodyMapping mapping : responseBodyMappings) {
                if (mapping.doesMatch(header.baseName, op.isArray)) {
                    if (mapping.mappedBodyType() != null) {
                        bodyMapping = mapping;
                    }
                    headerWasMapped = true;
                }
            }
            if (headerWasMapped) {
                iter.remove();
            }
        }

        if (bodyMapping != null) {
            wrapOperationReturnType(op, bodyMapping.mappedBodyType(), bodyMapping.isValidated(), bodyMapping.isListWrapper());
        }
    }

    /**
     * Wrap the return type of operation in the provided type.
     *
     * @param op The operation to modify.
     * @param wrapperType The wrapper type.
     * @param isValidated Whether the wrapper requires validation.
     * @param isListWrapper Whether the wrapper should be around list items.
     */
    private void wrapOperationReturnType(CodegenOperation op, String wrapperType, boolean isValidated, boolean isListWrapper) {
        CodegenProperty newReturnType = new CodegenProperty();
        newReturnType.required = true;
        newReturnType.isModel = isValidated;

        String typeName = makeSureImported(wrapperType, op.imports);

        String originalReturnType;
        if ((isListWrapper || fluxForArrays) && op.isArray && op.returnProperty.items != null) {
            if (fluxForArrays && wrapperType.equals(MONO_CLASS_NAME)) {
                typeName = makeSureImported(FLUX_CLASS_NAME, op.imports);
                op.vendorExtensions.put("isReturnFlux", true);
            }
            originalReturnType = op.returnBaseType;
            newReturnType.dataType = typeName + '<' + op.returnBaseType + '>';
            newReturnType.items = op.returnProperty.items;
        } else {
            originalReturnType = op.returnType;
            if (originalReturnType == null) {
                originalReturnType = "Void";
                op.returnProperty = new CodegenProperty();
                op.returnProperty.dataType = "Void";
            }
            newReturnType.dataType = typeName + '<' + originalReturnType + '>';
            newReturnType.items = op.returnProperty;
        }

        op.returnType = newReturnType.dataType;
        op.returnContainer = null;
        op.returnProperty = newReturnType;
        op.isArray = op.returnProperty.isArray;
    }

    private void processOperationWithResponseWrappers(CodegenOperation op) {
        boolean hasNon200StatusCodes = op.responses.stream().anyMatch(
            response -> !"200".equals(response.code) && response.code.startsWith("2")
        );
        boolean hasNonMappedHeaders = !op.responseHeaders.isEmpty();
        boolean requiresHttpResponse = hasNon200StatusCodes || hasNonMappedHeaders;
        if (generateHttpResponseAlways || (generateHttpResponseWhereRequired && requiresHttpResponse)) {
            wrapOperationReturnType(op, "io.micronaut.http.HttpResponse", false, false);
        }

        if (reactive) {
            wrapOperationReturnType(op, MONO_CLASS_NAME, false, false);
        }
    }

    private String makeSureImported(String typeName, Set<String> imports) {
        // Find the index of the first capital letter
        int firstCapitalIndex = 0;
        for (int i = 0; i < typeName.length(); i++) {
            if (Character.isUpperCase(typeName.charAt(i))) {
                firstCapitalIndex = i;
                break;
            }
        }

        // Add import if the name is fully-qualified
        if (firstCapitalIndex != 0) {
            // Add import if fully-qualified name is used
            String dataType = typeName.substring(firstCapitalIndex);
            importMapping.put(dataType, typeName);
            typeName = dataType;
        }
        imports.add(typeName);
        return typeName;
    }

    @Override
    public String toVarName(String name) {
        var varName = super.toVarName(name);

        if (varName.chars().allMatch(c -> Character.isUpperCase(c) || c == '_')) {
            return varName;
        }

        // Micronaut can't process correctly properties like `eTemperature`, when first symbol in lower case
        // and second symbol in upper case.
        // See this: https://github.com/micronaut-projects/micronaut-core/pull/10130
        if (varName.length() >= 2 && Character.isLowerCase(varName.charAt(0)) && Character.isUpperCase(varName.charAt(1))) {
            varName = "" + varName.charAt(0) + Character.toLowerCase(varName.charAt(1)) + varName.substring(2);
        }

        return varName;
    }

    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        objs = super.postProcessAllModels(objs);

        if (!additionalOneOfTypeAnnotations.isEmpty()) {
            for (String modelName : objs.keySet()) {
                Map<String, Object> models = objs.get(modelName);
                models.put(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS, additionalOneOfTypeAnnotations);
            }
        }

        if (!additionalEnumTypeAnnotations.isEmpty()) {
            for (String modelName : objs.keySet()) {
                Map<String, Object> models = objs.get(modelName);
                models.put(ADDITIONAL_ENUM_TYPE_ANNOTATIONS, additionalEnumTypeAnnotations);
            }
        }

        var isServer = isServer();

        for (ModelsMap models : objs.values()) {
            CodegenModel model = models.getModels().get(0).getModel();

            var requiredVarsWithoutDiscriminator = new ArrayList<CodegenProperty>();
            var requiredParentVarsWithoutDiscriminator = new ArrayList<CodegenProperty>();
            var allVars = new ArrayList<CodegenProperty>();

            processParentModel(model, requiredVarsWithoutDiscriminator, requiredParentVarsWithoutDiscriminator, allVars, false);
            model.allVars = allVars;

            var withInheritance = model.hasChildren || model.parent != null;
            model.vendorExtensions.put("withInheritance", withInheritance);

            var optionalVars = new ArrayList<CodegenProperty>();
            var requiredVars = new ArrayList<CodegenProperty>();
            for (var v : model.vars) {
                v.vendorExtensions.put("hasChildren", model.hasChildren);
                if (!notContainsProp(v, requiredVarsWithoutDiscriminator)
                    || (!model.hasChildren || (v.required && !v.isDiscriminator))) {
                    if (notContainsProp(v, requiredVars)) {
                        requiredVars.add(v);
                    }
                } else {
                    if (notContainsProp(v, optionalVars)) {
                        optionalVars.add(v);
                    }
                }
            }

            model.vendorExtensions.put("hasOwnVars", !model.vars.isEmpty());
            model.vendorExtensions.put("withMultipleVars", model.vars.size() > 1);
            if (!requiredParentVarsWithoutDiscriminator.isEmpty()) {
                model.vendorExtensions.put("requiredParentVarsWithoutDiscriminator", requiredParentVarsWithoutDiscriminator);
            }
            model.vendorExtensions.put("requiredVarsWithoutDiscriminator", requiredVarsWithoutDiscriminator);
            model.vendorExtensions.put("requiredVars", requiredVars);
            model.vendorExtensions.put("withRequiredOrOptionalVars", !requiredVarsWithoutDiscriminator.isEmpty() || !optionalVars.isEmpty());
            model.vendorExtensions.put("optionalVars", optionalVars);
            model.vendorExtensions.put("serialId", random.nextLong());
            model.vendorExtensions.put("withRequiredVars", !model.requiredVars.isEmpty());
            normalizeExtraAnnotations(EXT_ANNOTATIONS_CLASS, true, model.vendorExtensions);
            if (model.discriminator != null) {
                model.vendorExtensions.put("hasMappedModels", !model.discriminator.getMappedModels().isEmpty());
                model.discriminator.getVendorExtensions().put("hasMappedModels", !model.discriminator.getMappedModels().isEmpty());
            }
            for (var property : model.vars) {
                processProperty(property, isServer, model, objs);
            }
            for (var property : model.requiredVars) {
                processProperty(property, isServer, model, objs);
            }
            if (model.parentVars != null) {
                for (var property : model.parentVars) {
                    processProperty(property, isServer, model, objs);
                }
            }

            model.hasVars = !requiredVarsWithoutDiscriminator.isEmpty() || (withInheritance && !optionalVars.isEmpty());
            // just for tests
            if (System.getProperty("micronaut.test.no-vars") != null) {
                model.hasVars = false;
                model.vendorExtensions.put("hasOwnVars", false);
                model.vendorExtensions.put("requiredVarsWithoutDiscriminator", Collections.emptyList());
                model.vendorExtensions.put("optionalVars", Collections.emptyList());
                model.vendorExtensions.put("requiredVars", Collections.emptyList());
                model.vendorExtensions.put("withRequiredVars", false);
                model.vars = Collections.emptyList();
            }

            addStrValueToEnum(model);
        }

        for (ModelsMap models : objs.values()) {
            CodegenModel model = models.getModels().get(0).getModel();
            processOneOfModels(model, objs.values());
        }

        return objs;
    }

    @Override
    protected void updateEnumVarsWithExtensions(List<Map<String, Object>> enumVars, Map<String, Object> vendorExtensions, String dataType) {
        super.updateEnumVarsWithExtensions(enumVars, vendorExtensions, dataType);
        if (vendorExtensions == null) {
            return;
        }
        var xDeprecated = (List<Object>) vendorExtensions.get("x-deprecated");
        if (xDeprecated != null && !xDeprecated.isEmpty()) {
            for (var deprecatedItem : xDeprecated) {
                Map<String, Object> foundEnumVar = null;
                for (var enumVar : enumVars) {
                    var isString = (boolean) enumVar.get("isString");
                    var value = (String) enumVar.get("value");
                    if (!isString) {
                        if (value.startsWith("(short)")) {
                            value = value.replace("(short) ", "");
                        }
                        var argPos = value.indexOf('(');
                        // case for BigDecimal
                        if (argPos >= 0) {
                            value = value.substring(argPos + 1, value.indexOf(')'));
                        }
                        var upperValue = value.toUpperCase();
                        if (upperValue.endsWith("F")
                            || upperValue.endsWith("L")
                            || upperValue.endsWith("D")) {
                            value = value.substring(0, value.length() - 1);
                        }
                        if (!value.contains("'")) {
                            value = value.replace("'", "");
                        }
                        if (!value.contains("\"")) {
                            value = "\"" + value + "\"";
                        }
                    }
                    if (value.equals("\"" + deprecatedItem + '"')) {
                        foundEnumVar = enumVar;
                        break;
                    }
                }
                if (foundEnumVar != null) {
                    foundEnumVar.put("deprecated", true);
                }
            }
        }

        var baseType = (String) vendorExtensions.get("baseType");
        for (var enumVar : enumVars) {
            if ((boolean) enumVar.get("isString")) {
                continue;
            }
            var value = (String) enumVar.get("value");
            value = value.replace("\"", "");
            if ("char".equals(baseType) && !value.startsWith("'")) {
                enumVar.put("value", "'" + value + "'");
            } else if ("short".equalsIgnoreCase(baseType)) {
                enumVar.put("value", value);
            } else if ("byte".equalsIgnoreCase(baseType)) {
                enumVar.put("value", value);
            }
        }
    }

    private void processOneOfModels(CodegenModel model, Collection<ModelsMap> models) {

        if (!model.vendorExtensions.containsKey("x-is-one-of-interface")
            || !Boolean.parseBoolean(model.vendorExtensions.get("x-is-one-of-interface").toString())) {
            return;
        }

        var discriminator = model.discriminator;
        if (discriminator == null) {
            return;
        }
        var oneOfInterfaceName = model.name;
        for (var modelMap : models) {
            var m = modelMap.getModels().get(0).getModel();

            if (!m.vendorExtensions.containsKey("x-implements")) {
                continue;
            }
            var xImplements = m.vendorExtensions.get("x-implements");
            if (!(xImplements instanceof List<?> xImplementsList)) {
                continue;
            }
            for (var implInterface : xImplementsList) {
                if (!oneOfInterfaceName.equalsIgnoreCase(implInterface.toString())) {
                    continue;
                }
                for (var prop : m.allVars) {
                    if (prop.name.equals(discriminator.getPropertyName())) {
                        prop.isDiscriminator = true;
                        prop.isOverridden = true;
                        prop.isNullable = false;
                        prop.isOptional = false;
                        prop.required = true;
                        prop.isReadOnly = false;
                        prop.vendorExtensions.put("typeWithEnumWithGenericAnnotations", discriminator.getPropertyType());
                        prop.vendorExtensions.put("overridden", true);
                        break;
                    }
                }
                var properties = (ArrayList<CodegenProperty>) m.vendorExtensions.get("requiredVarsWithoutDiscriminator");
                if (properties != null && !properties.isEmpty()) {
                    for (var prop : properties) {
                        if (prop.name.equals(discriminator.getPropertyName())) {
                            prop.isDiscriminator = true;
                            prop.isOverridden = true;
                            prop.isNullable = false;
                            prop.isOptional = false;
                            prop.required = true;
                            prop.isReadOnly = false;
                            prop.vendorExtensions.put("typeWithEnumWithGenericAnnotations", discriminator.getPropertyType());
                            prop.vendorExtensions.put("overridden", true);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void processProperty(CodegenProperty property, boolean isServer, CodegenModel model, Map<String, ModelsMap> models) {

        property.vendorExtensions.put("withRequiredAndOptionalVars", model.vendorExtensions.get("withRequiredAndOptionalVars"));
        property.vendorExtensions.put("inRequiredArgsConstructor", !property.isReadOnly || isServer);
        property.vendorExtensions.put("isServer", isServer);
        property.vendorExtensions.put("defaultValueIsNotNull", property.defaultValue != null && !property.defaultValue.equals("null"));
        property.vendorExtensions.put("fieldAnnPrefix", ksp ? "" : "field:");
        property.vendorExtensions.put("x-implements", model.vendorExtensions.get("x-implements"));
        if ("null".equals(property.example)) {
            property.example = null;
        }
        if (useBeanValidation && (
            (!property.isContainer && property.isModel)
                || (property.getIsArray() && property.getComplexType() != null && models.containsKey(property.getComplexType()))
        )) {
            property.vendorExtensions.put("withValid", true);
        }

        // check pattern property for date types: if set, need use this pattern as `@Format` annotation value
        if (isDateType(property.dataType) && StringUtils.isNotEmpty(property.pattern)) {
            property.vendorExtensions.put("formatPattern", property.pattern);
            property.pattern = null;
        }

        processGenericAnnotations(property, useBeanValidation, false, property.isNullable || property.isDiscriminator,
            property.required, property.isReadOnly, true);

        normalizeExtraAnnotations(EXT_ANNOTATIONS_FIELD, true, property.vendorExtensions);
        normalizeExtraAnnotations(EXT_ANNOTATIONS_SETTER, true, property.vendorExtensions);
    }

    private void processParentModel(CodegenModel model, List<CodegenProperty> requiredVarsWithoutDiscriminator,
                                    List<CodegenProperty> requiredParentVarsWithoutDiscriminator,
                                    List<CodegenProperty> allVars,
                                    boolean processParentModel) {
        var parent = model.parentModel;
        var hasParent = parent != null;

        for (var variable : model.vars) {
            if (notContainsProp(variable, allVars)) {
                allVars.add(variable);
            }
        }

        var parentIsOneOfInterface = hasParent && Boolean.TRUE.equals(parent.getVendorExtensions().get("x-is-one-of-interface"));

        if (!processParentModel) {
            processVar(model, model.vars, requiredVarsWithoutDiscriminator, requiredParentVarsWithoutDiscriminator, processParentModel);
        }
        processVar(model, model.requiredVars, requiredVarsWithoutDiscriminator, requiredParentVarsWithoutDiscriminator, processParentModel);

        requiredParentVarsWithoutDiscriminator(model, requiredParentVarsWithoutDiscriminator);
        if (hasParent) {
            model.parentVars = parent.allVars;
            processParentModel(parent, requiredVarsWithoutDiscriminator, requiredParentVarsWithoutDiscriminator, allVars, true);
        }

        if (parentIsOneOfInterface) {
            for (var variable : parent.vars) {
                if (notContainsProp(variable, model.vars)) {
                    if (parent.discriminator != null && parent.discriminator.getPropertyName().equals(variable.name)) {
                        variable.isDiscriminator = true;
                        variable.isOverridden = true;
                    }
                    model.vars.add(variable);
                }
            }
            for (var variable : parent.requiredVars) {
                if (notContainsProp(variable, model.requiredVars)) {
                    model.requiredVars.add(variable);
                }
            }
            model.parentModel = null;
            model.parent = null;
            model.parentVars = null;
        }
    }

    private void processVar(CodegenModel model, List<CodegenProperty> vars, List<CodegenProperty> requiredVarsWithoutDiscriminator, List<CodegenProperty> requiredParentVarsWithoutDiscriminator, boolean processParentModel) {

        var parent = model.parentModel;
        var hasParent = parent != null;
        var parentRequiredVars = hasParent
            ? (List<CodegenProperty>) parent.vendorExtensions.get("requiredVarsWithoutDiscriminator")
            : Collections.<CodegenProperty>emptyList();
        var parentIsOneOfInterface = hasParent && Boolean.TRUE.equals(parent.getVendorExtensions().get("x-is-one-of-interface"));

        for (var v : vars) {

            if (notContainsProp(v, requiredVarsWithoutDiscriminator) && (!isDiscriminator(v, model) || parentIsOneOfInterface)) {
                var copyVar = v;
                if ((hasParent && !useOneOfInterfaces && !notContainsProp(v, parent.allVars))
                    || (v.isOverridden != null && !v.isOverridden && !v.vendorExtensions.containsKey("overridden") && notContainsProp(v, vars))) {
                    copyVar = v.clone();
                    copyVar.isOverridden = true;
                    copyVar.vendorExtensions.put("overridden", true);
                }
                v.vendorExtensions.put("hasChildren", model.hasChildren);
                if (!hasParent || notContainsProp(v, parentRequiredVars) || parentIsOneOfInterface) {
                    if (processParentModel) {
                        copyVar = v.clone();
                        copyVar.isOverridden = true;
                    }
                    if (copyVar.isOverridden != null && copyVar.isOverridden) {
                        copyVar.vendorExtensions.put("overridden", true);
                    }
                    requiredVarsWithoutDiscriminator.add(copyVar);
                }
            }
            v.isNullable = v.isNullable || (v.required && v.isReadOnly && isServer()) || !v.required;
        }
    }

    private void requiredParentVarsWithoutDiscriminator(CodegenModel model, List<CodegenProperty> requiredParentVarsWithoutDiscriminator) {

        var parent = model.parentModel;
        if (parent == null) {
            return;
        }

        for (var v : parent.vars) {
            boolean isDiscriminator = isDiscriminator(v, model);
            if (v.required && !isDiscriminator) {
                v.vendorExtensions.put("isServerOrNotReadOnly", !v.isReadOnly || isServer());
                if (notContainsProp(v, requiredParentVarsWithoutDiscriminator)) {
                    var childVar = v.clone();
                    if (!useOneOfInterfaces) {
                        childVar.isOverridden = true;
                        childVar.vendorExtensions.put("overridden", true);
                    }
                    requiredParentVarsWithoutDiscriminator.add(childVar);
                }
            }
        }
    }

    private boolean notContainsProp(CodegenProperty prop, List<CodegenProperty> props) {
        if (props == null || props.isEmpty()) {
            return true;
        }
        for (var p : props) {
            if (prop.name.equals(p.name)) {
                return false;
            }
        }
        return true;
    }

    private boolean isDiscriminator(CodegenProperty prop, CodegenModel model) {
        var isDiscriminator = prop.isDiscriminator;
        if (isDiscriminator) {
            return true;
        }
        if (model.parentModel == null) {
            return false;
        }
        CodegenProperty parentProp = null;
        for (var pv : model.parentModel.allVars) {
            if (pv.required && pv.name.equals(prop.name)) {
                isDiscriminator = pv.isDiscriminator;
                parentProp = pv;
                break;
            }
        }
        if (isDiscriminator) {
            return true;
        }
        return parentProp != null && isDiscriminator(parentProp, model.parentModel);
    }

    @Override
    public void setParameterExampleValue(CodegenParameter codegenParameter, Parameter parameter) {
        if (parameter.getExample() != null) {
            codegenParameter.example = parameter.getExample().toString();
            return;
        }

        if (parameter.getExamples() != null && !parameter.getExamples().isEmpty()) {
            Example example = parameter.getExamples().values().iterator().next();
            if (example.getValue() != null) {
                codegenParameter.example = example.getValue().toString();
            }
        } else {

            Schema<?> schema = parameter.getSchema();
            if (schema != null && schema.getExample() != null) {
                codegenParameter.example = schema.getExample().toString();
            }
        }

        setParameterExampleValue(codegenParameter);
    }

    @Override
    public void setParameterExampleValue(CodegenParameter p) {
        p.example = getParameterExampleValue(p);
    }

    protected String getParameterExampleValue(CodegenParameter p) {
        List<Object> allowableValues = p.allowableValues == null ? null : (List<Object>) p.allowableValues.get("values");

        return getExampleValue(p.defaultValue, p.example, p.dataType, p.isModel, allowableValues,
            p.items == null ? null : p.items.dataType,
            p.items == null ? null : p.items.defaultValue,
            p.requiredVars, false);
    }

    protected String getPropertyExampleValue(CodegenProperty p) {
        List<Object> allowableValues = p.allowableValues == null ? null : (List<Object>) p.allowableValues.get("values");
        var model = allModels.get(p.getDataType());

        return getExampleValue(p.defaultValue, p.example, p.dataType, p.isModel, allowableValues,
            p.items == null ? null : p.items.dataType,
            p.items == null ? null : p.items.defaultValue,
            model != null ? model.requiredVars : null, true);
    }

    private boolean withExample(String example) {
        return example != null && !example.equals("null");
    }

    public String getExampleValue(
        String defaultValue, String example, String dataType, Boolean isModel, List<Object> allowableValues,
        String itemsType, String itemsExample, List<CodegenProperty> requiredVars, boolean isProperty
    ) {
        example = defaultValue != null ? defaultValue : example;
        String containerType = dataType == null ? null : dataType.split("<")[0];

        if ("String".equals(dataType)) {
            example = withExample(example) ? "\"" + escapeText(example) + "\"" : "\"example\"";
        } else if ("Int".equals(dataType) || "Short".equals(dataType)) {
            example = withExample(example) ? example : "56";
        } else if ("Long".equals(dataType)) {
            example = StringUtils.appendIfMissingIgnoreCase(withExample(example) ? example : "56", "L");
        } else if ("Float".equals(dataType)) {
            example = StringUtils.appendIfMissingIgnoreCase(withExample(example) ? example : "3.4", "F");
        } else if ("Double".equals(dataType)) {
            example = StringUtils.appendIfMissingIgnoreCase(withExample(example) ? example : "3.4", "D");
        } else if ("Boolean".equals(dataType)) {
            example = withExample(example) ? example : "false";
        } else if ("File".equals(dataType) || "java.io.File".equals(dataType)) {
            example = null;
        } else if ("OffsetDateTime".equals(dataType)) {
            example = "OffsetDateTime.of(2001, 2, 3, 12, 0, 0, 0, java.time.ZoneOffset.of(\"+02:00\"))";
        } else if ("LocalDate".equals(dataType)) {
            example = "LocalDate.of(2001, 2, 3)";
        } else if ("LocalDateTime".equals(dataType)) {
            example = "LocalDateTime.of(2001, 2, 3, 4, 5)";
        } else if ("ByteArray".equals(dataType)) {
            example = "ByteArray(10)";
        } else if ("BigDecimal".equals(dataType)) {
            example = "BigDecimal(\"78\")";
        } else if ("MultipartBody".equals(dataType)) {
            example = "MultipartBody.builder().build()";
        } else if (allowableValues != null && !allowableValues.isEmpty()) {
            // This is an enum
            Object value = example;
            if (value == null || !allowableValues.contains(value)) {
                value = allowableValues.get(0);
            }
            if (isProperty) {
                dataType = importMapping.getOrDefault(dataType, modelPackage + '.' + dataType);
            }
            example = dataType + ".fromValue(\"" + value + "\")";
        } else if ((isModel != null && isModel) || (isModel == null && !languageSpecificPrimitives.contains(dataType))) {
            if (requiredVars == null) {
                example = null;
            } else {
                if (requiredPropertiesInConstructor) {
                    var builder = new StringBuilder();
                    if (isProperty) {
                        dataType = importMapping.getOrDefault(dataType, modelPackage + '.' + dataType);
                    }
                    builder.append(dataType).append("(");
                    for (int i = 0; i < requiredVars.size(); ++i) {
                        if (i != 0) {
                            builder.append(", ");
                        }
                        builder.append(getPropertyExampleValue(requiredVars.get(i)));
                    }
                    builder.append(")");
                    example = builder.toString();
                } else {
                    example = dataType + "()";
                }
            }
        }

        if ("List".equals(containerType)) {
            String innerExample;
            if ("String".equals(itemsType)) {
                itemsExample = itemsExample != null ? itemsExample : "example";
                innerExample = "\"" + escapeText(itemsExample) + "\"";
            } else {
                innerExample = itemsExample != null ? itemsExample : "";
            }

            if (StringUtils.isNotEmpty(innerExample)) {
                example = "listOf(" + innerExample + ")";
            } else {
                example = "listOf<" + itemsType + ">()";
            }
        } else if ("Set".equals(containerType)) {
            example = "HashSet<Any>()";
        } else if ("Map".equals(containerType)) {
            example = "HashMap<Any, Any>()";
        } else if (example == null) {
            example = "null";
        }

        return example;
    }

    @Override
    public String toDefaultValue(CodegenProperty cp, Schema schema) {
        if (cp.items != null) {
            return calcDefaultValues(cp.items.datatypeWithEnum, cp.items.dataType, cp.items.getIsEnumOrRef(), schema).getRight();
        } else {
            return calcDefaultValues(null, null, false, schema).getRight();
        }
    }

    private Pair<String, String> calcDefaultValues(String itemsDatatypeWithEnum, String itemsDataType, boolean itemsIsEnumOrRef, Schema schema) {
        String defaultValueInit = null;
        String defaultValueStr = null;
        schema = ModelUtils.getReferencedSchema(this.openAPI, schema);
        if (ModelUtils.isBooleanSchema(schema)) {
            if (schema.getDefault() != null) {
                defaultValueInit = schema.getDefault().toString();
                defaultValueStr = schema.getDefault().toString();
            }
        } else if (ModelUtils.isDateSchema(schema)) {
            // TODO
            defaultValueInit = null;
            defaultValueStr = null;
        } else if (ModelUtils.isDateTimeSchema(schema)) {
            // TODO
            defaultValueInit = null;
            defaultValueStr = null;
        } else if (ModelUtils.isNumberSchema(schema)) {
            if (schema.getDefault() != null) {
                defaultValueInit = fixNumberValue(schema.getDefault().toString(), schema);
                defaultValueStr = schema.getDefault().toString();
            }
        } else if (ModelUtils.isIntegerSchema(schema)) {
            if (schema.getDefault() != null) {
                defaultValueInit = fixNumberValue(schema.getDefault().toString(), schema);
                defaultValueStr = schema.getDefault().toString();
            }
        } else if (ModelUtils.isURISchema(schema)) {
            if (schema.getDefault() != null) {
                defaultValueInit = importMapping.get("URI") + ".create(\"" + schema.getDefault() + "\")";
                defaultValueStr = schema.getDefault().toString();
            }
        } else if (ModelUtils.isArraySchema(schema) && itemsDatatypeWithEnum != null) {
            var pair = toArrayDefaultValue(itemsDatatypeWithEnum, itemsDataType, itemsIsEnumOrRef, schema);
            defaultValueInit = pair.getLeft();
            defaultValueStr = pair.getRight();
        } else if (ModelUtils.isStringSchema(schema)) {
            if (schema.getDefault() != null) {
                String def = schema.getDefault().toString();
                if (schema.getEnum() == null) {
                    defaultValueInit = "\"" + escapeText(def) + "\"";
                    defaultValueStr = escapeText(def);
                } else {
                    // convert to enum var name later in postProcessModels
                    defaultValueInit = "\"" + def + "\"";
                    defaultValueStr = def;
                }
            }
        } else if (ModelUtils.isObjectSchema(schema)) {
            if (schema.getDefault() != null) {
                defaultValueInit = super.toDefaultValue(schema);
                defaultValueStr = super.toDefaultValue(schema);
            }
        }

        return Pair.of(defaultValueInit, defaultValueStr);
    }

    private String fixNumberValue(String number, Schema p) {
        if (ModelUtils.isFloatSchema(p)) {
            return number + "F";
        } else if (ModelUtils.isDoubleSchema(p)) {
            if (number.contains(".")) {
                return number;
            }
            return number + ".0";
        } else if (ModelUtils.isLongSchema(p)) {
            return number + "L";
        }
        return number;
    }

    // left - initStr, right - defaultStr
    private Pair<String, String> toArrayDefaultValue(String itemsDatatypeWithEnum, String itemsDataType, boolean itemsIsEnumOrRef, Schema schema) {
        if (schema.getDefault() != null) {
            String arrInstantiationType = ModelUtils.isSet(schema) ? "set" : "arrayList";

            if (!(schema.getDefault() instanceof ArrayNode def)) {
                return Pair.of(null, null);
            }
            if (def.isEmpty()) {
                return Pair.of(arrInstantiationType + "Of()", null);
            }

            var defaultContent = new StringBuilder();
            Schema<?> itemsSchema = ModelUtils.getSchemaItems(schema);
            def.elements().forEachRemaining((element) -> {
                String defaultValue = element.asText();
                if (defaultValue != null) {
                    if (itemsIsEnumOrRef) {
                        String className = itemsDatatypeWithEnum;
                        String enumVarName = toEnumVarName(defaultValue, itemsDataType);
                        if (enumVarName != null) {
                            defaultContent.append(className).append(".").append(enumVarName).append(",");
                        } else {
                            defaultContent.append("null").append(",");
                        }
                    } else {
                        itemsSchema.setDefault(defaultValue);
                        defaultValue = calcDefaultValues(itemsDatatypeWithEnum, itemsDataType, itemsIsEnumOrRef, itemsSchema).getRight();
                        defaultContent.append(defaultValue).append(",");
                    }
                }
            });
            defaultContent.deleteCharAt(defaultContent.length() - 1); // remove trailing comma
            return Pair.of(arrInstantiationType + "Of(" + defaultContent + ")", defaultContent.toString());
        }
        return Pair.of(null, null);
    }

    @Override
    protected ImmutableMap.Builder<String, Mustache.Lambda> addMustacheLambdas() {
        return super.addMustacheLambdas()
            .put("replaceDotsWithUnderscore", new ReplaceDotsWithUnderscoreLambda());
    }

    public String getPackageName() {
        return packageName;
    }

    public void setSerializationLibrary(final String serializationLibrary) {
        try {
            this.serializationLibrary = SerializationLibraryKind.valueOf(serializationLibrary).name();
        } catch (IllegalArgumentException ex) {
            StringBuilder sb = new StringBuilder(serializationLibrary + " is an invalid enum property naming option. Please choose from:");
            for (SerializationLibraryKind availableSerializationLibrary : SerializationLibraryKind.values()) {
                sb.append("\n  ").append(availableSerializationLibrary.name());
            }
            throw new RuntimeException(sb.toString());
        }
    }

    public void setDateTimeLibrary(String name) {
        dateLibrary = name;
    }

    /**
     * Sanitize against Kotlin specific naming conventions, which may differ from those required by {@link DefaultCodegen#sanitizeName}.
     *
     * @param name string to be sanitize
     * @return sanitized string
     */
    private String normalizeKotlinSpecificNames(final String name) {
        if (typeMapping.containsValue(name)) {
            return name;
        }
        String word = name;
        for (Map.Entry<String, String> specialCharacters : specialCharReplacements.entrySet()) {
            word = replaceSpecCharacters(word, specialCharacters);
        }

        // Fallback, replace unknowns with underscore.
        word = Pattern.compile("\\W+", Pattern.UNICODE_CHARACTER_CLASS).matcher(word).replaceAll("_");
        if (word.matches("\\d.*")) {
            word = "_" + word;
        }

        // _, __, and ___ are reserved in Kotlin. Treat all names with only underscores consistently, regardless of count.
        if (word.matches("^_*$")) {
            word = word.replaceAll("\\Q_\\E", "Underscore");
        }

        return word;
    }

    private String replaceSpecCharacters(String word, Map.Entry<String, String> specialCharacters) {
        String specialChar = specialCharacters.getKey();
        String replacementChar = specialCharacters.getValue();
        // Underscore is the only special character we'll allow
        if (!specialChar.equals("_") && word.contains(specialChar)) {
            return replaceChars(word, specialChar, replacementChar);
        }
        return word;
    }

    private String replaceChars(String word, String oldValue, String newValue) {
        if (!word.contains(oldValue)) {
            return word;
        }
        if (word.equals(oldValue)) {
            return newValue;
        }
        int i = word.indexOf(oldValue);
        String start = word.substring(0, i);
        String end = recurseOnEndOfTheWord(word, oldValue, newValue, i);
        return start + newValue + end;
    }

    private String recurseOnEndOfTheWord(String word, String oldValue, String newValue, int lastReplacedValue) {
        String end = word.substring(lastReplacedValue + 1);
        if (!end.isEmpty()) {
            end = firstTitleCase(end);
            end = replaceChars(end, oldValue, newValue);
        }
        return end;
    }

    private String firstTitleCase(final String input) {
        return input.substring(0, 1).toUpperCase(Locale.ROOT) + input.substring(1);
    }

    @Override
    public void setUseOneOfInterfaces(Boolean useOneOfInterfaces) {
        super.setUseOneOfInterfaces(useOneOfInterfaces);
        additionalProperties.put("useOneOfInterfaces", useOneOfInterfaces);
    }

    @Override
    public boolean getUseInlineModelResolver() {
        return false;
    }

    public void setGenerateSwaggerAnnotations(boolean generateSwaggerAnnotations) {
        this.generateSwaggerAnnotations = Boolean.toString(generateSwaggerAnnotations);
        if (generateSwaggerAnnotations) {
            additionalProperties.put("generateSwagger2Annotations", true);
        }
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        additionalProperties.put(OPT_DATE_FORMAT, dateFormat);
    }

    public void setDateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
        additionalProperties.put(OPT_DATE_TIME_FORMAT, dateTimeFormat);
    }

    public void setUseEnumCaseInsensitive(boolean useEnumCaseInsensitive) {
        this.useEnumCaseInsensitive = useEnumCaseInsensitive;
    }

    public void setAdditionalOneOfTypeAnnotations(List<String> additionalOneOfTypeAnnotations) {
        this.additionalOneOfTypeAnnotations = additionalOneOfTypeAnnotations;
    }

    public void setAdditionalEnumTypeAnnotations(List<String> additionalEnumTypeAnnotations) {
        this.additionalEnumTypeAnnotations = additionalEnumTypeAnnotations;
    }

    @Override
    public void postProcess() {
        // disable output donation suggestion
    }
}
