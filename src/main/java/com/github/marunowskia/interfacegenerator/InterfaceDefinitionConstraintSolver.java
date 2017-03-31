package com.github.marunowskia.interfacegenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;
import com.google.common.io.Files;

public class InterfaceDefinitionConstraintSolver { 

	public static Set<InterfaceDefinition> buildResult(ValueGraph<String, List<GenericMethod>> methodGraph) {
//		Result resultV1 = new Result();
		ResultConstructor resultV2 = new ResultConstructor();
		
		MutableValueGraph<String, List<GenericMethod>> methodGraphCopy = Graphs.inducedSubgraph(methodGraph, methodGraph.nodes()); // Raw copy of methodGraph

		Set<InterfaceDefinition> currentResult = new HashSet<>();
		while(!CollectionUtils.isEmpty(methodGraphCopy.nodes())) {
			
			Collection<InterfaceDefinition> leafNodes = new ArrayList<>();
			getLeafNodes(methodGraphCopy).forEach(leafType -> {
				methodGraphCopy.removeNode(leafType);
				
				Set<String> returnedTypes = methodGraph.successors(leafType);
				
				if(returnedTypes.isEmpty()) {
					return;
				}
				
				InterfaceDefinition identicalInterface = new InterfaceDefinition();
				identicalInterface.setName("I"+StringUtils.substringAfterLast(leafType, "."));
				identicalInterface.setPkg(StringUtils.substringBeforeLast(leafType, "."));
				identicalInterface.getImplementedBy().add(leafType);
//				identicalInterface.getDependencies().addAll(leafType.);
				
				
				returnedTypes.forEach(type -> {
					List<GenericMethod> methodSignatures = methodGraph.edgeValue(leafType, type);
					identicalInterface.getMethodSignatures().addAll(methodSignatures);
				});
				
				leafNodes.add(identicalInterface);
			});
			leafNodes.addAll(currentResult);
			currentResult = resultV2.buildResult(leafNodes);
//			InterfaceComposer.outputInterfaces(currentResult, Files.createTempDir());
		}
		
		return currentResult;//resultV1.getFinalDefinitions();
	}

	private static List<String> getLeafNodes(MutableValueGraph<String, List<GenericMethod>> graph) {
		return graph
				.nodes()
				.stream()
				.filter(s -> graph.successors(s).isEmpty())
				.collect(Collectors.toList());
	}

}
