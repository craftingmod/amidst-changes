package amidst.mojangapi.minecraftinterface.local;

import static amidst.mojangapi.minecraftinterface.local.SymbolicNames.*;

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
						.ifDetect(c -> c.getNumberOfConstructors() <= 1
						    && c.getNumberOfFields() > 15
						    && c.searchForUtf8EqualTo("block")
						    && c.searchForUtf8EqualTo("potion")
						    && c.searchForUtf8EqualTo("biome")
						    && c.searchForUtf8EqualTo("item")
						)
						.thenDeclareRequired(CLASS_REGISTRY)
					.next()
					    .ifDetect(c ->
					        (c.searchForStringContaining("Server-Worker-")
					         || c.searchForStringContaining("Worker-"))
					        && c.searchForStringContaining("os.name")
					        && c.searchForLong(1000000L)
					    )
					    .thenDeclareOptional(CLASS_UTIL)
					.next()
						.ifDetect(c ->
							c.searchForLong(2003L)
							&& c.searchForLong(70L)
							&& c.searchForLong(50L)
						)
						.thenDeclareRequired(CLASS_BIOME_LAYERS)
							.requiredMethod(METHOD_BIOME_LAYERS_BUILD, "a").real("boolean").real("int").real("int").real("java.util.function.LongFunction").end()
//					.next()
//						.ifDetect(c ->
//							c.searchForUtf8EqualTo("Unknown biome id: ")
//						)
//						.thenDeclareRequired(CLASS_BIOME_LAYER_SAMPLER)
//							.requiredField(FIELD_BIOME_LAYER_SAMPLER_SAMPLER, "b")
//					.next()
//						.ifDetect(c ->
//							c.getRealSuperClassName().equals("java/lang/Object")
//							&& c.getNumberOfConstructors() == 1
//							&& c.getNumberOfFields() == 3
//							&& c.getField(0).hasFlags(AccessFlags.PRIVATE | AccessFlags.FINAL)
//							&& c.getField(1).hasFlags(AccessFlags.PRIVATE | AccessFlags.FINAL)
//							&& c.getField(2).hasFlags(AccessFlags.PRIVATE | AccessFlags.FINAL)
//							&& c.hasMethodWithRealArgsReturning("int", "int", "int")
//							&& c.hasMethodWithRealArgsReturning("int")
//							&& c.isFinal()
//						)
//						.thenDeclareRequired(CLASS_CACHING_LAYER_SAMPLER)
//							.requiredField(FIELD_CACHING_LAYER_SAMPLER_LAYER_OPERATOR, "a")
					.next()
						.ifDetect(c ->
							c.isInterface()
							&& c.hasMethodWithRealArgsReturning("int", "int", "int")
							&& c.getNumberOfMethods() == 1
							&& c.getNumberOfConstructors() == 0
							&& c.getNumberOfFields() == 0
							&& c.searchForUtf8EqualTo("apply")
						)
						.thenDeclareRequired(CLASS_LAYER_OPERATOR)
							.requiredMethod(METHOD_LAYER_OPERATOR_APPLY, "apply").real("int").real("int").end()
					.next()
						.ifDetect(c ->
							c.isInterface()
							&& c.hasMethodWithRealArgsReturning("int", "int", "int")
							&& c.getNumberOfMethods() == 1
							&& c.getNumberOfConstructors() == 0
							&& c.getNumberOfFields() == 0
							&& !c.searchForUtf8EqualTo("apply")
						)
						.thenDeclareRequired(CLASS_LAYER_SAMPLER)
							.requiredMethod(METHOD_LAYER_SAMPLER_SAMPLE, "a").real("int").real("int").end()
					.next()
						.ifDetect(c ->
							c.isInterface()
							&& c.hasMethodWithRealArgsReturning(new String[] { null })
							&& c.getNumberOfMethods() == 1
							&& c.getNumberOfConstructors() == 0
							&& c.getNumberOfFields() == 0
							&& c.searchForUtf8EqualTo("make")
						)
						.thenDeclareRequired(CLASS_LAYER_FACTORY)
							.requiredMethod(METHOD_LAYER_FACTORY_MAKE, "make").end()
					.next()
						.ifDetect(c ->
							c.isInterface()
							&& !c.getRealSuperClassName().equals("java.lang.Object") // TODO: check this
							&& c.hasMethodWithRealArgsReturning("long", "long", null)
							&& c.hasMethodWithRealArgsReturning("int", "int", "int")
							&& c.hasMethodWithRealArgsReturning("int", "int", "int", "int", "int")
						)
						.thenDeclareRequired(CLASS_LAYER_SAMPLE_CONTEXT)
					.next()
						.ifDetect(c ->
							c.hasMethodWithRealArgsReturning("int", "int", "int", "double", "double", "double", "double", "double", "double", "double")
						)
						.thenDeclareRequired(CLASS_PERLIN_NOISE_SAMPLER)
							.requiredConstructor(CONSTRUCTOR_PERLIN_NOISE_SAMPLER).real("java.util.Random").end()
					.construct();
    }
    // @formatter:on
}
