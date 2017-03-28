package com.github.marunowskia.interfacegenerator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraph;

public class InterfaceDefinitionConstraintSolver { 

	public static Set<InterfaceDefinition> buildResult(ValueGraph<String, List<GenericMethod>> methodGraph) {
		Result result = new Result();
		MutableValueGraph<String, List<GenericMethod>> methodGraphCopy = Graphs.inducedSubgraph(methodGraph, methodGraph.nodes()); // Raw copy of methodGraph
		while(!CollectionUtils.isEmpty(methodGraphCopy.nodes())) {
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
				
				returnedTypes.forEach(type -> {
					List<GenericMethod> methodSignatures = methodGraph.edgeValue(leafType, type);
					identicalInterface.getMethodSignatures().addAll(methodSignatures);
				});
				
				result.include(identicalInterface);
			});
		}
		return result.getFinalDefinitions();
	}

	private static List<String> getLeafNodes(MutableValueGraph<String, List<GenericMethod>> graph) {
		return graph
				.nodes()
				.stream()
				.filter(s -> graph.successors(s).isEmpty())
				.collect(Collectors.toList());
	}

}
