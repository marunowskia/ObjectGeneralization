//package com.github.marunowskia.interfacegenerator;
//
//import java.util.Set;
//
//import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
//import com.google.common.collect.Lists;
//import com.google.common.collect.Sets;
//
//public class TestObjectSupplier {
//
//	public static InterfaceDefinition getInterface(String name) {
//		InterfaceDefinition singleInterface = new InterfaceDefinition();
//
//
//		
//		singleInterface.pkg = "com.github.marunowskia.interfacegenerator";
//		
//
//		singleInterface.dependencies = Lists.newArrayList(
//				"java.util.Collection",
//				"java.io.File",
//				"java.io.IOException",
//				"java.nio.file.Path",
//				"java.nio.file.Paths",
//				"java.util.List",
//				"java.util.stream.Collectors",
//				"org.junit.Test",
//				"com.github.javaparser.ast.stmt.AssertStmt",
//				"com.google.common.io.Files",
//				"org.junit.Assert"
//				);
//		
//		singleInterface.name = name;
//		singleInterface.genericParameters = Lists.newArrayList(
//				"E"
//				);
//		
//		singleInterface.mustExtend = Lists.newArrayList();
//
//		singleInterface.methodSignatures = Sets.newHashSet(
//				"boolean add(E arg0)",
//				"boolean remove(Object arg0)",
//				"boolean containsAll(Collection<?> arg0)",
//				"boolean addAll(Collection<? extends E> arg0)",
//				"boolean addAll(int arg0, Collection<? extends E> arg1)",
//				"boolean removeAll(Collection<?> arg0)",
//				"boolean retainAll(Collection<?> arg0)"
//				);
//		
//		return singleInterface;
//	}
//}
