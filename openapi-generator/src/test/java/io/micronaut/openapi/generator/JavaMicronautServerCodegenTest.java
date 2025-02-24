package io.micronaut.openapi.generator;

import io.micronaut.openapi.generator.assertions.TestUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;

import static java.util.stream.Collectors.groupingBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class JavaMicronautServerCodegenTest extends AbstractMicronautCodegenTest {

    static String ROLES_EXTENSION_TEST_PATH = "src/test/resources/3_0/micronaut/roles-extension-test.yaml";
    static String MULTI_TAGS_TEST_PATH = "src/test/resources/3_0/micronaut/multi-tags-test.yaml";

    @Test
    void clientOptsUnicity() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.cliOptions()
            .stream()
            .collect(groupingBy(CliOption::getOpt))
            .forEach((k, v) -> assertEquals(1, v.size(), k + " is described multiple times"));
    }

    @Test
    void testInitialConfigValues() {
        final var codegen = new JavaMicronautServerCodegen();
        codegen.processOpts();

        var openAPI = new OpenAPI();
        openAPI.addServersItem(new Server().url("https://one.com/v2"));
        openAPI.setInfo(new Info());
        codegen.preprocessOpenAPI(openAPI);

        assertEquals(Boolean.FALSE, codegen.additionalProperties().get(CodegenConstants.HIDE_GENERATION_TIMESTAMP));
        assertFalse(codegen.isHideGenerationTimestamp());
        assertEquals("org.openapitools.model", codegen.modelPackage());
        assertEquals("org.openapitools.model", codegen.additionalProperties().get(CodegenConstants.MODEL_PACKAGE));
        assertEquals("org.openapitools.api", codegen.apiPackage());
        assertEquals("org.openapitools.api", codegen.additionalProperties().get(CodegenConstants.API_PACKAGE));
        assertEquals("org.openapitools.controller", codegen.additionalProperties().get(JavaMicronautServerCodegen.OPT_CONTROLLER_PACKAGE));
        assertEquals("org.openapitools", codegen.getInvokerPackage());
        assertEquals("org.openapitools", codegen.additionalProperties().get(CodegenConstants.INVOKER_PACKAGE));
    }

    @Test
    void testApiAndModelFilesPresent() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(CodegenConstants.INVOKER_PACKAGE, "org.test.test");
        codegen.additionalProperties().put(CodegenConstants.MODEL_PACKAGE, "org.test.test.model");
        codegen.additionalProperties().put(CodegenConstants.API_PACKAGE, "org.test.test.api");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.APIS,
            CodegenConstants.MODELS);

        String invokerFolder = outputPath + "src/main/java/org/test/test/";
        assertFileExists(invokerFolder + "Application.java");

        String apiFolder = outputPath + "src/main/java/org/test/test/api/";
        assertFileExists(apiFolder + "PetApi.java");
        assertFileExists(apiFolder + "StoreApi.java");
        assertFileExists(apiFolder + "UserApi.java");

        String modelFolder = outputPath + "src/main/java/org/test/test/model/";
        assertFileExists(modelFolder + "Pet.java");
        assertFileExists(modelFolder + "User.java");
        assertFileExists(modelFolder + "Order.java");

        String resources = outputPath + "src/main/resources/";
        assertFileExists(resources + "application.yml");
    }

    @Test
    void doUseValidationParam() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.USE_BEANVALIDATION, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.APIS);

        // Files are not generated
        String apiFolder = outputPath + "/src/main/java/org/openapitools/api/";
        assertFileContains(apiFolder + "PetApi.java", "@Valid");
        assertFileContains(apiFolder + "PetApi.java", "@NotNull");
    }

    @Test
    void doNotUseValidationParam() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.USE_BEANVALIDATION, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.APIS);

        // Files are not generated
        String apiFolder = outputPath + "/src/main/java/org/openapitools/api/";
        assertFileNotContains(apiFolder + "PetApi.java", "@Valid");
        assertFileNotContains(apiFolder + "PetApi.java", "@NotNull");
    }

    @Test
    void doGenerateForTestJUnit() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_TEST,
            JavaMicronautServerCodegen.OPT_TEST_JUNIT);
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.API_TESTS, CodegenConstants.APIS, CodegenConstants.MODELS);

        // Files are not generated
        assertFileExists(outputPath + "src/test/java/");
        String apiTestFolder = outputPath + "src/test/java/org/openapitools/api/";
        assertFileExists(apiTestFolder + "PetApiTest.java");
        assertFileContains(apiTestFolder + "PetApiTest.java", "PetApiTest", "@MicronautTest");
    }

    @Test
    void doGenerateForTestSpock() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_TEST,
            JavaMicronautServerCodegen.OPT_TEST_SPOCK);
        String outputPath = generateFiles(codegen, PETSTORE_PATH,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.API_TESTS, CodegenConstants.APIS, CodegenConstants.MODELS);

        // Files are not generated
        assertFileExists(outputPath + "src/test/groovy");
        String apiTestFolder = outputPath + "src/test/groovy/org/openapitools/api/";
        assertFileExists(apiTestFolder + "PetApiSpec.groovy");
        assertFileContains(apiTestFolder + "PetApiSpec.groovy", "PetApiSpec", "@MicronautTest");
    }

    @Test
    void doGenerateRequiredPropertiesInConstructor() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "true");
        codegen.additionalProperties().put(CodegenConstants.SERIALIZATION_LIBRARY, SerializationLibraryKind.JACKSON.name());
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        // Constructor should have properties
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";
        assertFileContains(modelPath + "Pet.java", "public Pet(String name, List<@NotNull String> photoUrls)");
        assertFileContains(modelPath + "Pet.java", "private Pet()");
    }

    @Test
    void doNotGenerateRequiredPropertiesInConstructor() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REQUIRED_PROPERTIES_IN_CONSTRUCTOR, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        // Constructor should have properties
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";
        assertFileNotContainsRegex(modelPath + "Pet.java", "public Pet\\([^)]+\\)");
        assertFileNotContains(modelPath + "Pet.java", "private Pet()");
        assertFileNotContainsRegex(modelPath + "User.java", "public User\\([^)]+\\)");
        assertFileNotContains(modelPath + "User.java", "private User()");
        assertFileNotContainsRegex(modelPath + "Order.java", "public Order\\([^)]+\\)");
        assertFileNotContains(modelPath + "Order.java", "private Order()");
    }

    @Test
    void testExtraAnnotations1() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/issue_11772.yml", CodegenConstants.MODELS);

        TestUtils.assertExtraAnnotationFiles(outputPath + "/src/main/java/org/openapitools/model");
    }

    @Test
    void doNotGenerateAuthRolesWithExtensionWhenNotUseAuth() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_USE_AUTH, false);
        String outputPath = generateFiles(codegen, ROLES_EXTENSION_TEST_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileNotContains(apiPath + "BooksApi.java", "@Secured");
        assertFileNotContains(apiPath + "UsersApi.java", "@Secured");
        assertFileNotContains(apiPath + "ReviewsApi.java", "@Secured");
    }

    @Test
    void generateAuthRolesWithExtension() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_USE_AUTH, true);
        String outputPath = generateFiles(codegen, ROLES_EXTENSION_TEST_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContainsRegex(apiPath + "BooksApi.java", "IS_ANONYMOUS[^;]{0,100}bookSearchGet");
        assertFileContainsRegex(apiPath + "BooksApi.java", "@Secured\\(\"admin\"\\)[^;]{0,100}createBook");
        assertFileContainsRegex(apiPath + "BooksApi.java", "IS_ANONYMOUS[^;]{0,100}getBook");
        assertFileContainsRegex(apiPath + "BooksApi.java", "IS_AUTHENTICATED[^;]{0,100}reserveBook");

        assertFileContainsRegex(apiPath + "ReviewsApi.java", "IS_AUTHENTICATED[^;]{0,100}bookSendReviewPost");
        assertFileContainsRegex(apiPath + "ReviewsApi.java", "IS_ANONYMOUS[^;]{0,100}bookViewReviewsGet");

        assertFileContainsRegex(apiPath + "UsersApi.java", "IS_ANONYMOUS[^;]{0,100}getUserProfile");
        assertFileContainsRegex(apiPath + "UsersApi.java", "IS_AUTHENTICATED[^;]{0,100}updateProfile");
    }

    @Test
    void doGenerateMonoWrapHttpResponse() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.java", "Mono<HttpResponse<@Valid Pet>>");
    }

    @Test
    void doGenerateMono() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_FLUX_FOR_ARRAYS, "false");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.java", "Mono<@Valid Pet>");
        assertFileNotContains(apiPath + "PetApi.java", "Flux<@Valid Pet>");
        assertFileNotContains(apiPath + "PetApi.java", "HttpResponse");
    }

    @Test
    void doGenerateMonoAndFlux() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "true");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_FLUX_FOR_ARRAYS, "true");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.java", "Mono<@Valid Pet>");
        assertFileContains(apiPath + "PetApi.java", "Flux<@Valid Pet>");
        assertFileNotContains(apiPath + "PetApi.java", "HttpResponse");
    }

    @Test
    void doGenerateWrapHttpResponse() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "false");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "true");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.java", "HttpResponse<@Valid Pet>");
        assertFileNotContains(apiPath + "PetApi.java", "Mono");
    }

    @Test
    void doGenerateNoMonoNoWrapHttpResponse() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_REACTIVE, "false");
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_HTTP_RESPONSE_ALWAYS, "false");
        String outputPath = generateFiles(codegen, PETSTORE_PATH, CodegenConstants.MODELS, CodegenConstants.APIS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        assertFileContains(apiPath + "PetApi.java", "Pet");
        assertFileNotContains(apiPath + "PetApi.java", "Mono");
        assertFileNotContains(apiPath + "PetApi.java", "HttpResponse");
    }

    @Test
    void doGenerateOperationOnlyForFirstTag() {
        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, MULTI_TAGS_TEST_PATH, CodegenConstants.MODELS,
            CodegenConstants.APIS, CodegenConstants.API_TESTS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        String apiTestPath = outputPath + "/src/test/java/org/openapitools/api/";

        // Verify files are generated only for the required tags
        assertFileExists(apiPath + "AuthorsApi.java");
        assertFileExists(apiPath + "BooksApi.java");
        assertFileNotExists(apiPath + "SearchApi.java");

        // Verify the same for test files
        assertFileExists(apiTestPath + "AuthorsApiTest.java");
        assertFileExists(apiTestPath + "BooksApiTest.java");
        assertFileNotExists(apiTestPath + "SearchApiTest.java");

        // Verify all the methods are generated only ones
        assertFileContains(apiPath + "AuthorsApi.java",
            "authorSearchGet", "getAuthor", "getAuthorBooks");
        assertFileContains(apiPath + "BooksApi.java",
            "bookCreateEntryPost", "bookSearchGet", "bookSendReviewPost", "getBook", "isBookAvailable");
        assertFileNotContains(apiPath + "BooksApi.java", "getAuthorBooks");
    }

    @Test
    void doRepeatOperationForAllTags() {
        var codegen = new JavaMicronautServerCodegen();
        codegen.additionalProperties().put(JavaMicronautServerCodegen.OPT_GENERATE_OPERATION_ONLY_FOR_FIRST_TAG, "false");
        String outputPath = generateFiles(codegen, MULTI_TAGS_TEST_PATH, CodegenConstants.MODELS,
            CodegenConstants.APIS, CodegenConstants.API_TESTS);

        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        String apiTestPath = outputPath + "/src/test/java/org/openapitools/api/";

        // Verify all the tags created
        assertFileExists(apiPath + "AuthorsApi.java");
        assertFileExists(apiPath + "BooksApi.java");
        assertFileExists(apiPath + "SearchApi.java");

        // Verify the same for test files
        assertFileExists(apiTestPath + "AuthorsApiTest.java");
        assertFileExists(apiTestPath + "BooksApiTest.java");
        assertFileExists(apiTestPath + "SearchApiTest.java");

        // Verify all the methods are repeated for each of the tags
        assertFileContains(apiPath + "AuthorsApi.java",
            "authorSearchGet", "getAuthor", "getAuthorBooks");
        assertFileContains(apiPath + "BooksApi.java",
            "bookCreateEntryPost", "bookSearchGet", "bookSendReviewPost", "getBook", "isBookAvailable", "getAuthorBooks");
        assertFileContains(apiPath + "SearchApi.java",
            "authorSearchGet", "bookSearchGet");
    }

    @Test
    void testReadOnlyConstructorBug() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/readonlyconstructorbug.yml", CodegenConstants.MODELS);
        String apiPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(apiPath + "BookInfo.java", "public BookInfo(String name, String requiredReadOnly)");
        assertFileContains(apiPath + "ExtendedBookInfo.java", "public ExtendedBookInfo(String isbn, String name, String requiredReadOnly)", "super(name, requiredReadOnly)");
    }

    @Test
    void testDiscriminatorConstructorBug() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/discriminatorconstructorbug.yml", CodegenConstants.MODELS);
        String apiPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(apiPath + "BookInfo.java", "public BookInfo(String name)");
        assertFileContains(apiPath + "BasicBookInfo.java", "public BasicBookInfo(String author, String name)", "super(name)");
        assertFileContains(apiPath + "DetailedBookInfo.java", "public DetailedBookInfo(String isbn, String name, String author)", "super(author, name)");
    }

    @Test
    void testGenericAnnotations() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/modelwithprimitivelist.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileContains(apiPath + "BooksApi.java", "@Body @NotNull List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String> requestBody");
        assertFileContains(modelPath + "CountsContainer.java", "private List<@NotEmpty List<@NotNull List<@Size(max = 10) @NotNull ZonedDateTime>>> counts;");
        assertFileContains(modelPath + "BooksContainer.java", "private List<@Pattern(regexp = \"[a-zA-Z ]+\") @Size(max = 10) @NotNull String> books;");
    }

    @Test
    void testPluralBodyParamName() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/plural.yml", CodegenConstants.APIS);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(apiPath + "DefaultApi.java", "@Body @NotNull List<@Valid Book> books");
    }

    @Test
    void testControllerEnums() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/controller-enum.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String modelPath = outputPath + "src/main/java/org/openapitools/model/";

        assertFileExists(modelPath + "GetTokenRequestGrantType.java");
        assertFileExists(modelPath + "GetTokenRequestClientSecret.java");
        assertFileExists(modelPath + "GetTokenRequestClientId.java");
        assertFileExists(modelPath + "ArtistsArtistIdDirectAlbumsGetSortByParameter.java");
    }

    @Test
    void testFileEndpoint() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/file.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String apiPath = outputPath + "src/main/java/org/openapitools/api/";

        assertFileContains(apiPath + "RequestBodyApi.java", "@Nullable(inherited = true) CompletedFileUpload file");
    }

    @Test
    void testReservedWords() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/javaReservedWords.yml",
            CodegenConstants.APIS,
            CodegenConstants.MODELS,
            CodegenConstants.SUPPORTING_FILES,
            CodegenConstants.MODEL_TESTS,
            CodegenConstants.MODEL_DOCS,
            CodegenConstants.API_TESTS,
            CodegenConstants.API_DOCS
        );
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/ParametersApi.java", "Mono<Void> callInterface(",
            "@QueryValue(\"class\") @NotNull @Valid Package propertyClass,",
            "@QueryValue(\"while\") @NotNull String _while");
        assertFileContains(path + "model/Package.java",
            "public static final String JSON_PROPERTY_FOR = \"for\";",
            "@JsonProperty(JSON_PROPERTY_FOR)",
            "private String _for;",
            "public String get_for() {",
            "public void set_for(String _for) {");
        assertFileContains(path + "controller/ParametersController.java",
            "public Mono<Void> callInterface(Package propertyClass, String _while) {");
    }

    @Test
    void testCommonPathParametersWithRef() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/openmeteo.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/WeatherForecastApisApi.java", "@Get(\"/v1/forecast/{id}\")",
            "@PathVariable(\"id\") @NotNull String id,",
            "@QueryValue(\"hourly\") @Nullable(inherited = true) List<V1ForecastIdGetHourlyParameterInner> hourly,");

        assertFileContains(path + "model/V1ForecastIdGetHourlyParameterInner.java",
            "public enum V1ForecastIdGetHourlyParameterInner {",
            "@JsonProperty(\"temperature_2m\")",
            "TEMPERATURE_2M(\"temperature_2m\"),");
    }

    @Test
    void testResponseRef() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/spec.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/ResponseBodyApi.java", "@ApiResponse(responseCode = \"default\", description = \"An unexpected error has occurred\")");
    }

    @Test
    void testExtraAnnotations() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/extra-annotations.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/BooksApi.java",
            """
                    @Post("/add-book")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    @NotBlank
                    Mono<@Valid Book> addBook(
                """);

        assertFileContains(path + "model/Book.java",
            """
                @Serializable
                public class Book {
                """,
            """
                    @NotNull
                    @Size(max = 10)
                    @Schema(name = "title", requiredMode = Schema.RequiredMode.REQUIRED)
                    @JsonProperty(JSON_PROPERTY_TITLE)
                    @jakarta.validation.constraints.NotBlank
                    private String title;
                """,
            """
                    @NotEmpty
                    public void setTitle(String title) {
                        this.title = title;
                    }
                """
        );
    }

    @Test
    void testOperationDescription() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/operation-with-desc.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DatasetsApi.java", "description = \"Creates a brand new dataset.\"");
    }

    @Test
    void testSecurity() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/security.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.java",
            """
                    @Secured({"read", "admin"})
                    Mono<Void> get();
                """,
            """
                    @Secured({"write", "admin"})
                    Mono<Void> save();
                """);
    }

    @Test
    void testMultipartFormData() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multipartdata.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/ResetPasswordApi.java", """
                @Consumes("multipart/form-data")
                @Secured(SecurityRule.IS_ANONYMOUS)
                Mono<@Valid SuccessResetPassword> profilePasswordPost(
                    @Header("WCToken") @NotNull String wcToken,
                    @Header("WCTrustedToken") @NotNull String wcTrustedToken,
                    @Part("name") @Nullable(inherited = true) String name,
                    @Part("file") @Nullable(inherited = true) CompletedFileUpload file
                );
            """);
    }

    @Test
    void testMultipleContentTypesEndpoints() {

        var codegen = new JavaMicronautServerCodegen();
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/multiple-content-types.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/DefaultApi.java", """
                    @Post("/multiplecontentpath")
                    @Consumes({"application/json", "application/xml"})
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<HttpResponse<Void>> myOp(
                        @Body @Nullable(inherited = true) @Valid Coordinates coordinates
                    );
                """,
            """
                    @Post("/multiplecontentpath")
                    @Consumes("multipart/form-data")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<HttpResponse<Void>> myOp_1(
                        @Nullable(inherited = true) @Valid Coordinates coordinates,
                        @Nullable(inherited = true) CompletedFileUpload file
                    );
                """,
            """
                    @Post("/multiplecontentpath")
                    @Consumes({"application/yaml", "text/json"})
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<HttpResponse<Void>> myOp_2(
                        @Body @Nullable(inherited = true) @Valid MySchema mySchema
                    );
                """);
    }

    @Test
    void testPolymorphism() {

        var codegen = new JavaMicronautServerCodegen();
        codegen.setUseAuth(false);
        codegen.setReactive(false);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/1794/openapi.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "model/CurrencyInvoiceCreateDto.java", """
                    @Override
                    public CurrencyInvoiceCreateDto docType(DocType docType) {
                        super.setDocType(docType);
                        return this;
                    }
                """,
            """
                    @Override
                    public CurrencyInvoiceCreateDto sellerVatId(String sellerVatId) {
                        super.setSellerVatId(sellerVatId);
                        return this;
                    }
                """
        );
        assertFileNotContains(path + "model/BaseInvoiceDto.java", """
                    this.docType = docType;
                    this.sellerVatId = sellerVatId;
            """
        );
    }

    @Test
    void testDeprecated() {

        var codegen = new JavaMicronautServerCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/deprecated.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/ParametersApi.java",
            """
                    /**
                     * A method to send primitives as request parameters
                     *
                     * @param name (required)
                     *        Deprecated: Deprecated message2
                     * @param age (required)
                     * @param height (required)
                     *        Deprecated: Deprecated message4
                     * @return Success (status code 200)
                     *         or An unexpected error has occurred (status code default)
                     * @deprecated Deprecated message1
                     */
                    @Deprecated
                    @Operation(
                        operationId = "sendPrimitives",
                        description = "A method to send primitives as request parameters",
                        deprecated = true,
                        responses = {
                            @ApiResponse(responseCode = "200", description = "Success", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SendPrimitivesResponse.class))),
                            @ApiResponse(responseCode = "default", description = "An unexpected error has occurred")
                        },
                        parameters = {
                            @Parameter(name = "name", deprecated = true, required = true, in = ParameterIn.PATH),
                            @Parameter(name = "age", required = true, in = ParameterIn.QUERY),
                            @Parameter(name = "height", deprecated = true, required = true, in = ParameterIn.HEADER)
                        }
                    )
                    @Get("/sendPrimitives/{name}")
                    @Secured(SecurityRule.IS_ANONYMOUS)
                    Mono<@Valid SendPrimitivesResponse> sendPrimitives(
                        @PathVariable("name") @NotNull @Deprecated String name,
                        @QueryValue("age") @NotNull BigDecimal age,
                        @Header("height") @NotNull @Deprecated Float height
                    );
                """);
    }

    @Test
    void testCustomValidationMessages() {

        var codegen = new JavaMicronautServerCodegen();
        codegen.setUseEnumCaseInsensitive(true);
        String outputPath = generateFiles(codegen, "src/test/resources/3_0/validation-messages.yml", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/BooksApi.java",
            """
                @QueryValue("emailParam") @NotNull List<@Email(regexp = "email@dot.com", message = "This is email pattern message") @Size(min = 5, max = 10, message = "This is min max email length message") @NotNull(message = "This is required email message") String> emailParam,
                """,
            """
                @QueryValue("strParam") @NotNull List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(min = 5, max = 10, message = "This is min max string length message") @NotNull(message = "This is required string message") String> strParam,
                """,
            """
                @QueryValue("strParam2") @NotNull List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(min = 5, message = "This is min max string length message") @NotNull(message = "This is required string message") String> strParam2,
                """,
            """
                @QueryValue("strParam3") @NotNull List<@Pattern(regexp = "my_pattern", message = "This is string pattern message") @Size(max = 10, message = "This is min max string length message") @NotNull(message = "This is required string message") String> strParam3,
                """,
            """
                @QueryValue("intParam") @NotNull List<@NotNull(message = "This is required int message") @Min(value = 5, message = "This is min message") @Max(value = 10, message = "This is max message") Integer> intParam,
                """,
            """
                @QueryValue("decimalParam") @NotNull List<@NotNull(message = "This is required decimal message") @DecimalMin(value = "5.5", message = "This is decimal min message") @DecimalMax(value = "10.5", message = "This is decimal max message") BigDecimal> decimalParam,
                """,
            """
                    @QueryValue("decimalParam2") @NotNull(message = "This is required param message") List<@NotNull(message = "This is required decimal message") @DecimalMin(value = "5.5", inclusive = false, message = "This is decimal min message") @DecimalMax(value = "10.5", inclusive = false, message = "This is decimal max message") BigDecimal> decimalParam2,
                """,
            """
                @QueryValue("positiveParam") @NotNull List<@NotNull(message = "This is required int message") @Positive(message = "This is positive message") Integer> positiveParam,
                """,
            """
                @QueryValue("positiveOrZeroParam") @NotNull List<@NotNull(message = "This is required int message") @PositiveOrZero(message = "This is positive or zero message") Integer> positiveOrZeroParam,
                """,
            """
                @QueryValue("negativeParam") @NotNull List<@NotNull(message = "This is required int message") @Negative(message = "This is negative message") Integer> negativeParam,
                """,
            """
                @QueryValue("negativeOrZeroParam") @NotNull List<@NotNull(message = "This is required int message") @NegativeOrZero(message = "This is negative or zero message") Integer> negativeOrZeroParam,
                """);
    }

    @Test
    void testSwaggerAnnotations() {

        var codegen = new JavaMicronautServerCodegen();
        codegen.setGenerateSwaggerAnnotations(true);
        String outputPath = generateFiles(codegen, "src/test/resources/petstore.json", CodegenConstants.APIS, CodegenConstants.MODELS);
        String path = outputPath + "src/main/java/org/openapitools/";

        assertFileContains(path + "api/PetApi.java",
            """
                    @Operation(
                        operationId = "findPetsByStatus",
                        summary = "Finds Pets by status",
                        description = "Multiple status values can be provided with comma separated strings",
                        responses = {
                            @ApiResponse(responseCode = "200", description = "successful operation", content = {
                                @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Pet.class))),
                                @Content(mediaType = "application/xml", array = @ArraySchema(schema = @Schema(implementation = Pet.class)))
                            }),
                            @ApiResponse(responseCode = "400", description = "Invalid status value")
                        },
                        parameters = @Parameter(name = "status", description = "Status values that need to be considered for filter", in = ParameterIn.QUERY),
                        security = @SecurityRequirement(name = "petstore_auth", scopes = {"write:pets", "read:pets"})
                    )
                    @Get("/pet/findByStatus")
                    @Produces({"application/json", "application/xml"})
                    @Secured({"write:pets", "read:pets"})
                    Mono<@NotNull List<@Valid Pet>> findPetsByStatus(
                        @QueryValue(value = "status", defaultValue = "[\\"available\\"]") @Nullable(inherited = true) List<@NotNull String> status
                    );
                """);
    }
}
