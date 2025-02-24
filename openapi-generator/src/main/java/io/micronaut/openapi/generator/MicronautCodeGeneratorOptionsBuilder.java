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

import java.util.List;
import java.util.Map;

/**
 * Builder for generic options that the Micronaut code generator supports.
 */
@SuppressWarnings("UnusedReturnValue")
public interface MicronautCodeGeneratorOptionsBuilder {

    /**
     * Sets the generator language.
     *
     * @param lang generator language
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withLang(GeneratorLanguage lang);

    /**
     * Sets the package of the generated API classes.
     *
     * @param apiPackage the package name
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withApiPackage(String apiPackage);

    /**
     * Sets the package of the generated invoker classes.
     *
     * @param invokerPackage the package name
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withInvokerPackage(String invokerPackage);

    /**
     * Sets the package of the generated model classes.
     *
     * @param modelPackage the package name
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withModelPackage(String modelPackage);

    /**
     * Sets the artifact id of the project.
     *
     * @param artifactId the artifact id
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withArtifactId(String artifactId);

    /**
     * Add the parameter mappings.
     *
     * @param parameterMappings the parameter mappings specified by a {@link ParameterMapping} objects
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withParameterMappings(List<ParameterMapping> parameterMappings);

    /**
     * Add the response body mappings.
     *
     * @param responseBodyMappings the response body mappings specified by a {@link ResponseBodyMapping} objects
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withResponseBodyMappings(List<ResponseBodyMapping> responseBodyMappings);

    /**
     * Add the schema mappings.
     *
     * @param schemaMapping the schema mappings
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withSchemaMapping(Map<String, String> schemaMapping);

    /**
     * Add the import mappings.
     *
     * @param importMapping the import mappings
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withImportMapping(Map<String, String> importMapping);

    /**
     * Add the name mappings.
     *
     * @param nameMapping the name mappings
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withNameMapping(Map<String, String> nameMapping);

    /**
     * Add the type mappings.
     *
     * @param typeMapping the type mappings
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withTypeMapping(Map<String, String> typeMapping);

    /**
     * Add the enum name mappings.
     *
     * @param enumNameMapping the enum name mappings
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withEnumNameMapping(Map<String, String> enumNameMapping);

    /**
     * Add the model name mappings.
     *
     * @param modelNameMapping the model name mappings
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withModelNameMapping(Map<String, String> modelNameMapping);

    /**
     * Add the inline schema name mappings.
     *
     * @param inlineSchemaNameMapping the inline schema name mappings
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withInlineSchemaNameMapping(Map<String, String> inlineSchemaNameMapping);

    /**
     * Add the inline schema options.
     *
     * @param inlineSchemaOption the inline schema options
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withInlineSchemaOption(Map<String, String> inlineSchemaOption);

    /**
     * Add the OpenAPI normalizer options.
     *
     * @param openapiNormalizer the OpenAPI normalizer options
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withOpenapiNormalizer(Map<String, String> openapiNormalizer);

    /**
     * Set the api name prefix.
     *
     * @param apiNamePrefix the api name prefix
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withApiNamePrefix(String apiNamePrefix);

    /**
     * Set the api name suffix.
     *
     * @param apiNameSuffix the api name suffix
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withApiNameSuffix(String apiNameSuffix);

    /**
     * Set the model name prefix.
     *
     * @param modelNamePrefix the model name prefix
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withModelNamePrefix(String modelNamePrefix);

    /**
     * Set the model name suffix.
     *
     * @param modelNameSuffix the model name suffix
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withModelNameSuffix(String modelNameSuffix);

    /**
     * Set the implicit headers flag.
     *
     * @param implicitHeaders implicit headers
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withImplicitHeaders(boolean implicitHeaders);

    /**
     * Set the implicit headers regex.
     *
     * @param implicitHeadersRegex implicit headers regex
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withImplicitHeadersRegex(String implicitHeadersRegex);

    /**
     * If set to true, the generator will use reactive types.
     *
     * @param reactive the reactive flag
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withReactive(boolean reactive);

    /**
     * If true, the generated operation return types will be wrapped in HttpResponse.
     *
     * @param generateHttpResponseAlways the wrapping flag
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withGenerateHttpResponseAlways(boolean generateHttpResponseAlways);

    /**
     * Wrap the operations response in HttpResponse object where non-200 HTTP status codes or additional headers are defined.
     *
     * @param generateHttpResponseWhereRequired the wrapping flag
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withGenerateHttpResponseWhereRequired(boolean generateHttpResponseWhereRequired);

    /**
     * If set to true, controller and client method will be generated with openAPI annotations.
     *
     * @param generateSwaggerAnnotations the generate swagger annotations flag
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withGenerateSwaggerAnnotations(boolean generateSwaggerAnnotations);

    /**
     * If set to true, the generated code will use bean validation.
     *
     * @param beanValidation the bean validation flag
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withBeanValidation(boolean beanValidation);

    /**
     * If set to true, the generated enums check enum value with ignoring case.
     *
     * @param useEnumCaseInsensitive the useEnumCaseInsensitive flag
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withUseEnumCaseInsensitive(boolean useEnumCaseInsensitive);

    /**
     * If set to true, the generated code will make use of {@link java.util.Optional}.
     *
     * @param optional the optional flag
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withOptional(boolean optional);

    /**
     * Configures the test framework to use for generated tests.
     *
     * @param testFramework the test framework
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withTestFramework(MicronautCodeGeneratorEntryPoint.TestFramework testFramework);

    /**
     * Configure the serialization library.
     *
     * @param library the serialization library.
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withSerializationLibrary(SerializationLibraryKind library);

    /**
     * Configure the date-time format.
     *
     * @param format the date-time format.
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withDateTimeFormat(DateTimeFormat format);

    /**
     * Flag to indicate whether to use the utils.OneOfImplementorAdditionalData related logic.
     *
     * @param useOneOfInterfaces if true, then use the utils.OneOfImplementorAdditionalData related logic.
     *
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withUseOneOfInterfaces(boolean useOneOfInterfaces);

    /**
     * Additional annotations for enum type (class level annotations).
     *
     * @param additionalEnumTypeAnnotations additional annotations for enum type (class level annotations).
     *
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withAdditionalEnumTypeAnnotations(List<String> additionalEnumTypeAnnotations);

    /**
     * Additional annotations for model type (class level annotations).
     *
     * @param additionalModelTypeAnnotations additional annotations for model type (class level annotations).
     *
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withAdditionalModelTypeAnnotations(List<String> additionalModelTypeAnnotations);

    /**
     * Additional annotations for oneOf interfaces (class level annotations).
     *
     * @param additionalOneOfTypeAnnotations additional annotations for oneOf interfaces (class level annotations).
     *
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withAdditionalOneOfTypeAnnotations(List<String> additionalOneOfTypeAnnotations);

    /**
     * Additional generator properties.
     *
     * @param additionalProperties additional generator properties.
     *
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withAdditionalProperties(Map<String, Object> additionalProperties);

    /**
     * Flag to indicate whether to use the "jakarta" or "javax" package.
     *
     * @param useJakartaEe if true, then use the "jakarta" package, otherwise - "javax".
     *
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withUseJakartaEe(boolean useJakartaEe);

    /**
     * Sort method arguments to place required parameters before optional parameters.
     * Default: true
     *
     * @param sortParamsByRequiredFlag Sort method arguments to place required parameters before optional parameters
     *
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withSortParamsByRequiredFlag(boolean sortParamsByRequiredFlag);

    /**
     * Skip examples defined in operations to avoid out of memory errors.
     * Default: false
     *
     * @param skipOperationExample Skip examples defined in operations to avoid out of memory errors.
     *
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withSkipOperationExample(boolean skipOperationExample);

    /**
     * Skip sorting operations.
     * Default: false
     *
     * @param skipSortingOperations Skip sorting operations
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withSkipSortingOperations(boolean skipSortingOperations);

    /**
     * Character to use as a delimiter for the prefix. Default: '_'
     *
     * @param removeOperationIdPrefixDelimiter Character to use as a delimiter for the prefix. Default: '_'
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withRemoveOperationIdPrefixDelimiter(String removeOperationIdPrefixDelimiter);

    /**
     * Count of delimiter for the prefix. Use -1 for last Default: 1
     *
     * @param removeOperationIdPrefixCount Count of delimiter for the prefix. Use -1 for last Default: 1
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withRemoveOperationIdPrefixCount(int removeOperationIdPrefixCount);

    /**
     * Sort model properties to place required parameters before optional parameters.
     *
     * @param sortModelPropertiesByRequiredFlag Sort model properties to place required parameters before optional parameters.
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withSortModelPropertiesByRequiredFlag(boolean sortModelPropertiesByRequiredFlag);

    /**
     * Whether to ensure parameter names are unique in an operation (rename parameters that are not).
     *
     * @param ensureUniqueParams Whether to ensure parameter names are unique in an operation (rename parameters that are not).
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withEnsureUniqueParams(boolean ensureUniqueParams);

    /**
     * boolean, toggles whether Unicode identifiers are allowed in names or not, default is false.
     *
     * @param allowUnicodeIdentifiers toggles whether Unicode identifiers are allowed in names or not, default is false
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withAllowUnicodeIdentifiers(boolean allowUnicodeIdentifiers);

    /**
     * Add form or body parameters to the beginning of the parameter list.
     *
     * @param prependFormOrBodyParameters Add form or body parameters to the beginning of the parameter list.
     * @return this builder
     */
    MicronautCodeGeneratorOptionsBuilder withPrependFormOrBodyParameters(boolean prependFormOrBodyParameters);

    /**
     * The possible date-time formatting configurations.
     */
    enum DateTimeFormat {
        OFFSET_DATETIME,
        ZONED_DATETIME,
        LOCAL_DATETIME
    }

    /**
     * The possible languages for generator.
     */
    enum GeneratorLanguage {
        JAVA, KOTLIN,
    }
}
