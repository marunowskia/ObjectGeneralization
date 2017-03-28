package com.github.marunowskia.interfacegenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;

public class InterfaceDefinitionConstraintSolver {

	public static List<InterfaceDefinition> satisfyConstraints(ValueGraph<String, List<MethodDeclaration>> methodGraph) {
		List<Hashtable<String,InterfaceDefinition>> interfaceTiers = buildInterfaceDefinitions(methodGraph);
		Map<String, InterfaceDefinition> typeToInterfaceMap = new Hashtable<>();
		Structure structure = new Structure();
		interfaceTiers.forEach(tier->tier.forEach((name,topLevelInterface) -> {
			topLevelInterface = structure.add(topLevelInterface, Collections.singletonList(name)); // name must be fully qualified!
			structure.collapse();
			typeToInterfaceMap.clear();
			structure.getImplementingTypes().forEach((k,v)-> {
				v.forEach(implementingType -> {
					typeToInterfaceMap.put(StringUtils.substringAfterLast(implementingType, "."), k);
					typeToInterfaceMap.put(implementingType, k);
				});
			});
			typeToInterfaceMap.put(StringUtils.substringAfterLast(name, "."), topLevelInterface);
			typeToInterfaceMap.put(name, topLevelInterface);
		}));
		structure.getImplementingTypes().forEach((def, implementors) -> {
			System.out.println("The types" + implementors + " implement interface " + def.getName());
			System.out.println("The interface " + def.getName() + " covers these methods: " + structure.getAllMethods(def));
			System.out.println("The interface " + def.getName() + " adds these methods: " + def.getMethodSignatures().keySet());
			System.out.println("The interface " + def.getName() + " extends these interfaces: " + def.getMustExtend().stream().map(i->i.getName()).collect(Collectors.toList()));
			System.out.println();
		});
		structure.collapse();
		return structure.getStructureContents();
				
	}
	// from https://commons.apache.org/sandbox/commons-classscan/xref/org/apache/commons/classscan/builtin/ClassNameHelper.html
	private static final String IDENTIFIER_REGEX = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
	
	private static List<Hashtable<String,InterfaceDefinition>> buildInterfaceDefinitions(ValueGraph<String, List<MethodDeclaration>> methodGraph) {
	List<Hashtable<String,InterfaceDefinition>> result = new ArrayList<>();
	MutableValueGraph<String, List<MethodDeclaration>> methodGraphCopy = Graphs.inducedSubgraph(methodGraph, methodGraph.nodes()); // Raw copy of methodGraph
	while(!CollectionUtils.isEmpty(methodGraphCopy.nodes())) {
		Hashtable<String,InterfaceDefinition> requiredInterfaces = new Hashtable<>();
		getLeafNodes(methodGraphCopy).forEach(leafType -> {
			
			Set<String> returnedTypes = methodGraph.successors(leafType);
			final InterfaceDefinition assignedInterface = new InterfaceDefinition();
			if(leafType.contains("Middle23")) {
				System.out.println("Something fishy");
			}
			assignedInterface.setName("I"+StringUtils.substringAfterLast(leafType, "."));
			assignedInterface.setPkg(StringUtils.substringBeforeLast(leafType, "."));
			returnedTypes.forEach(type -> {
				List<MethodDeclaration> methodSignatures = methodGraph.edgeValue(leafType, type);
				methodSignatures.forEach(signature-> {
					assignedInterface.getMethodSignatures().put(new StringBuilder().append("public ").append(type).append(" ").append(signature.getName()).append("()").toString(), signature);
					
				});
				System.out.println(assignedInterface.getMethodSignatures());
				requiredInterfaces.put(leafType, assignedInterface);
				requiredInterfaces.values();
			});
			methodGraphCopy.removeNode(leafType);
		});
		result.add(requiredInterfaces);
		
	}
	
	
	return result;//assignedInterfaceActual.values();
 }
	
	private static List<String> getLeafNodes(MutableValueGraph<String, List<MethodDeclaration>> graph) {
		return graph
				.nodes()
				.stream()
				.filter(s -> graph.successors(s).isEmpty())
				.collect(Collectors.toList());
	}

}
