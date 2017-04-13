package com.github.marunowskia.interfacegenerator;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.QualifiedNameExpr;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableValueGraph;
import com.google.common.io.Files;

public class Application {

	public static void main(String args[]) {
		// The input directory will be
		// The set of input files is effectively the unix command:
		// find $pathToInputDirectory -iname '*.java'`
		Path inputDirectory = Paths.get("/path/to/input/");

		// File outputDirectory 		= Files.createTempDir();
		Path outputDirectory = Paths.get("/path/to/output");

		// This flag determines whether the original *.java files should be updated to implement the newly created interfaces
		boolean updateOriginalFiles = true;

		unifyTypeHierarchy(inputDirectory, outputDirectory, updateOriginalFiles);
	}

	public static void unifyTypeHierarchy(Path inputDirectory, Path outputDirectory, boolean modifyInputFiles) {
		Collection<File> allJavaFiles =  new ArrayList<>();

		Collection<File> desiredDirs = 
		FileUtils.listFilesAndDirs(
				inputDirectory.toFile(),
				FileFilterUtils.falseFileFilter(), 
				FileFilterUtils.prefixFileFilter("SoapContract")
			);

		System.out.println(desiredDirs);
		desiredDirs.forEach(dir -> {
					allJavaFiles.addAll(
						FileUtils.listFiles(
							dir,
							FileFilterUtils.suffixFileFilter(".java"), 
							DirectoryFileFilter.DIRECTORY)
					);
				});

		allJavaFiles.forEach(System.out::println);

		// Construct type dependency graph
		MutableValueGraph<String, List<GenericMethod>> methodGraph = com.google.common.graph.ValueGraphBuilder.directed().allowsSelfLoops(false).build();

		HashMap<String, TypeDeclaration> typeToTypeDeclaration = new HashMap<>();
		HashMap<String, CompilationUnit> classToCompilationMap = new HashMap<>();

		HashMap<String, Set<String>> packageContents = createPackageToClassMap(allJavaFiles);

		allJavaFiles.forEach(file -> {
			try {



				CompilationUnit fileContents = JavaParser.parse(file);

				fileContents.setData(file);

				Hashtable<String, String> classToPackageMap = mapTypeToFullyQualifiedType(packageContents, fileContents);


				HashMap<String, File> originalFiles = new HashMap<>();
				Hashtable<String, List<GenericMethod>> requestReturnTypeMethods = new Hashtable<>();
				createPathToTypeMap(fileContents).forEach((path, type) -> {
						// Currently, we assume there are no nested types
						classToCompilationMap.put(path, fileContents);
						typeToTypeDeclaration.put(path, type);

						if(type.getMembers()!=null)
						type.getMembers().forEach(member -> {
							if(member instanceof MethodDeclaration) {
								MethodDeclaration method = ((MethodDeclaration) member);

								// Only care about getters right now
								boolean methodHasNoParameters = CollectionUtils.isEmpty(method.getParameters());
								boolean methodIsNotVoid = !method.getType().toStringWithoutComments().contains("void");
								boolean methodIsGetter = method.getName().startsWith("get") || method.getName().startsWith("is");

								if(methodHasNoParameters && methodIsNotVoid && methodIsGetter) {

										// There is an edge from this class to another class which is named [the method's name]
									Set<String> returnTypePaths = TypeUpdateUtility.getAllReferencedTypes(method.getType().toStringWithoutComments());

									returnTypePaths.forEach(returnTypePath -> {
										if(classToPackageMap.containsKey(returnTypePath)) {
											returnTypePath = classToPackageMap.get(returnTypePath);
										}

										if(path.equals(returnTypePath)) {
											System.out.println("Error. Self loop");
										}

										String key = path + "|" + returnTypePath;

										GenericMethod newDeclaration = new GenericMethod();
										newDeclaration.setOriginalDeclaration(method, classToPackageMap);

										List<GenericMethod> methodList = requestReturnTypeMethods.getOrDefault(key, Lists.newArrayList());
										requestReturnTypeMethods.put(key, methodList);

										methodList.add(newDeclaration);
									});
								}
							}
						});;
					});

					requestReturnTypeMethods.keySet().forEach(key-> {
						List<GenericMethod> methods = requestReturnTypeMethods.get(key);
						String fromVertex = StringUtils.substringBefore(key, "|");
						fromVertex.hashCode();
						String toVertex = StringUtils.substringAfter(key, "|");
						methodGraph.putEdgeValue(
								fromVertex,
								toVertex,
								methods);
					});

			} catch (ParseException | IOException e) {
				e.printStackTrace();
			}
		});

		// Verify that the graph is a dag
		if(Graphs.hasCycle(methodGraph)) {
			throw new IllegalArgumentException("The target directy contains a cyclic type-reference. Aborting.");
		}

		InterfaceComposer.generateAndExportInterfaces(methodGraph, outputDirectory.toFile(), classToCompilationMap, modifyInputFiles);
	}

