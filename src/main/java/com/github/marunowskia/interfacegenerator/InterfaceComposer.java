package com.github.marunowskia.interfacegenerator;
import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.graph.ValueGraph;
import com.google.common.io.Files;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterfaceComposer {

	
	public static void generateAndExportInterfaces(ValueGraph<String, List<MethodDeclaration>> methodGraph, File outputDirectory) {
		List<InterfaceDefinition> optimizedInterfaces = InterfaceDefinitionConstraintSolver.satisfyConstraints(methodGraph);
		outputInterfaces(optimizedInterfaces, outputDirectory);
	}
	
	

	public static void outputInterfaces(Collection<InterfaceDefinition> requiredInterfaces, File parentDirectory) {
		
		requiredInterfaces.forEach(def -> {
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


			
			List<String> extendsList = ofNullable(def.mustExtend).orElse(new ArrayList<>())
													 .stream().map(id->id.name).collect(Collectors.toList());

//			builder.append("package ").append(def.pkg).append(";\n");
			ofNullable(def.dependencies).orElse(new ArrayList<>()).forEach(str -> builder.append("import ").append(str).append(";\n"));
			builder.append("\npublic interface ").append(def.name);

			if(0 < Optional.ofNullable(def.genericParameters).map(List::size).orElse(0)) {
				System.out.println("have generic parameters");
				builder.append(" <").append(String.join(", ", def.genericParameters)).append("> ");
			}

			if(0 < Optional.ofNullable(extendsList).map(List::size).orElse(0)) {
				builder.append(" extends ").append(String.join(", ", extendsList));
			}
			builder.append(" {\n");
			
			def.getMethodSignatures().keySet().forEach(str -> builder.append("\t").append(str).append(";\n"));
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
		public List<String> genericParameters = new ArrayList<>();
		public List<String> dependencies = new ArrayList<>();
		public List<InterfaceDefinition> mustExtend = new ArrayList<>();
		public HashMap<String, MethodDeclaration> methodSignatures = new HashMap<>();
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
		public HashMap<String, MethodDeclaration> getMethodSignatures() {
			return methodSignatures;
		}
		public void setMethodSignatures(HashMap<String, MethodDeclaration> methodSignatures) {
			this.methodSignatures = methodSignatures;
		}
		@Override
		public String toString() {
			return "InterfaceDefinition [pkg=" + pkg + ", name=" + name + ", genericParameters=" + genericParameters
					+ ", dependencies=" + dependencies + ", methodSignatures=" + methodSignatures + "]";
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
			if (dependencies == null) {
				if (other.dependencies != null)
					return false;
			} else if (!dependencies.equals(other.dependencies))
				return false;
			if (genericParameters == null) {
				if (other.genericParameters != null)
					return false;
			} else if (!genericParameters.equals(other.genericParameters))
				return false;
			if (methodSignatures == null) {
				if (other.methodSignatures != null)
					return false;
			} else if (!methodSignatures.equals(other.methodSignatures))
				return false;
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
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((dependencies == null) ? 0 : dependencies.hashCode());
			result = prime * result + ((genericParameters == null) ? 0 : genericParameters.hashCode());
			result = prime * result + ((methodSignatures == null) ? 0 : methodSignatures.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((pkg == null) ? 0 : pkg.hashCode());
			return result;
		}
		
		
	}
}
