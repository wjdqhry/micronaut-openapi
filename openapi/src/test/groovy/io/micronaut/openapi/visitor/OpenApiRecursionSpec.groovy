package io.micronaut.openapi.visitor

import io.micronaut.openapi.AbstractOpenApiTypeElementSpec
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema

class OpenApiRecursionSpec extends AbstractOpenApiTypeElementSpec {

    void "test OpenAPI handles recursion"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import io.micronaut.http.annotation.*;
import io.reactivex.Maybe;

@Controller("/")
class MyController {
    @Get
    public Maybe<TestInterface> hey() {
        return Maybe.empty();
    }
}

@Schema(oneOf = {TestImpl1.class, TestImpl2.class})
interface TestInterface {
    String getType();
}

class TestImpl1 implements TestInterface {

    private TestInterface woopsie;

    @Override
    public String getType() {
        return null;
    }

    public TestInterface getWoopsie() {
        return woopsie;
    }

    public void setWoopsie(TestInterface woopsie) {
        this.woopsie = woopsie;
    }
}

class TestImpl2 implements TestInterface {
    @Override
    public String getType() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
        Schema testImpl1 = schemas.get("TestImpl1")
        Schema woopsieRef = testImpl1.getProperties()["woopsie"]
        woopsieRef
        woopsieRef.$ref == "#/components/schemas/TestInterface"
    }

    void "test OpenAPI handles recursion when no @Schema annotation"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import io.micronaut.http.annotation.*;

@Controller("/")
class MyController {
    @Get
    public TestRecursion hey() {
        return null;
    }
}

class TestRecursion {

    private TestRecursion woopsie;

    public TestRecursion getWoopsie() {
        return woopsie;
    }

    public void setWoopsie(TestRecursion woopsie) {
        this.woopsie = woopsie;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
        Schema testRecursion = schemas.get("TestRecursion")
        Schema woopsieRef = testRecursion.getProperties()["woopsie"]
        woopsieRef
        woopsieRef.$ref == "#/components/schemas/TestRecursion"
    }

    void "test OpenAPI handles recursion when recursive item has different name"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import io.micronaut.http.annotation.*;
import io.reactivex.Maybe;

@Controller("/")
class MyController {
    @Get
    public Maybe<TestInterface> hey() {
        return Maybe.empty();
    }
}

@Schema(oneOf = {TestImpl1.class, TestImpl2.class})
interface TestInterface {
    String getType();
}

class TestImpl1 implements TestInterface {

    private TestInterface woopsie;

    @Override
    public String getType() {
        return null;
    }

    @Schema(name = "woopsie-id", description = "woopsie doopsie", oneOf = {TestImpl1.class, TestImpl2.class})
    public TestInterface getWoopsie() {
        return woopsie;
    }

    public void setWoopsie(TestInterface woopsie) {
        this.woopsie = woopsie;
    }
}

class TestImpl2 implements TestInterface {
    @Override
    public String getType() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
        Schema testImpl1 = schemas.TestImpl1
        Schema woopsieRef = testImpl1.properties."woopsie-id"

        woopsieRef.description == "woopsie doopsie"
        woopsieRef.allOf.size() == 1
        woopsieRef.allOf[0].$ref == "#/components/schemas/TestInterface"
        Schema woopsie = schemas.TestInterface
        woopsie
        !woopsie.description
    }

    void "test OpenAPI applies additional annotations to recursive property"() {
        given:
        buildBeanDefinition('test.MyBean', '''

package test;

import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.*;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.*;
import io.reactivex.Maybe;

@Controller("/")
class MyController {
    @Get
    public Maybe<TestInterface> hey() {
        return Maybe.empty();
    }
}

@Schema(oneOf = {TestImpl1.class, TestImpl2.class})
interface TestInterface {
    String getType();
}

class TestImpl1 implements TestInterface {

    private TestInterface woopsie;

    @Override
    public String getType() {
        return null;
    }

    /**
     * Some docs
     */
    @Nullable
    @Deprecated
    public TestInterface getWoopsie() {
        return woopsie;
    }

    public void setWoopsie(TestInterface woopsie) {
        this.woopsie = woopsie;
    }
}

class TestImpl2 implements TestInterface {
    @Override
    public String getType() {
        return null;
    }
}

@jakarta.inject.Singleton
class MyBean {}
''')

        OpenAPI openAPI = Utils.testReference
        Map<String, Schema> schemas = openAPI.getComponents().getSchemas()

        expect:
        Schema testImpl1 = schemas.get("TestImpl1")
        Schema woopsieProperty = testImpl1.getProperties()["woopsie"]
        woopsieProperty instanceof ComposedSchema
        ((ComposedSchema) woopsieProperty).deprecated
        ((ComposedSchema) woopsieProperty).description == "Some docs"
        ((ComposedSchema) woopsieProperty).nullable
        ((ComposedSchema) woopsieProperty).allOf[0].$ref == "#/components/schemas/TestInterface"
    }

}
