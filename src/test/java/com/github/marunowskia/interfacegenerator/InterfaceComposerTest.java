package com.github.marunowskia.interfacegenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class InterfaceComposerTest {

	@Test
	public void outputInterfacesTest() throws IOException {  

		Path parentDirectoryPath = Paths.get("/tmp/ObjectGeneralization.InterfaceComposerTest.OutputInterfacesTest");
		File parentDirectory = parentDirectoryPath.toFile();

		Assert.assertTrue(parentDirectory.mkdir()||parentDirectory.exists());

		InterfaceDefinition singleInterface = new InterfaceDefinition();


		
		singleInterface.pkg = "com.github.marunowskia.interfacegenerator";
		

		singleInterface.dependencies = Lists.newArrayList(
				"java.util.Collection",
				"java.io.File",
				"java.io.IOException",
				"java.nio.file.Path",
				"java.nio.file.Paths",
				"java.util.List",
				"java.util.stream.Collectors",
				"org.junit.Test",
				"com.github.javaparser.ast.stmt.AssertStmt",
				"com.google.common.io.Files",
				"org.junit.Assert"
				);
		
		singleInterface.name = "IFakeInterface";
		singleInterface.genericParameters = Lists.newArrayList(
				"E"
				);
		
		singleInterface.mustExtend = Lists.newArrayList();

		singleInterface.methodSignatures = Sets.newHashSet(
				"boolean add(E arg0)",
				"boolean remove(Object arg0)",
				"boolean containsAll(Collection<?> arg0)",
				"boolean addAll(Collection<? extends E> arg0)",
				"boolean addAll(int arg0, Collection<? extends E> arg1)",
				"boolean removeAll(Collection<?> arg0)",
				"boolean retainAll(Collection<?> arg0)"
				);

		InterfaceComposer.outputInterfaces(Lists.newArrayList(singleInterface), parentDirectory);

		Assert.assertTrue(parentDirectory.listFiles().length!=0);
		Files.walk(parentDirectoryPath, 100, FileVisitOption.FOLLOW_LINKS).filter(path->path.toFile().isFile()).forEach(path -> {
			try {
				CompilationUnit fileContents = JavaParser.parse(path.toFile());
				Assert.assertEquals(singleInterface.dependencies.size(), fileContents.getImports().size());
				int expectedNumberOfMethods = fileContents.getTypes().get(0).getMembers().size();
				Assert.assertEquals(singleInterface.methodSignatures.size(), expectedNumberOfMethods);
				// TODO: theses asserts need to perform more detailed checks. right now we're basically just making the right number of components made it in
				
				System.out.println("\n\n"+fileContents.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
