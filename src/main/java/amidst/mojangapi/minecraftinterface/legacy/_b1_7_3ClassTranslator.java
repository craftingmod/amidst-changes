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
					.requiredConstructor(CONSTRUCTOR_WORLD).symbolic(CLASS_I_SAVE_HANDLER).real("java.lang.String").real("long").end()
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
					.requiredMethod(METHOD_WORLD_CHUNK_MANAGER_GET_BIOME_GEN_AT, "a").real("int").real("int").end()
			.construct();
	}
	
	public boolean extract(amidst.clazz.real.RealClass c) {
		System.out.println(c.getRealClassName());
		if(c.getRealClassName().equals("fd")) {
			System.out.println();
			return true;
		}
		return false;
	}
}
