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
                .ifDetect(c -> c.getNumberOfConstructors() == 3
                        && c.getNumberOfFields() == 4
                        && c.getField(1).hasFlags(AccessFlags.PRIVATE | AccessFlags.STATIC | AccessFlags.FINAL)
                        && c.searchForUtf8EqualTo("argument.id.invalid")
                        && c.searchForUtf8EqualTo("minecraft")
                )
                .thenDeclareOptional(CLASS_REGISTRY_KEY)
                    .requiredConstructor(CONSTRUCTOR_REGISTRY_KEY).real("java.lang.String").end()
            .next()
                .ifDetect(c -> c.getNumberOfConstructors() <= 1
                    && c.getNumberOfFields() > 15
                    && c.searchForUtf8EqualTo("block")
                    && c.searchForUtf8EqualTo("potion")
                    && c.searchForUtf8EqualTo("biome")
                    && c.searchForUtf8EqualTo("item")
                )
                .thenDeclareOptional(CLASS_REGISTRY)
                    .requiredField(FIELD_REGISTRY_META_REGISTRY, "g")
                    .requiredMethod(METHOD_REGISTRY_GET_ID, "a").real("java.lang.Object").end()
                    .requiredMethod(METHOD_REGISTRY_GET_BY_KEY, "a").symbolic(CLASS_REGISTRY_KEY).end()
            .next()
                .ifDetect(c -> c.getRealClassName().contains("$")
                    && c.isInterface()
                    && c.getNumberOfMethods() == 1
                    && c.hasMethodWithRealArgsReturning("int", "int", "int", null)
                    && !c.hasMethodWithRealArgsReturning("int", "int", "int", "boolean")
                )
                .thenDeclareRequired(CLASS_NOISE_BIOME_PROVIDER)
                    .requiredMethod(METHOD_NOISE_BIOME_PROVIDER_GET_BIOME, "b").real("int").real("int").real("int").end()
            .next()
                .ifDetect(c -> !c.getRealClassName().contains("$")
                    && c.getRealSuperClassName().equals("java/lang/Enum")
                    && c.hasMethodWithRealArgsReturning("long", "int", "int", "int", null, null)
                    && c.getNumberOfMethods() == 7
                )
                .thenDeclareRequired(CLASS_BIOME_ZOOMER)
                    .requiredMethod(METHOD_BIOME_ZOOMER_GET_BIOME, "a").real("long").real("int").real("int").real("int").symbolic(CLASS_NOISE_BIOME_PROVIDER).end()
            .next()
            	.ifDetect(c -> c.searchForUtf8EqualTo("legacy_biome_init_layer")
	            	&& c.searchForUtf8EqualTo("large_biomes")
	            	&& c.searchForUtf8EqualTo("seed")
	            	&& c.hasMethodWithRealArgsReturning("int", "int", "int", null)
            	)
            	.thenDeclareRequired(CLASS_OVERWORLD_BIOME_PROVIDER)
            		.requiredConstructor(CONSTRUCTOR_OVERWORLD_BIOME_PROVIDER).real("long").real("boolean").real("boolean").end()
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
