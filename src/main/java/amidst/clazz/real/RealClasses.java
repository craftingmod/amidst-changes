package amidst.clazz.real;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import amidst.documentation.Immutable;
import net.fabricmc.loader.util.FileSystemUtil;

@Immutable
public enum RealClasses {
	;
	
	private static final RealClassBuilder REAL_CLASS_BUILDER = new RealClassBuilder();

	public static List<RealClass> fromJarFile(Path jarFile) throws FileNotFoundException, JarFileParsingException {
		return readRealClassesFromJarFile(jarFile);
	}

	private static List<RealClass> readRealClassesFromJarFile(Path jarFile)
			throws JarFileParsingException,
			FileNotFoundException {
		if (!Files.exists(jarFile)) {
			throw new FileNotFoundException("Attempted to load jar file at: " + jarFile + " but it does not exist.");
		}

		try (FileSystem jarContents = FileSystemUtil.getJarFileSystem(jarFile, true).get()){
			return readJarFile(jarContents);
		} catch (IOException | RealClassCreationException e) {
			throw new JarFileParsingException("Error extracting jar data.", e);
		}
	}

	private static List<RealClass> readJarFile(FileSystem zipFile) throws IOException, RealClassCreationException {
		List<RealClass> result = new ArrayList<>();
		for (Path root: zipFile.getRootDirectories()) {
			readJarFileDirectory(root, result);
		}
		return result;
	}

	private static void readJarFileDirectory(Path directory, List<RealClass> result)
			throws IOException,
			RealClassCreationException {
		for (Path path: (Iterable<Path>) Files.list(directory)::iterator) {
			String realClassName = getPathWithoutExtension(path.toString(), "class");
			if (Files.isDirectory(path)) {
				readJarFileDirectory(path, result);
			} else if (realClassName != null) {
				RealClass realClass = readRealClass(realClassName, new BufferedInputStream(Files.newInputStream(path)));
				if (realClass != null) {
					result.add(realClass);
				}
			}
		}
	}

	private static RealClass readRealClass(String realClassName, BufferedInputStream stream)
			throws IOException,
			RealClassCreationException {
		try (BufferedInputStream theStream = stream) {
			byte[] classData = new byte[theStream.available()];
			theStream.read(classData);
			return REAL_CLASS_BUILDER.construct(realClassName, classData);
		}
	}

	@SuppressWarnings("unused")
	private static String getFileNameWithoutExtension(String fileName, String extension) {
		String[] split = fileName.split("\\.");
		if (split.length == 2 && split[0].indexOf('/') == -1 && split[1].equals(extension)) {
			return split[0];
		} else {
			return null;
		}
	}
	
	private static String getPathWithoutExtension(String path, String extension) {
		String noExt = path.split("\\." + extension)[0];
		noExt = noExt.replace('/', '.');
		if (noExt.charAt(0) == '.') {
			return noExt.substring(1);
		} else {
			return noExt;
		}
	}
	
}
