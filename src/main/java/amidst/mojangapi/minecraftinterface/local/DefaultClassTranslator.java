package amidst.mojangapi.minecraftinterface.local;

import static amidst.mojangapi.minecraftinterface.local.SymbolicNames.*;

import amidst.clazz.real.AccessFlags;
import amidst.clazz.translator.ClassTranslator;

public enum DefaultClassTranslator {
    INSTANCE;

    private final ClassTranslator classTranslator = createClassTranslator();

    public static ClassTranslator get() {
        return INSTANCE.classTranslator;
    }

    // @formatter:off
    private ClassTranslator createClassTranslator() {
        return ClassTranslator
            .builder()
                .ifDetect(c ->
                        (c.getNumberOfConstructors() == 3
                        && c.getNumberOfFields() == 3
                        && c.getField(0).hasFlags(AccessFlags.PRIVATE | AccessFlags.STATIC | AccessFlags.FINAL)
                        && c.searchForUtf8EqualTo("argument.id.invalid")
                        && c.searchForUtf8EqualTo("minecraft")) // before 20w21a
                        || c.searchForUtf8EqualTo("ResourceKey[") // from 20w21a
                )
                .thenDeclareRequired(CLASS_RESOURCE_KEY)
                    .optionalConstructor(CONSTRUCTOR_RESOURCE_KEY).real("java.lang.String").end() // before 20w21a
            .next()
                .ifDetect(c -> c.getNumberOfConstructors() <= 1
                    && c.getNumberOfFields() > 15
                    && c.searchForUtf8EqualTo("block")
                    && c.searchForUtf8EqualTo("potion")
                    && c.searchForUtf8EqualTo("dimension")
                    && c.searchForUtf8EqualTo("item")
                )
                .thenDeclareRequired(CLASS_REGISTRY)
                    .requiredField(FIELD_REGISTRY_META_REGISTRY, "field_11144")
                    .requiredField(FIELD_REGISTRY_META_REGISTRY2, "field_11144")
                    .requiredField(FIELD_REGISTRY_META_REGISTRY3, "field_11144")
                	.optionalMethod(METHOD_REGISTRY_CREATE_KEY, "method_29106").real("java.lang.String").end()
                    .optionalMethod(METHOD_REGISTRY_GET_ID, "method_10249").real("java.lang.Object").end()
                    .optionalMethod(METHOD_REGISTRY_GET_ID2, "method_10206").real("java.lang.Object").end()
                    .requiredMethod(METHOD_REGISTRY_GET_BY_KEY, "method_29107").symbolic(CLASS_RESOURCE_KEY).end()
            .next()
            	.ifDetect(c ->
            		c.searchForStringContaining("Missing builtin registry: ")
            	)
            	.thenDeclareOptional(CLASS_DYNAMIC_REGISTRY_MANAGER)
            		.optionalMethod(METHOD_DYNAMIC_REGISTRY_MANAGER_CREATE, "method_30528").end()
            		.optionalMethod(METHOD_DYNAMIC_REGISTRY_MANAGER_GET, "method_30530").symbolic(CLASS_RESOURCE_KEY).end()
            .next()
            	.ifDetect(c ->
            		c.searchForDouble(6.283185307179586D)
            		&& c.searchForUtf8EqualTo("Generating structure reference")
            	)
            	.thenDeclareRequired(CLASS_CHUNK_GENERATOR)
            .next()
                .ifDetect(c -> c.searchForUtf8EqualTo("level-seed")
                	&& c.searchForUtf8EqualTo("generator-settings")
                )
                .thenDeclareRequired(CLASS_WORLD_GEN_SETTINGS)
            		.optionalMethod(METHOD_WORLD_GEN_SETTINGS_CREATE, "method_28021").real("java.util.Properties").end()
                	.optionalMethod(METHOD_WORLD_GEN_SETTINGS_CREATE2, "method_28021").symbolic(CLASS_DYNAMIC_REGISTRY_MANAGER).real("java.util.Properties").end()
            .next()
                .ifDetect(c -> c.getRealClassName().contains("$")
                    && c.isInterface()
                    && c.getNumberOfMethods() == 1
                    && c.hasMethodWithRealArgsReturning("int", "int", "int", null)
                    && !c.hasMethodWithRealArgsReturning("int", "int", "int", "boolean")
                    && !c.searchForUtf8EqualTo("fetch")
                )
                .thenDeclareRequired(CLASS_NOISE_BIOME_PROVIDER)
                    .requiredMethod(METHOD_NOISE_BIOME_PROVIDER_GET_BIOME, "method_16359").real("int").real("int").real("int").end()
            .next()
                .ifDetect(c -> !c.getRealClassName().contains("$")
                    && c.getRealSuperClassName().equals("java/lang/Enum")
                    && c.hasMethodWithRealArgsReturning("long", "int", "int", "int", null, null)
                    && c.hasMethodWithRealArgsReturning("double", "double")
                )
                .thenDeclareRequired(CLASS_OVERWORLD_BIOME_ZOOMER)
                    .requiredMethod(METHOD_BIOME_ZOOMER_GET_BIOME, "method_22396").real("long").real("int").real("int").real("int").symbolic(CLASS_NOISE_BIOME_PROVIDER).end()
            .next()
                .ifDetect(c ->
                    (c.getNumberOfConstructors() == 1 || c.getNumberOfConstructors() == 2)
                    && c.getNumberOfFields() > 0
                    && c.getField(0).hasFlags(AccessFlags.STATIC | AccessFlags.FINAL)
                    && (c.searchForFloat(0.62222224F) || c.searchForUtf8EqualTo("Feature placement"))
                )
                .thenDeclareRequired(CLASS_BIOME)
            .next()
                .ifDetect(c ->
                    (c.searchForStringContaining("Server-Worker-")
                     || c.searchForStringContaining("Worker-"))
                    && c.searchForStringContaining("os.name")
                    && c.searchForLong(1000000L)
                )
                .thenDeclareOptional(CLASS_UTIL)
            .construct();
    }
    // @formatter:on
}
