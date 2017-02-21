package com.github.marunowskia.interfacegenerator;
import static java.util.Optional.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.io.Files;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterfaceComposer {

	
	public static void generateAndExportInterfaces(ValueGraph<String, List<String>> methodGraph, File outputDirectory) {
		outputInterfaces(composeInterfaces(methodGraph), outputDirectory );
	}
	
	public static Collection<InterfaceDefinition> composeInterfaces(ValueGraph<String, List<String>> methodGraph ) {
//		Hashtable<String, String> assignedInterface = new Hashtable<>(); // Map from a package+class to one of the interfaces we plan to output
//		Hashtable<String, InterfaceDefinition> assignedInterfaceActual = new Hashtable<>(); // Map from a package+class to one of the interfaces we plan to output
		Collection<InterfaceDefinition> requiredInterfaces = new ArrayList<>();
		
		
		methodGraph.nodes().forEach(node -> {
			Set<String> returnedTypes = methodGraph.successors(node);
			final InterfaceDefinition assignedInterface = new InterfaceDefinition();
			assignedInterface.setName(StringUtils.substringAfterLast(node, "."));
			assignedInterface.setPkg(StringUtils.substringBeforeLast(node, "."));
//			assignedInterface.setGenericParameters();// Is this actually necessary? Can this just get extracted as part of the type name?
			/* TODO: 	IMPORTANT: RETURN TYPES OF THE SAME CLASS/INTERFACE,
						BUT DIFFERENT GENERIC PARAMETERS *MUST* NOT RESULT IN MORE 
						THAN ONE INTERFACES BEING GENERATED 
			*/ 
			returnedTypes.forEach(type -> {
				List<String> methodNames = methodGraph.edgeValue(node, type);
				
				// Generate the return type:
				methodNames.stream().map(name -> new StringBuilder().append("public ").append(type).append(" ").append(name).append("()").toString());
				assignedInterface.setMethodSignatures(methodNames);
				requiredInterfaces.add(assignedInterface);
			});
		});
		
		return requiredInterfaces;//assignedInterfaceActual.values();
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
