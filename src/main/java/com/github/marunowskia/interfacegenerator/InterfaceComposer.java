package com.github.marunowskia.interfacegenerator;
import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.graph.ValueGraph;
import com.google.common.io.Files;
import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
import com.google.common.graph.ImmutableValueGraph;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterfaceComposer {

	
	public static void generateAndExportInterfaces(ValueGraph<String, List<String>> methodGraph, File outputDirectory) {
		Collection<InterfaceDefinition> optimizedInterfaces = InterfaceDefinitionConstraintSolver.satisfyConstraints(methodGraph);
		outputInterfaces(optimizedInterfaces, outputDirectory);
	}
	
	

	public static void outputInterfaces(Collection<InterfaceDefinition> requiredInterfaces, File parentDirectory) {
		requiredInterfaces.forEach(def -> {
			StringBuilder builder = new StringBuilder();

			// ==================== Output file structure ====================
			// package PKG;
			// import *.*;
			// public interface NAME {
			// 		public <? extends GenericType> ReturnType getSomething();
			// 		...
			// }


			
			List<String> extendsList = ofNullable(def.mustExtend).orElse(new ArrayList<>())
													 .stream().map(id->id.name).collect(Collectors.toList());

			builder.append("package ").append(def.pkg).append(";\n\n");
			ofNullable(def.dependencies).orElse(new ArrayList<>()).forEach(str -> builder.append("import ").append(str).append(";\n"));
			builder.append("\n\npublic interface ").append(def.name);

			if(0 < Optional.ofNullable(def.genericParameters).map(List::size).orElse(0)) {
				System.out.println("have generic parameters");
				builder.append(" <").append(String.join(", ", def.genericParameters)).append("> ");
			}

			if(0 < Optional.ofNullable(extendsList).map(List::size).orElse(0)) {
				builder.append(" extends ").append(String.join(", ", extendsList));
			}
			builder.append(" {\n");
			
			def.methodSignatures.forEach(str -> builder.append("\t").append(str).append(";\n"));
			builder.append("}\n\n");


			// Write the string builder's content to the appropriate output file.
			try {
				Path outputPath = Paths.get(parentDirectory.getAbsolutePath(), def.pkg.split("\\.")).resolve(def.name);
				// SLF4J just stopped working?
				System.out.println(outputPath);
				File outputFile = outputPath.toFile();
				outputFile.delete();
				Files.createParentDirs(outputFile);
				outputFile.createNewFile();
				Files.write(builder.toString().getBytes(), outputFile);
				System.out.println("\n");
				System.out.println(builder.toString());
				System.out.println("\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	@Data
	public static class InterfaceDefinition {

		public String pkg;
		public String name;
		public List<String> genericParameters;
		public List<String> dependencies;
		public List<InterfaceDefinition> mustExtend;
		public List<String> methodSignatures;
		public String getPkg() {
			return pkg;
		}
		public void setPkg(String pkg) {
			this.pkg = pkg;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public List<String> getGenericParameters() {
			return genericParameters;
		}
		public void setGenericParameters(List<String> genericParameters) {
			this.genericParameters = genericParameters;
		}
		public List<String> getDependencies() {
			return dependencies;
		}
		public void setDependencies(List<String> dependencies) {
			this.dependencies = dependencies;
		}
		public List<InterfaceDefinition> getMustExtend() {
			return mustExtend;
		}
		public void setMustExtend(List<InterfaceDefinition> mustExtend) {
			this.mustExtend = mustExtend;
		}
		public List<String> getMethodSignatures() {
			return methodSignatures;
		}
		public void setMethodSignatures(List<String> methodSignatures) {
			this.methodSignatures = methodSignatures;
		}
		
		
	}
}
