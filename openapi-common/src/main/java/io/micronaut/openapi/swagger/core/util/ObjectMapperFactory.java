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
package io.micronaut.openapi.swagger.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

import io.micronaut.openapi.swagger.core.jackson.ExampleSerializer;
import io.micronaut.openapi.swagger.core.jackson.MediaTypeSerializer;
import io.micronaut.openapi.swagger.core.jackson.Schema31Serializer;
import io.micronaut.openapi.swagger.core.jackson.SchemaSerializer;
import io.micronaut.openapi.swagger.core.jackson.mixin.Components31Mixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.ComponentsMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.DateSchemaMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.Discriminator31Mixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.DiscriminatorMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.ExampleMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.ExtensionsMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.InfoMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.LicenseMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.MediaTypeMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.OpenAPI31Mixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.OpenAPIMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.OperationMixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.Schema31Mixin;
import io.micronaut.openapi.swagger.core.jackson.mixin.SchemaMixin;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.callbacks.Callback;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.links.Link;
import io.swagger.v3.oas.models.links.LinkParameter;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.Encoding;
import io.swagger.v3.oas.models.media.EncodingProperty;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.XML;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.swagger.v3.oas.models.tags.Tag;

import org.yaml.snakeyaml.LoaderOptions;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactoryBuilder;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * This class is copied from swagger-core library.
 *
 * @since 4.6.0
 */
public class ObjectMapperFactory {

    private ObjectMapperFactory() {
    }

    public static ObjectMapper createJson() {
        return create(null, false);
    }

    public static ObjectMapper createYaml(boolean openapi31) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        YAMLFactory factory = new YAMLFactoryBuilder(new YAMLFactory())
            .loaderOptions(loaderOptions)
            .disable(Feature.WRITE_DOC_START_MARKER)
            .enable(Feature.MINIMIZE_QUOTES)
            .enable(Feature.SPLIT_LINES)
            .enable(Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS)
            .build();

