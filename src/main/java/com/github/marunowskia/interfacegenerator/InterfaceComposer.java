package com.github.marunowskia.interfacegenerator;
import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ValueGraph;
import com.google.common.io.Files;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterfaceComposer {

	
	public static void generateAndExportInterfaces(ValueGraph<String, List<GenericMethod>> methodGraph, File outputDirectory) {
//		List<InterfaceDefinition> optimizedInterfaces = InterfaceDefinitionConstraintSolver.satisfyConstraints(methodGraph);
//		InterfaceDefinitionConstraintSolver.buildInterfaceDefinitions(methodGraph);
		Set<InterfaceDefinition> result = InterfaceDefinitionConstraintSolver.buildResult(methodGraph);
		outputInterfaces(InterfaceDefinitionConstraintSolver.buildResult(methodGraph), outputDirectory);
	}
	
	

	public static void outputInterfaces(Collection<InterfaceDefinition> requiredInterfaces, File parentDirectory) {
		
		requiredInterfaces.forEach(def -> {
			if(StringUtils.isBlank(def.pkg)) {
				def.pkg = "defaultpackage";
			}
			
			Path outputPath = Paths.get(parentDirectory.getAbsolutePath(), def.pkg.split("\\.")).resolve(def.name);
			System.out.println(outputPath);
		});
		requiredInterfaces.forEach(def -> {
			StringBuilder builder = new StringBuilder();

			// ==================== Output file structure ====================
			// package PKG;
			// import *.*;
			// public interface NAME {
			// 		public <? extends GenericType> ReturnType getSomething();
			// 		...
			// }


			
			Set<String> extendsList = ofNullable(def.mustExtend).orElse(new HashSet<>())
													 .stream().map(id->id.name).collect(Collectors.toSet());

//			builder.append("package ").append(def.pkg).append(";\n");
			ofNullable(def.dependencies).orElse(new HashSet<>()).forEach(str -> builder.append("import ").append(str).append(";\n"));
			builder.append("\npublic interface ").append(def.name);

			if(0 < Optional.ofNullable(def.genericParameters).map(Set::size).orElse(0)) {
				System.out.println("have generic parameters");
				builder.append(" <").append(String.join(", ", def.genericParameters)).append("> ");
			}

			if(0 < Optional.ofNullable(extendsList).map(Set::size).orElse(0)) {
				builder.append(" extends ").append(String.join(", ", extendsList));
			}
			builder.append(" {\n");
			
			def.methodSignatures.forEach(method -> builder.append("\t").append(method.getMethodSignature()).append(";\n"));
			builder.append("}");


			// Write the string builder's content to the appropriate output file.
			try {
				Path outputPath = Paths.get(parentDirectory.getAbsolutePath(), def.pkg.split("\\.")).resolve(def.name);
				// SLF4J just stopped working?
				File outputFile = outputPath.toFile();
				outputFile.delete();
				Files.createParentDirs(outputFile);
				outputFile.createNewFile();
				
				System.out.println(builder.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	@Data
	public static class InterfaceDefinition {

		public String pkg;
		public String name;
		public Set<String> genericParameters = new HashSet<>();
		public Set<String> dependencies = new HashSet<>();
		
		public Set<InterfaceDefinition> mustExtend = new HashSet<>();
		
		public Set<InterfaceDefinition> extendedBy = new HashSet<>();
		public Set<String> implementedBy = new HashSet<>();
		
		public Set<GenericMethod> methodSignatures = new HashSet<>();
		
		public InterfaceDefinition(String pkg, String name) {
			this.pkg = pkg;
			this.name = name;
		}
		
		public InterfaceDefinition() {
		}
		
		public String getPkg() {
			return pkg;
		}
		public String getName() {
			return name;
		}
		
		public String setPkg(String setTo) {
			return pkg = setTo;
		}
		public String setName(String setTo) {
			return name = setTo;
		}
		public Set<String> getGenericParameters() {
			return genericParameters;
		}
		public void setGenericParameters(Set<String> genericParameters) {
			this.genericParameters = genericParameters;
		}
		public Set<String> getDependencies() {
			return dependencies;
		}
		public void setDependencies(Set<String> dependencies) {
			this.dependencies = dependencies;
		}
		public Set<InterfaceDefinition> getMustExtend() {
			return mustExtend;
		}
		public void setMustExtend(Set<InterfaceDefinition> mustExtend) {
			this.mustExtend = mustExtend;
		}
		public Set<GenericMethod> getMethodSignatures() {
			return methodSignatures;
		}
		public void setMethodSignatures(Set<GenericMethod> methodSignatures) {
			this.methodSignatures = methodSignatures;
		}
		
		public Set<InterfaceDefinition> getExtendedBy() {
			return extendedBy;
		}
		public void setExtendedBy(Set<InterfaceDefinition> extendedBy) {
			this.extendedBy = extendedBy;
		}
		
		
		public Set<String> getImplementedBy() {
			return implementedBy;
		}
		public void setImplementedBy(Set<String> implementedBy) {
			this.implementedBy = implementedBy;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((pkg == null) ? 0 : pkg.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			InterfaceDefinition other = (InterfaceDefinition) obj;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			if (pkg == null) {
				if (other.pkg != null)
					return false;
			} else if (!pkg.equals(other.pkg))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "Interface " + name;
		}
		
		
		
		
	}
}
