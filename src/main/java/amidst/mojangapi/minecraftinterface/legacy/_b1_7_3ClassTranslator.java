package amidst.mojangapi.minecraftinterface.legacy;

import static amidst.mojangapi.minecraftinterface.legacy._b1_7_3SymbolicNames.*;

import amidst.clazz.translator.ClassTranslator;

public enum _b1_7_3ClassTranslator {
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
					c.isInterface()
					&& c.getNumberOfMethods() == 6
					&& c.hasMethodWithRealArgsReturning("java/lang/String", "java/io/File")
				)
				.thenDeclareRequired(CLASS_I_SAVE_HANDLER)
			.next()
				.ifDetect(c -> 
					c.searchForLong(0xffffffL)
				)
				.thenDeclareRequired(CLASS_WORLD)
					.requiredConstructor(CONSTRUCTOR_WORLD).symbolic(CLASS_I_SAVE_HANDLER).real("String").real("long").end()
			.next()
				.ifDetect(c -> 
					c.searchForUtf8EqualTo("Plains")
				)
				.thenDeclareRequired(CLASS_BIOME_GEN_BASE)
					.requiredField(FIELD_BIOME_GEN_BASE_COLOR, "o")
			.next()
				.ifDetect(c -> 
					c.searchForLong(0x84a59L)
				)
				.thenDeclareRequired(CLASS_WORLD_CHUNK_MANAGER)
					.requiredConstructor(CONSTRUCTOR_WORLD_CHUNK_MANAGER).symbolic(CLASS_WORLD).end()
					.requiredMethod(METHOD_WORLD_CHUNK_MANAGER_GET_BIOMES, "a").real("int").real("int").real("int").real("int").end()
					.requiredField(FIELD_WORLD_CHUNK_MANAGER_TEMPERATURE, "a")
			.next()
				.ifDetect(c -> 
					c.searchForLong(0x1ef1565bd5L)
					&& c.searchForDouble(1.3999999999999999D)
					&& c.searchForDouble(0.29999999999999999D)
				)
				.thenDeclareRequired(CLASS_CHUNK_PROVIDER_GENERATOR)
					.requiredConstructor(CONSTRUCTOR_CHUNK_PROVIDER_GENERATOR).symbolic(CLASS_WORLD).real("long").end()
					.requiredField(FIELD_CHUNK_PROVIDER_GENERATOR_RANDOM, "j")
					.requiredMethod(METHOD_CHUNK_PROVIDER_GENERATOR_PRODUCE_NOISE, "a").realArray("double", 1).real("int").real("int").real("int").real("int").real("int").real("int").end()
			.construct();
	}
	
	public boolean extract(amidst.clazz.real.RealClass c) {
		if(c.getRealClassName().equals("fd")) {
			return true;
		}
		return false;
	}
}
