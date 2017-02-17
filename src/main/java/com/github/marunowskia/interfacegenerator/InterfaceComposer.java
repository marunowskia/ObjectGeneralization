package com.github.marunowskia.interfacegenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.io.Files;

public class InterfaceComposer {

	public static List<InterfaceDefinition> composeInterfaces() {
		return null;
	}
	
	public static void outputInterfaces(List<InterfaceDefinition> requiredInterfaces, File parentDirectory) {
		requiredInterfaces.forEach(def -> {
			StringBuilder builder = new StringBuilder();
			
			// ==================== Output file structure ====================
			// package PKG;
			// import *.*;
			// public interface NAME {
			// 		public <? extends GenericType> ReturnType getSomething();
			// 		...
			// }

			builder.append("package ").append(def.pkg).append(";\n\n");
			def.dependencies.forEach(str -> builder.append("import ").append(str).append(";\n"));
			builder.append("public interface ").append(def.name).append("{\n");
			def.methodSignatures.forEach(str -> builder.append(str).append(";\n"));
			builder.append('}');
			
			try {
				Path outputPath = Paths.get(parentDirectory.getAbsolutePath(), def.pkg.split("\\."));
				File outputFile = outputPath.toFile();
				Files.write(builder.toString().getBytes(), outputFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	public static class InterfaceDefinition {
		public String pkg;
		public String name;
		public List<String> dependencies;
		public List<InterfaceDefinition> mustExtend;
		public List<String> methodSignatures;
	}
}
