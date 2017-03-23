package com.github.marunowskia.interfacegenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;

public class InterfaceDefinitionConstraintSolver {

	public static List<InterfaceDefinition> satisfyConstraints(ValueGraph<String, List<String>> methodGraph) {
		// Constraints:
		// 0) Each class may must implement exactly one of the created interfaces.
		// 1) All methods callable using a reference to the original object must be callable using a reference to the interface version of that object.
		// 2) Any type reference which can be replaced with an equivalent reference must be replaced.
		// 3) No method signature may occur in more than one InterfaceDefintion of the FINAL return value.
		// 4) Genericized methods in the original type definition must be genericized in its InterfaceDefintion in the FINAL return value.
		// 5) Mergeable InterfaceDefintion objects must be merged. Two InterfaceDefinitions may be merged if they contain the same method signatures and extend set of InterfaceDefinitions
		
		// Step 1: Create an InterfaceDefinition for each TypeDefinition from methodGraph.
		List<Hashtable<String,InterfaceDefinition>> interfaceTiers = buildInterfaceDefinitions(methodGraph);
		

		// Contstraint 0:
		Map<String, InterfaceDefinition> typeToInterfaceMap = new Hashtable<>();
		
		
		Structure structure = new Structure();
		// Step 2: Replace type references with their equivalent interfaces. 
		interfaceTiers.forEach(tier->tier.forEach((name,topLevelInterface) -> {
			// Since this is a leaf interface, we know that the return types cannot refer to any of the other types in this tier, 
			// so we are guaranteed to have all necessary information to update the return types.
			topLevelInterface.setMethodSignatures(
				topLevelInterface.getMethodSignatures()
						.stream()
						.map(signature -> updateSignature(signature, typeToInterfaceMap))
						.collect(Collectors.toList())
				);
			topLevelInterface = structure.add(topLevelInterface, Collections.singletonList(name)); // name must be fully qualified!
			structure.collapse();
			
			// TODO: replace this with one-line version
			typeToInterfaceMap.clear();
			structure.getImplementingTypes().forEach((k,v)-> {
				v.forEach(implementingType -> {
					typeToInterfaceMap.put(implementingType, k);
				});
			});
			
			typeToInterfaceMap.put(name, topLevelInterface);
		}));
		structure.collapse();
		return structure.getStructureContents();
				
	}
	
	
	private static String updateSignature(String methodSignature, Map<String, InterfaceDefinition> replacements) {
		// Initial implementation only cares about return types.
		
		// XXX: Replace fake logic for figuring out the method return type with a tested open source library.
		
		String paredSignature = StringUtils.normalizeSpace(methodSignature);
		
		//public ReturnType methodName(); ==> ReturnType methodName();
		if(paredSignature.startsWith("public")) {
			paredSignature = paredSignature.replaceFirst("public", "");
		}
		
		//ReturnType methodName' '?(.* ==> returnType methodName
		paredSignature = StringUtils.substringBefore(paredSignature, "(")
						  .trim(); // Remove possible space after methodName
		
		String returnType = StringUtils.substringBefore(paredSignature, " ");
		String updatedReturnType = TypeUpdateUtility.updateType(returnType, replacements);
		return methodSignature.replaceFirst(returnType, updatedReturnType);
	}
	
	// from https://commons.apache.org/sandbox/commons-classscan/xref/org/apache/commons/classscan/builtin/ClassNameHelper.html
	private static final String IDENTIFIER_REGEX = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
	private static Pattern IDENTIFIER_PACKAGE = Pattern.compile(IDENTIFIER_REGEX);


	
	
	private static List<Hashtable<String,InterfaceDefinition>> buildInterfaceDefinitions(ValueGraph<String, List<String>> methodGraph) {
	List<Hashtable<String,InterfaceDefinition>> result = new ArrayList<>();
	MutableValueGraph<String, List<String>> methodGraphCopy = Graphs.inducedSubgraph(methodGraph, methodGraph.nodes()); // Raw copy of methodGraph
	while(!CollectionUtils.isEmpty(methodGraphCopy.nodes())) {
		Hashtable<String,InterfaceDefinition> requiredInterfaces = new Hashtable<>();
		getLeafNodes(methodGraphCopy).forEach(leafType -> {
			Set<String> returnedTypes = methodGraph.successors(leafType);
			final InterfaceDefinition assignedInterface = new InterfaceDefinition();
			assignedInterface.setName("I"+StringUtils.substringAfterLast(leafType, "."));
			assignedInterface.setPkg(StringUtils.substringBeforeLast(leafType, "."));
			returnedTypes.forEach(type -> {
				List<String> methodSignatures = methodGraph.edgeValue(leafType, type);
				// Generate the return type:
				methodSignatures.stream().map(name -> new StringBuilder().append("public ").append(type).append(" ").append(name).append("()").toString());
				assignedInterface.getMethodSignatures().addAll(methodSignatures);
				requiredInterfaces.put(leafType, assignedInterface);
				requiredInterfaces.values();
			});
			methodGraphCopy.removeNode(leafType);
		});
		result.add(requiredInterfaces);
	}
	
	
	return result;//assignedInterfaceActual.values();
 }
	
	private static List<String> getLeafNodes(MutableValueGraph<String, List<String>> graph) {
		return graph
				.nodes()
				.stream()
				.filter(s -> graph.successors(s).isEmpty())
				.collect(Collectors.toList());
	}

}
