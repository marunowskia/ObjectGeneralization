package com.github.marunowskia.interfacegenerator;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ValueGraph;
import com.google.common.io.Files;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterfaceComposer {

	public static void generateAndExportInterfaces(ValueGraph<String, List<GenericMethod>> methodGraph,
			File outputDirectory) {
		// List<InterfaceDefinition> optimizedInterfaces =
		// InterfaceDefinitionConstraintSolver.satisfyConstraints(methodGraph);
		// InterfaceDefinitionConstraintSolver.buildInterfaceDefinitions(methodGraph);
		Set<InterfaceDefinition> result = InterfaceDefinitionConstraintSolver.buildResult(methodGraph);
		outputInterfaces(InterfaceDefinitionConstraintSolver.buildResult(methodGraph), outputDirectory);
	}

	public static void outputInterfaces(Collection<InterfaceDefinition> requiredInterfaces, File parentDirectory) {

		requiredInterfaces.forEach(def -> {
			if (StringUtils.isBlank(def.pkg)) {
				def.pkg = "defaultpackage";
			}

			Path outputPath = Paths.get(parentDirectory.getAbsolutePath(), def.pkg.split("\\.")).resolve(def.name);
			System.out.println(outputPath);
		});
		requiredInterfaces.forEach(def -> {

			String javaSourceCode = createJavaSourceCode(def);

			// Write the string builder's content to the appropriate output
			// file.
			try {
				Path outputPath = Paths.get(parentDirectory.getAbsolutePath(), def.pkg.split("\\."))
						.resolve(def.name + ".java");
				// SLF4J just stopped working?
				File outputFile = outputPath.toFile();
				outputFile.delete();
				Files.createParentDirs(outputFile);
				outputFile.createNewFile();
				Files.write(javaSourceCode.getBytes(), outputFile);
				System.out.println(javaSourceCode);

			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private static String createJavaSourceCode(InterfaceDefinition def) {
		StringBuilder builder = new StringBuilder();

		// ==================== Output file structure ====================
		// package PKG;
		addPackageDeclaration(builder, def);

		// import *.*;
		addImports(builder, def);

		//
		// // Implemented by:
		// // -> PACKAGE_1.TYPE_1
		// // -> ... ...
		// // -> PACKAGE_X.TYPE_X
		addImplementedByComments(builder, def);

		// // Extended by:
		// // -> PACKAGE_1.TYPE_1
		// // -> ... ...
		// // -> PACKAGE_Y.TYPE_Y
		addExtendedByComments(builder, def);

		// public interface NAME
		// extends
		// SuperInterface1,
		// ...
		// SuperInterfaceZ
		// {
		// public ReturnType getSomething();
		// public default Optional<ReturnType> oSomething() {...}
		// public default ReturnType something() {...}

		// public JAXBElement<ReturnType> getSomethingElse();
		// public default Optional<? extends ReturnType> oSomethingElse() { }
		// public default ReturnType somethingElse() {...}
		// ...
		// public default boolean isMissing() {return false;}
		// public static final NAME MISSING = new NAME() {}
		// public static NAME missing() { return MISSING;}
		//
		// }
		addInterfaceDeclaration(builder, def);
		// ==================== Output file structure ====================

		return builder.toString();
	}

	private static void addPackageDeclaration(StringBuilder builder, InterfaceDefinition def) {
		Preconditions.checkArgument(StringUtils.isNotBlank(def.pkg));
		builder.append("package ").append(def.pkg).append(";\n\n");
	}

	private static void addImports(StringBuilder builder, InterfaceDefinition def) {
		Set<String> dependencies = def.getDependencies();

		builder	.append("import defaultpackage.*;\n")
				.append("import java.util.Optional;\n\n");

		dependencies.forEach(dependency -> {
			builder.append("import ").append(dependency).append(";\n");
		});

		if (!dependencies.isEmpty()) {
			builder.append("\n");
		}
	}

	private static void addImplementedByComments(StringBuilder builder, InterfaceDefinition def) {

		Set<String> implementedBy = def.getImplementedBy();

		if (!implementedBy.isEmpty()) {
			builder.append("// Implemented by:\n");

			implementedBy.forEach(implementor -> {
				builder	.append("//")
						.append(implementor).append("\n");
			});

			builder.append("\n");
		}
	}

	private static void addExtendedByComments(StringBuilder builder, InterfaceDefinition def) {
		Set<InterfaceDefinition> extendedBy = def.getExtendedBy();

		if (!extendedBy.isEmpty()) {
			builder.append("// Extended by:\n");

			extendedBy.forEach(otherInterface -> {
				
				builder
					.append("//")
					.append(otherInterface.getPkg())
					.append(".")
					.append(otherInterface.getName())
					.append("\n");
			});

			builder.append("\n");
		}

	}

	private static void addInterfaceDeclaration(StringBuilder builder, InterfaceDefinition def) {

		// public interface NAME
		builder.append("\npublic interface ").append(def.name).append("\n");

		// extends
		// SuperInterface1,
		// ...
		// SuperInterfaceZ
		addExtendsDeclarations(builder, def);

		// {
		builder.append("{\n");

		// default public ReturnType getSomething() {return null;}
		// default public JAXBElement<ReturnType> getSomethingElse() {return
		// null;}
		//
		addRequiredMethods(builder, def);

		// public default Optional<ReturnType> oSomething() {return
		// Optional.ofNullable(getSomething();}
		// public default Optional<? extends ReturnType> oSomethingElse()
		// {return Optional.ofNullable(getSomethingElse();}
		//
		addOptionalWrappers(builder, def);

		// public default ReturnType something() {...}
		// public default ReturnType somethingElse() {...}
		//
		addChainableMethods(builder, def);

		// public default boolean isMissing() {return false;}
		// public static final NAME MISSING = new NAME() {}
		// public static NAME missing() { return MISSING;}
		//
		addIsMissingComponent(builder, def);

		// }
		builder.append("}");
	}

	private static void addExtendsDeclarations(StringBuilder builder, InterfaceDefinition def) {
		Set<InterfaceDefinition> extendsList = new HashSet<>();
		ofNullable(def.mustExtend).ifPresent(extendsList::addAll);

		if (!extendsList.isEmpty()) {
			builder.append("extends\n");

			extendsList
					.stream()
					.filter(extended -> StringUtils.equals(extended.getPkg(), def.getPkg()))
					.map(InterfaceDefinition::getName)
					.sorted()
					.forEach(fromSamePkg -> builder.append("\t/*?*/").append(fromSamePkg).append(",\n"));

			extendsList.stream().filter(extended -> !StringUtils.equals(extended.getPkg(), def.getPkg())).sorted(
					Comparator.comparing(InterfaceDefinition::getPkg).thenComparing(InterfaceDefinition::getName))

					.forEach(fromOtherPkg -> builder.append("\t").append(fromOtherPkg.getPkg()).append(".")
							.append(fromOtherPkg.getName()).append(",\n"));

			builder.setLength(builder.length() - 2); // Hack to take off the
														// last comma
			builder.append("\n");
		}
	}

	private static void addRequiredMethods(StringBuilder builder, InterfaceDefinition def) {
		def.methodSignatures.forEach(method -> builder.append("\tdefault ").append(method.getMethodSignature())
				.append(" { return null;} \n\n"));

		if (!def.getMethodSignatures().isEmpty()) {
			builder.append("\n");
		}
	}

	private static void addOptionalWrappers(StringBuilder builder, InterfaceDefinition def) {
		def.methodSignatures.forEach(method -> {
			String originalMethodName = method.getOriginalDeclaration().getNameExpr().toStringWithoutComments();
			builder.append("\tpublic default Optional<? extends ").append(method.getReturnTypeString()).append("> o")
					.append(StringUtils.capitalize(StringUtils.substringAfter(originalMethodName, "get")))
					.append("() {return Optional.ofNullable(").append(originalMethodName).append("());}\n\n");
		});

		if (!def.getMethodSignatures().isEmpty()) {
			builder.append("\n");
		}
	}

	private static void addChainableMethods(StringBuilder builder, InterfaceDefinition def) {
		// TODO: Need to work out details for automatically unwrapping various wrapper classes.
		
		def.getMethodSignatures().forEach(method -> {
		
			String originalMethodName = method.getOriginalDeclaration().getNameExpr().toStringWithoutComments();
			String chainableReturnType = getChainableReturnType(method);
			String chainableMethodBody = getChainableMethodBody(method);
			
			builder	.append("\tpublic default ")
					.append(chainableReturnType)
					.append(" ")
					.append(StringUtils.uncapitalize(StringUtils.substringAfter(originalMethodName, "get")))
					.append("() {\n")
					.append(chainableMethodBody)
					.append("\n\t}\n\n");
		});
		
		// public default java.util.List<? extends ICustomField> customFields()
		// {
		// List<? extends ICustomField> result = getCustomField();
		// return result != null : result : Collections.EMPTY_LIST;
		// }
	}
	
	private static String getOriginalMethodName(GenericMethod method) {
		return method.getOriginalDeclaration().getNameExpr().toStringWithoutComments();
	}

	private static String getChainableMethodBody(GenericMethod method) {
		String returnType = getChainableReturnType(method);
		if(method.getReturnTypeString().contains("JAXBElement")) {
			return "\t\tOptional< ? extends " + returnType + "> result = ComOp.oj(" + getOriginalMethodName(method) + "());\n\t\treturn result.isPresent()?result.get() : " + returnType + ".missing();"; 
		}
		return "\t\treturn " + getOriginalMethodName(method) + "();";
	}

	private static String getChainableReturnType(GenericMethod method) {
		if(method.getReturnTypeString().contains("JAXBElement")) {
			String genericComponent = TypeUpdateUtility.getGenericComponent(method.getReturnTypeString());
			Collection<String> typeComponents = TypeUpdateUtility.getAllReferencedTypes(genericComponent);
			return typeComponents.stream().findFirst().orElse("void");
		}
		return method.getReturnTypeString();
	}
private static void addIsMissingComponent(StringBuilder builder, InterfaceDefinition def) {
		builder.append("\tpublic static final ").append(def.getName()).append(" MISSING = new ").append(def.getName())
				.append("() {};\n");

		builder.append("\tpublic static ").append(def.getName()).append(" missing() { return MISSING;}\n");

		builder.append("\tpublic default boolean isMissing() {return this==MISSING;}\n\n");
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