        return create(factory, openapi31);
    }

    public static ObjectMapper createYaml() {
        return createYaml(false);
    }

    public static ObjectMapper createJson31() {
        return create(null, true);
    }

    public static ObjectMapper createYaml31() {
        return createYaml(true);
    }

    @SuppressWarnings("deprecation")
    private static ObjectMapper create(JsonFactory jsonFactory, boolean openapi31) {
        ObjectMapper mapper = jsonFactory == null ? new ObjectMapper() : new ObjectMapper(jsonFactory);

        if (!openapi31) {
            // handle ref schema serialization skipping all other props
            mapper.registerModule(new SimpleModule() {
                @Override
                public void setupModule(SetupContext context) {
                    super.setupModule(context);
                    context.addBeanSerializerModifier(new BeanSerializerModifier() {
                        @Override
                        public JsonSerializer<?> modifySerializer(
                            SerializationConfig config, BeanDescription desc, JsonSerializer<?> serializer) {
                            if (Schema.class.isAssignableFrom(desc.getBeanClass())) {
                                return new SchemaSerializer((JsonSerializer<Object>) serializer);
                            } else if (MediaType.class.isAssignableFrom(desc.getBeanClass())) {
                                return new MediaTypeSerializer((JsonSerializer<Object>) serializer);
                            } else if (Example.class.isAssignableFrom(desc.getBeanClass())) {
                                return new ExampleSerializer((JsonSerializer<Object>) serializer);
                            }
                            return serializer;
                        }
                    });
                }
            });
        } else {
            mapper.registerModule(new SimpleModule() {
                @Override
                public void setupModule(SetupContext context) {
                    super.setupModule(context);
                    context.addBeanSerializerModifier(new BeanSerializerModifier() {
                        @Override
                        public JsonSerializer<?> modifySerializer(
                            SerializationConfig config, BeanDescription desc, JsonSerializer<?> serializer) {
                            if (Schema.class.isAssignableFrom(desc.getBeanClass())) {
                                return new Schema31Serializer((JsonSerializer<Object>) serializer);
                            } else if (MediaType.class.isAssignableFrom(desc.getBeanClass())) {
                                return new MediaTypeSerializer((JsonSerializer<Object>) serializer);
                            } else if (Example.class.isAssignableFrom(desc.getBeanClass())) {
                                return new ExampleSerializer((JsonSerializer<Object>) serializer);
                            }
                            return serializer;
                        }
                    });
                }
            });
        }

        if (!openapi31) {
            Module deserializerModule = new DeserializationModule();
            mapper.registerModule(deserializerModule);
        } else {
            Module deserializerModule = new DeserializationModule31();
            mapper.registerModule(deserializerModule);
        }
        mapper.registerModule(new JavaTimeModule());

        Map<Class<?>, Class<?>> sourceMixins = new LinkedHashMap<>();

        sourceMixins.put(ApiResponses.class, ExtensionsMixin.class);
        sourceMixins.put(Contact.class, ExtensionsMixin.class);
        sourceMixins.put(Encoding.class, ExtensionsMixin.class);
        sourceMixins.put(EncodingProperty.class, ExtensionsMixin.class);
        sourceMixins.put(Example.class, ExampleMixin.class);
        sourceMixins.put(ExternalDocumentation.class, ExtensionsMixin.class);
        sourceMixins.put(Link.class, ExtensionsMixin.class);
        sourceMixins.put(LinkParameter.class, ExtensionsMixin.class);
        sourceMixins.put(MediaType.class, MediaTypeMixin.class);
        sourceMixins.put(OAuthFlow.class, ExtensionsMixin.class);
        sourceMixins.put(OAuthFlows.class, ExtensionsMixin.class);
        sourceMixins.put(Operation.class, OperationMixin.class);
        sourceMixins.put(PathItem.class, ExtensionsMixin.class);
        sourceMixins.put(Paths.class, ExtensionsMixin.class);
        sourceMixins.put(Scopes.class, ExtensionsMixin.class);
        sourceMixins.put(Server.class, ExtensionsMixin.class);
        sourceMixins.put(ServerVariable.class, ExtensionsMixin.class);
        sourceMixins.put(ServerVariables.class, ExtensionsMixin.class);
        sourceMixins.put(Tag.class, ExtensionsMixin.class);
        sourceMixins.put(XML.class, ExtensionsMixin.class);
        sourceMixins.put(ApiResponse.class, ExtensionsMixin.class);
        sourceMixins.put(Parameter.class, ExtensionsMixin.class);
        sourceMixins.put(RequestBody.class, ExtensionsMixin.class);
        sourceMixins.put(Header.class, ExtensionsMixin.class);
        sourceMixins.put(SecurityScheme.class, ExtensionsMixin.class);
        sourceMixins.put(Callback.class, ExtensionsMixin.class);

        if (!openapi31) {
            sourceMixins.put(Schema.class, SchemaMixin.class);
            sourceMixins.put(DateSchema.class, DateSchemaMixin.class);
            sourceMixins.put(Components.class, ComponentsMixin.class);
            sourceMixins.put(Info.class, InfoMixin.class);
            sourceMixins.put(License.class, LicenseMixin.class);
            sourceMixins.put(OpenAPI.class, OpenAPIMixin.class);
            sourceMixins.put(Discriminator.class, DiscriminatorMixin.class);
        } else {
            sourceMixins.put(Info.class, ExtensionsMixin.class);
            sourceMixins.put(Schema.class, Schema31Mixin.class);
            sourceMixins.put(Components.class, Components31Mixin.class);
            sourceMixins.put(OpenAPI.class, OpenAPI31Mixin.class);
            sourceMixins.put(DateSchema.class, DateSchemaMixin.class);
            sourceMixins.put(Discriminator.class, Discriminator31Mixin.class);
        }
        mapper.setMixIns(sourceMixins);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        mapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        mapper.setSerializationInclusion(Include.NON_NULL);

        return mapper;
    }

    @SuppressWarnings("deprecation")
    public static ObjectMapper buildStrictGenericObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
        mapper.setSerializationInclusion(Include.NON_NULL);
        return mapper;
    }

}
