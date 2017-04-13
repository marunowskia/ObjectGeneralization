package com.github.marunowskia.interfacegenerator;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.reflect.ClassPath;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.io.FileUtils;
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
												   File outputDirectory, HashMap<String,CompilationUnit> originalSources,
                                                   boolean updateOriginalFiles) {

		Set<String> relevantTypes = originalSources.keySet()
                .stream()
                .filter(name -> !name.endsWith("Response"))
				.filter(name -> !name.endsWith("Response2"))
                .collect(Collectors.toSet());

		Set<InterfaceDefinition> result = InterfaceDefinitionConstraintSolver.buildResult(methodGraph, relevantTypes);

		outputInterfaces(result, outputDirectory);

		if(updateOriginalFiles) {
		    updateJavaSource(result, originalSources);
        }
	}

	public static void updateJavaSource(Collection<InterfaceDefinition> interfaces, HashMap<String,CompilationUnit> originalSources) {
		HashMap<String, Collection<InterfaceDefinition>> typeToMustImplementMap = new HashMap<>();
		interfaces.forEach(iface -> {
			iface.getImplementedBy().forEach(type -> {
				Collection<InterfaceDefinition> typeMustImplement = typeToMustImplementMap.getOrDefault(type, new HashSet<>());
				typeMustImplement.add(iface);
				typeToMustImplementMap.put(type, typeMustImplement);
			});
		});

		originalSources.forEach((name, source) -> {
			File originalFile = (File)source.getData(); // we put this here when building "methodGraph"
			for (TypeDeclaration typeDeclaration : source.getTypes()) {
				if(typeDeclaration instanceof ClassOrInterfaceDeclaration) {

				    if(typeToMustImplementMap.containsKey(name)) {
                        if (((ClassOrInterfaceDeclaration) typeDeclaration).getImplements() == null) {
                            ((ClassOrInterfaceDeclaration) typeDeclaration).setImplements(new ArrayList<>());
                        }

                        typeToMustImplementMap.get(name).forEach(mustImplement -> {
                            String fullyQualifiedPathToAdd = mustImplement.getPkg() + "." + mustImplement.getName();
                            ClassOrInterfaceDeclaration classOrInterface = (ClassOrInterfaceDeclaration) typeDeclaration;

                            boolean alreadExists = classOrInterface.getImplements()
                                    .stream()
                                    .map(existing -> existing.getScope() + "." + existing.getName())
                                    .anyMatch(fullyQualifiedPathToAdd::equals);

                            boolean complicated = CollectionUtils.isNotEmpty(classOrInterface.getExtends());

                            if(!alreadExists && !complicated) {
                                ClassOrInterfaceType ifaceToAdd = new ClassOrInterfaceType(fullyQualifiedPathToAdd);
                                ((ClassOrInterfaceDeclaration) typeDeclaration).getImplements().add(ifaceToAdd);
                            }
                        });
                    }
                    if(CollectionUtils.isEmpty(((ClassOrInterfaceDeclaration) typeDeclaration).getImplements())) {
				        ((ClassOrInterfaceDeclaration) typeDeclaration).setImplements(null);
                    }
				}
			}

			try {
				FileUtils.writeStringToFile(originalFile, source.toString(), Charset.defaultCharset());
			} catch (IOException e) {
				e.printStackTrace();
			}


		});
	}

	public static void outputInterfaces(Collection<InterfaceDefinition> requiredInterfaces, File parentDirectory) {

        HashSet<String> outputInterfacesFullyQualifiedType = new HashSet<>();
        requiredInterfaces.forEach(def -> {
            if (StringUtils.isBlank(def.getPkg())) {
                def.setPkg("defaultpackage");
            }


            outputInterfacesFullyQualifiedType.add(def.getPkg() + "." + def.getName());
            Path outputPath = Paths.get(parentDirectory.getAbsolutePath(), def.pkg.split("\\.")).resolve(def.name);
            System.out.println(outputPath);
        });

        requiredInterfaces.forEach(def -> {
            if(def.getPkg().length()<3) {
                System.out.println();
            }

            String javaSourceCode = createJavaSourceCode(def, outputInterfacesFullyQualifiedType);
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

	private static String createJavaSourceCode(InterfaceDefinition def, Set<String> outputInterfacesFullyQualifiedType) {
		StringBuilder builder = new StringBuilder();

		// ==================== Output file structure ====================
		// package PKG;
		addPackageDeclaration(builder, def);

		// import *.*;
		addImports(builder, def);


		// // Implemented by:
		// // -> PACKAGE_1.TYPE_1
		// // -> ... ...
		// // -> PACKAGE_X.TYPE_X
		addImplementedByComments(builder, def);

		// // Implemented indirectly by:
		// // -> PACKAGE_1.TYPE_1
		// // -> ... ...
		// // -> PACKAGE_X.TYPE_X
		addImplementedIndirectlyByComments(builder, def);

		// // Extended by:
		// // -> PACKAGE_1.TYPE_1
		// // -> ... ...
		// // -> PACKAGE_Y.TYPE_Y
		addExtendedByComments(builder, def);

		// // Method signatures:
		// // public Long getLong();
		// // public String getString();
		addMethodSignatureComments(builder, def);

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
		addInterfaceDeclaration(builder, def, outputInterfacesFullyQualifiedType);
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
                .append("import java.util.Optional;\n")
                .append("import java.util.Collections;\n\n");


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

	private static void addImplementedIndirectlyByComments(StringBuilder builder, InterfaceDefinition def) {

		Set<InterfaceDefinition> extendedBy = def.getExtendedBy();

		Set<String> indirectImplementors = def.getIndirectlyImplementedBy();

		if (!indirectImplementors.isEmpty()) {
			builder.append("// Implemented indirectly by:\n");
			indirectImplementors.stream().sorted()
					.forEach(implementor -> {
				builder	.append("//")
						.append(implementor).append("\n");
			});

			builder.append("\n");
		}
	}

	private static void addExtendedByComments(StringBuilder builder, InterfaceDefinition def) {
		// XXX: PRETENDS WE DON'T HAVE AN OBVIOUS PROBLEM WITH HASHCODES BEING COMPUTED AGAINST NON-FINAL STRINGS!
		Set<InterfaceDefinition> extendedBy = new HashSet<>(def.getExtendedBy());

		if (!extendedBy.isEmpty()) {
			builder.append("// Extended by:\n");

			extendedBy
					.stream()
					.sorted((a,b)->a.toString().compareTo(b.toString()))
					.forEach(otherInterface -> {

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

	private static void addMethodSignatureComments(StringBuilder builder, InterfaceDefinition def) {

		Set<GenericMethod> allMethods = def.getAllMethods();
		if(!allMethods.isEmpty()) {
			builder.append("// Method Signatures: \n");
			allMethods.stream()
					  .map(method -> "// "+method.getMethodSignature()+"\n")
					  .sorted()
					  .forEach(builder::append);

		}
	}

	private static void addInterfaceDeclaration(StringBuilder builder, InterfaceDefinition def, Set<String> outputInterfacesFullyQualifiedType) {

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
		addChainableMethods(builder, def, outputInterfacesFullyQualifiedType);

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

			extendsList
                    .stream()
                    .filter(extended -> !StringUtils.equals(extended.getPkg(), def.getPkg()))
                    .sorted(
					    Comparator  .comparing(InterfaceDefinition::getPkg)
                                    .thenComparing(InterfaceDefinition::getName))

					.forEach(fromOtherPkg -> {
					    builder .append("\t")
                                .append(fromOtherPkg.getPkg())
                                .append(".")
                                .append(fromOtherPkg.getName())
                                .append(",\n");
                    });

			builder.setLength(builder.length() - 2); // Hack to take off the
														// last comma
			builder.append("\n");
		}
	}

	private static void addRequiredMethods(StringBuilder builder, InterfaceDefinition def) {

		def.methodSignatures.forEach(method -> {

            builder .append("\tdefault ").append(method.getMethodSignature())
                    .append(" { return "+generateDefaultReturnValue(method.getReturnTypeString())+";} \n\n");
			}
		);

		if (!def.getMethodSignatures().isEmpty()) {
			builder.append("\n");
		}
	}

	private static String generateDefaultReturnValue(String returnType) {
	    switch(returnType) {
            case "int":
            case "short":
            case "long":
            case "byte":
            case "double":
            case "float":
                return "0";

            case "char":
                return "(char)0";

            case "boolean":
                return "false";
        }
        if(returnType.startsWith("java.util.List")) {
	        return "Collections.EMPTY_LIST";
        }
        return "null";

    }

	private static void addOptionalWrappers(StringBuilder builder, InterfaceDefinition def) {



		def.methodSignatures.forEach(method -> {
		    String returnType = method.getFullyQualifiedTypeString();

		    if(Arrays.asList("int","long","boolean","char","byte","double","float", "short","").contains(returnType)) {
                return;// no reason to make optionals of something that can't be null!
            }

            String originalMethodName = method.getOriginalDeclaration().getNameExpr().toStringWithoutComments();
            String methodName =
                    StringUtils.startsWithIgnoreCase(originalMethodName, "get")
                            ? StringUtils.substringAfter(originalMethodName, "get")
                            : StringUtils.substringAfter(originalMethodName, "is");


		    builder.append("\tpublic default Optional<? extends ");



            if(returnsJaxbWrapper(method)) {
                String genericType = TypeUpdateUtility.getGenericComponent(returnType);
                if(genericType.trim().startsWith("? extends ")) {
                    builder.append(StringUtils.substringAfter(genericType, "? extends "));
                }
                else {
                    builder.append(genericType);
                }
            }
            else {
                builder.append(method.getReturnTypeString());
            }

            builder .append("> o")
                    .append(methodName)
                    .append("() { return ");

            if(returnsJaxbWrapper(method)) {
                builder.append("ComOp.oj(");
            }
            else {
                builder.append("Optional.ofNullable(");
            }
            builder .append(originalMethodName)
                    .append("());}\n\n");
		});

		if (!def.getMethodSignatures().isEmpty()) {
			builder.append("\n");
		}
	}

	private static void addChainableMethods(StringBuilder builder, InterfaceDefinition def, Set<String> outputInterfacesFullyQualifiedType) {
		// TODO: Need to work out details for automatically unwrapping various wrapper classes.

		def.getMethodSignatures().forEach(method -> {

		    if(!isChainable(method, outputInterfacesFullyQualifiedType)) {
		        return;
            }

			String originalMethodName = method.getOriginalDeclaration().getNameExpr().toStringWithoutComments();
			String chainableReturnType = getChainableReturnType(method);
			String chainableMethodBody = getChainableMethodBody(method, outputInterfacesFullyQualifiedType);

			String methodName =
                    StringUtils.startsWithIgnoreCase(originalMethodName, "get")
                    ? StringUtils.substringAfter(originalMethodName, "get")
                    : StringUtils.substringAfter(originalMethodName, "is");

			methodName = StringUtils.uncapitalize(methodName);


			builder	.append("\tpublic default ")
					.append(chainableReturnType)
					.append(" ")
					.append(methodName)
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

	private static boolean isChainable(GenericMethod method,  Set<String> outputInterfacesFullyQualifiedType) {
	    String type = method.getReturnTypeString();

        if(returnsWrapper(method)) {
            type = TypeUpdateUtility.getGenericComponent(type);
            if(TypeUpdateUtility.getAllReferencedTypes(type).size()!=1) {
                return false;
            }
        }

        return returnsListWrapper(method) ||
                returnsWrapper(method) ||
                outputInterfacesFullyQualifiedType.contains(type);


    }

    private static boolean returnsWrapper(GenericMethod method) {
        return returnsJaxbWrapper(method) || returnsOptionalWrapper(method);
    }

    private static boolean returnsJaxbWrapper(GenericMethod method) {
	    String returnType = method.getReturnTypeString();
        return  TypeUpdateUtility.getPrimaryComponent(returnType).contains("JAXBElement")
                &&
                StringUtils.isNotBlank(TypeUpdateUtility.getGenericComponent(returnType));
    }

    private static boolean returnsOptionalWrapper(GenericMethod method) {
        return TypeUpdateUtility.getPrimaryComponent(method.getReturnTypeString()).contains("java.util.Optional");
    }

    private static boolean returnsListWrapper(GenericMethod method) {
        return TypeUpdateUtility.getPrimaryComponent(method.getReturnTypeString()).contains("java.util.List");
    }

	private static String getOriginalMethodName(GenericMethod method) {
		return method.getOriginalDeclaration().getNameExpr().toStringWithoutComments();
	}

	private static boolean implementsMissing(String type, Set<String> outputInterfacesFullyQualifiedType) {
        return outputInterfacesFullyQualifiedType.contains(type);
    }

	private static String getChainableMethodBody(GenericMethod method, Set<String> outputInterfacesFullyQualifiedType) {
		String returnType = getChainableReturnType(method);
		if(returnsJaxbWrapper(method) ) {
            if(implementsMissing(returnType, outputInterfacesFullyQualifiedType)) {
                return "\t\tOptional< ? extends " + returnType + "> result = ComOp.oj(" + getOriginalMethodName(method)
                        + "());\n\t\treturn result.isPresent()?result.get() : " + returnType + ".missing();";
            }
            else {
                    return "\t\tOptional< ? extends " + returnType + "> result = ComOp.oj(" + getOriginalMethodName(method)
                            + "());\n\t\treturn result.isPresent()?result.get() : null;";
            }
        }

        if(returnsOptionalWrapper(method)) {
            return "\t\tOptional< ? extends " + returnType + "> result = Optional.ofNullable("+getOriginalMethodName(method)
                    + "());\n\t\treturn result.isPresent() && result.get().isPresent? result.get().get() : " + returnType + ".missing();";
        }

        if(returnsListWrapper(method)) {
            return "\t\tOptional< ? extends " + returnType + "> result = Optional.ofNullable("+getOriginalMethodName(method)
                    + "());\n\t\treturn result.isPresent()?result.get() : Collections.EMPTY_LIST;";
        }

        return "\t\tOptional< ? extends " + returnType + "> result = Optional.ofNullable("+getOriginalMethodName(method)
                    + "());\n\t\treturn result.isPresent()?result.get() : " + returnType + ".missing();";
	}

	private static String getChainableReturnType(GenericMethod method) {

        String type = method.getReturnTypeString();
        if(returnsListWrapper(method)) {
            return type;
        }
        if(returnsJaxbWrapper(method) || returnsOptionalWrapper(method)) {
            type = TypeUpdateUtility.getGenericComponent(type);
            return CollectionUtils.extractSingleton(TypeUpdateUtility.getAllReferencedTypes(type));
        }
        return type;

	}

	private static void addIsMissingComponent(StringBuilder builder, InterfaceDefinition def) {
		builder.append("\tpublic static final ").append(def.getName()).append(" MISSING = new ").append(def.getName())
				.append("() {};\n");

		builder.append("\tpublic static ").append(def.getName()).append(" missing() { return MISSING;}\n");

		builder.append("\tpublic default boolean isMissing() {return this==MISSING;}\n\n");
		builder.append("\tpublic default boolean isPresent() {return this!=MISSING;}\n\n");
	}

	@Data
	public static class InterfaceDefinition {

	    public boolean required = false;
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

		public Set<GenericMethod> getAllMethods() {
//			System.out.println("new getAllMethods request: " + pkg + "." + name );

			return getAllMethods(0);
		}
		private Set<GenericMethod> getAllMethods(int depth) {
			// Will break on cyclic dependencies. But so will Java itself...
			Set<GenericMethod> result = new HashSet<GenericMethod>();
			result.addAll(getMethodSignatures());

			getMustExtend().forEach(extended->result.addAll(extended.getAllMethods( depth+1)));

			if(getMustExtend().isEmpty()) {
//				System.out.println(depth);
			}
			return result;
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

		public Set<String> getIndirectlyImplementedBy() {
            return extendedBy.stream().map(id->id.getImplementedBy()).reduce(new HashSet<String>(), (identity, incoming) -> {
                identity.addAll(incoming);
                return identity;
            });
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
			return "Interface " + pkg+"."+name;
		}

	}
}