	private static Hashtable<String, String> mapTypeToFullyQualifiedType(HashMap<String, Set<String>> packageContents, CompilationUnit fileContents) {

		String pkg = getPkg(fileContents);
		Hashtable<String, String> classToPackageMap = new Hashtable<>();

		if(fileContents.getImports()!=null) {
            fileContents.getImports().forEach(importDecl -> {
                String className 	= importDecl.getName().getName();
                String packageName  = "";
                NameExpr importName = importDecl.getName();
                while(importName != null && importName instanceof QualifiedNameExpr) {
                    packageName = ((QualifiedNameExpr) importName).getQualifier().getName() + "." + packageName; // ends with a period, assuming the import target is qualified with a package
                    importName = ((QualifiedNameExpr) importName).getQualifier();
                }
                classToPackageMap.put(className, packageName  + className);
            });
        }

		Set<String> knownTypes = packageContents.get(pkg);

		for (String knownType : knownTypes) {

            // This is broken, and needs to be reworked.
            classToPackageMap.put(StringUtils.substringAfter(knownType, pkg+"."), knownType); // Fully qualified reference to inner types
            classToPackageMap.put(StringUtils.substringAfterLast(knownType, "."), knownType); // Unqualified references to inner types.

        }
		return classToPackageMap;
	}

	private static HashMap<String,Set<String>> createPackageToClassMap(Collection<File> allJavaFiles) {
		HashMap<String, Set<String>> packageContents = new HashMap<>();
		allJavaFiles.forEach(file -> {
			try {
				CompilationUnit fileContents = JavaParser.parse(file);
				String pkg = getPkg(fileContents);

				if(!packageContents.containsKey(pkg)) {
					packageContents.put(pkg, new HashSet<>());
				}
				Set<String> knownTypes = packageContents.get(pkg);

				createPathToTypeMap(fileContents).keySet().forEach(knownTypes::add);

			} catch (ParseException | IOException e) {
				e.printStackTrace();
			}
		});

		return packageContents;
	}


	private static Map<String, TypeDeclaration> createPathToTypeMap(@NonNull CompilationUnit fromCompilationUnit) {
		HashMap<String, TypeDeclaration> result = new HashMap<>();

		String pkg = getPkg(fromCompilationUnit);

		if(fromCompilationUnit.getTypes()==null) {
			System.out.println("NULL COMPILATION UNIT TYPES: " + fromCompilationUnit.toString());
		}
		else {
			fromCompilationUnit.getTypes().forEach(typeDeclaration -> {

				if(typeDeclaration instanceof ClassOrInterfaceDeclaration) {
					ClassOrInterfaceDeclaration decl = (ClassOrInterfaceDeclaration) typeDeclaration;

					if(decl.isInterface()) {
						return;
					}

					if(CollectionUtils.isNotEmpty(decl.getExtends())) {
						return;
					}
				}
				String typePath = pkg + typeDeclaration.getName();
				result.put(typePath, typeDeclaration);
				addSubtypes(typeDeclaration, result, typePath + ".");
			});
		}
		return result;
	}

	private static void addSubtypes(@NonNull TypeDeclaration fromType, @NonNull HashMap<String, TypeDeclaration> toMap, @NonNull String typePath) {
		fromType.getChildrenNodes().forEach(subnode -> {
			if(subnode instanceof ClassOrInterfaceDeclaration) {
				ClassOrInterfaceDeclaration subtype = (ClassOrInterfaceDeclaration) subnode;
				String subtypePath = typePath+subtype.getName();
				toMap.put(subtypePath, subtype);
				addSubtypes(subtype, toMap, subtypePath+".");
			}
		});
	}

	private static String getPkg(CompilationUnit compilationUnit) {
		return  ofNullable(compilationUnit.getPackage())
				.map(PackageDeclaration::getName)
				.map(Object::toString)
				.map(str -> str +".")
				.orElse("");
	}
}