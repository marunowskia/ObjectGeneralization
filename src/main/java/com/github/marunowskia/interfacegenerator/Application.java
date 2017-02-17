package com.github.marunowskia.interfacegenerator;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.QualifiedNameExpr;
import com.google.common.base.Strings;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableValueGraph;

public class Application {

	public static void main(String args[]) {
//		Path parentPathOfTargets = Paths.get("/Users/marunal/common/uws-in-thingspace/UwsCompatibility/");
		Path parentPathOfTargets = Paths.get("/Users/marunal/workspaces/java/opensource/ObjectGeneralization/");

		File parentDirectoryOfTargets = parentPathOfTargets.toFile();

		Collection<File> allJavaFiles =  
				FileUtils.listFiles(
						parentDirectoryOfTargets, 
						FileFilterUtils.suffixFileFilter(".java"), 
						DirectoryFileFilter.DIRECTORY);
		
		allJavaFiles.forEach(System.out::println);

		// Construct type dependency graph


		MutableValueGraph<String, String> nameGraph = com.google.common.graph.ValueGraphBuilder.directed().build();
		Hashtable<String, TypeDeclaration> typeToTypeDeclaration = new Hashtable<>();

		allJavaFiles.forEach(file -> {
			try {


				CompilationUnit fileContents = JavaParser.parse(file);

				Hashtable<String, String> classToPackageMap = new Hashtable<>();
				Hashtable<String, CompilationUnit> classToCompilationMap = new Hashtable<>();

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

				if(Objects.nonNull(fileContents.getTypes())) {
					fileContents.getTypes().forEach(type -> {


						// Currently, we assume there are no nested types

						String packagePath = 
								ofNullable(fileContents)
								.map(CompilationUnit::getPackage)
								.map(PackageDeclaration::getName)
								.map(Object::toString)
								.orElse("");

						String typeName = type.getName();
						classToCompilationMap.put(packagePath + "." + typeName, fileContents);
						typeToTypeDeclaration.put(packagePath + "." + typeName, type);

						type.getMembers().forEach(member -> {
							if(member instanceof MethodDeclaration) {
								MethodDeclaration method = ((MethodDeclaration) member);

								// Only care about getters right now
								if(	method.getName().startsWith("get") 
										&& ( method.getParameters() == null || method.getParameters().isEmpty())) {

									// There is an edge from this class to another class which is named [the method's name]
									String requestingTypePath =  packagePath + "." + typeName;
									
									String returnTypePath = method.getType().toStringWithoutComments();
									
									if(classToPackageMap.containsKey(returnTypePath)) {
										returnTypePath = classToPackageMap.get(returnTypePath);
									}
									
									System.out.println("requesting class: " +requestingTypePath);
									System.out.println("return type class: " + returnTypePath);
									nameGraph.putEdgeValue(
											requestingTypePath, 
											returnTypePath,
											method.getName());
									method.getParentNode().getParentNode();
								}
							}
						});;
					});
				}
			} catch (ParseException | IOException e) {
				e.printStackTrace();
			}
		});

		// Verify that the graph is a dag
		if(Graphs.hasCycle(nameGraph)) {
			throw new IllegalArgumentException("The target directy contains a cyclic type-reference. Aborting.");
		}

		int a=0;
		while(!nameGraph.nodes().isEmpty()) {
			a++;
			List<String> leafNodes = getLeafNodes(nameGraph);

			final int b=a;
			// Convert all the non-frozen leaf classes into frozen classes.
			leafNodes.forEach(str -> {
				System.out.println(Strings.repeat("\t",  b) + b + str);
				TypeDeclaration decl = typeToTypeDeclaration.get(str);
				if(decl!=null) {
					if(decl.getMembers()!=null) {
//						decl.getMembers().forEach(member -> {System.out.println("member: " + member);});
					}
//					System.out.println(Strings.repeat("\t",  b) + decl.toString());
				}
				nameGraph.removeNode(str);
			});
		}
	}

	private static List<String> getLeafNodes(MutableValueGraph<String, String> graph) {
		return graph
				.nodes()
				.stream()
				.filter(s -> graph.successors(s).isEmpty())
				.collect(Collectors.toList());
	}
}
