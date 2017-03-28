package com.github.marunowskia.interfacegenerator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;

public class Result {

	Set<InterfaceDefinition> finalDefinitions = new HashSet<>();

	public void include(InterfaceDefinition incoming) {
		updateReturnTypes(incoming);

		checkForTotalOverlap(incoming);
		if(checkForPassthroughInterface(incoming)) return;
		
		checkForOverlap(incoming);
//		checkForSplitInterfaces();
//		checkForDuplicates();

		finalDefinitions.add(incoming);
		updateAllReturnTypes();
	}
	
	private boolean checkForPassthroughInterface(InterfaceDefinition incoming) {
		if(incoming.getMustExtend().size()==1 && incoming.getMethodSignatures().isEmpty()) {
			InterfaceDefinition incomingExtends = CollectionUtils.extractSingleton(incoming.getMustExtend());
			incomingExtends.getImplementedBy().addAll(incoming.getImplementedBy());
			incoming.getExtendedBy().forEach(incomingExtendedBy -> {
				incomingExtendedBy.getMustExtend().remove(incoming);
				incomingExtendedBy.getMustExtend().add(incomingExtends);
			});
			finalDefinitions.remove(incoming);
			return true;
		}
		
		return false;
	}
	
	private void checkForTotalOverlap(InterfaceDefinition incoming) {
		finalDefinitions.forEach(other -> {
			Set<GenericMethod> allMethodsFromOther = getAllMethods(other);
			if(CollectionUtils.isSubCollection( allMethodsFromOther, incoming.getMethodSignatures())) {

				incoming.getMustExtend()
				        .add(other);
				incoming.getMethodSignatures()
					    .removeAll(allMethodsFromOther);

				other.getExtendedBy().add(incoming);
			}
		});
	}
	
	private void checkForOverlap(InterfaceDefinition newInterface) {
		Set<InterfaceDefinition> overlapResolutions = new HashSet<>();
		for(InterfaceDefinition existingInterface : finalDefinitions) {
			
			if(CollectionUtils.isEmpty(newInterface.getMethodSignatures())) return; // This introduced undesirable dependence of the final outcome on the order that input got passed in. 
			
			Set<GenericMethod> overlap = getIntersection(newInterface, existingInterface);

			if(CollectionUtils.isNotEmpty(overlap)) {

				if(CollectionUtils.isSubCollection(newInterface.getMethodSignatures(), overlap)) {
					existingInterface.getExtendedBy().addAll(newInterface.getExtendedBy());
					newInterface.getMustExtend().add(existingInterface);
					newInterface.getMethodSignatures().removeAll(overlap);
					continue;
				}

				InterfaceDefinition sharedMethodInterface = new InterfaceDefinition();
				sharedMethodInterface.getDependencies().addAll(newInterface.getDependencies());
				sharedMethodInterface.getDependencies().addAll(existingInterface.getDependencies());


				sharedMethodInterface.setPkg(existingInterface.getPkg());
				sharedMethodInterface.setName(selectNameForSharedInterface(newInterface, existingInterface));
				sharedMethodInterface.setMethodSignatures(overlap);
				sharedMethodInterface.getExtendedBy().add(newInterface);
				sharedMethodInterface.getExtendedBy().add(existingInterface);
				overlapResolutions.add(sharedMethodInterface);


				newInterface.getMethodSignatures().removeAll(overlap);
				newInterface.getMustExtend().add(sharedMethodInterface);

				existingInterface.getMethodSignatures().removeAll(overlap);
				existingInterface.getMustExtend().add(sharedMethodInterface);				
			}
		}		
		finalDefinitions.addAll(overlapResolutions);
	}

	private volatile int interfaceCounter = 1;
	private String selectNameForSharedInterface(InterfaceDefinition newInterface, InterfaceDefinition existingInterface) {
		return "ICom" + interfaceCounter;
//		String commonPrefix = StringUtils.getCommonPrefix(newInterface.getName(), existingInterface.getName()); // TODO: Come up with a clever way to auto-name interfaces
//		String remainingNew = StringUtils.substringAfter(newInterface.getName(), commonPrefix);
//		String remainingOld = StringUtils.substringAfter(existingInterface.getName(), commonPrefix);
//
//		String selectedName;
//		if(remainingNew.compareTo(remainingOld) < 0) {
//			selectedName = commonPrefix + "_" + remainingNew + "_" + remainingOld;
//		}
//		else {
//			selectedName = commonPrefix + "_" + remainingOld + "_" + remainingNew;
//		}
//		return selectedName;
	}

	private void updateAllReturnTypes() {
		Map<String, InterfaceDefinition> implementorMap = createImplementorMap();
		finalDefinitions.forEach(existing -> {
			updateReturnTypes(existing, implementorMap);
		});
	}
	
	private void updateReturnTypes(InterfaceDefinition incoming) {
		Map<String, InterfaceDefinition> implementorMap = createImplementorMap();
		updateReturnTypes(incoming, implementorMap);
	}
	
	private void updateReturnTypes(InterfaceDefinition incoming, Map<String, InterfaceDefinition> implementorMap) {
		incoming.getMethodSignatures().forEach(sig -> sig.updateMethodSignature(implementorMap));
	}

	private Map<String, InterfaceDefinition> createImplementorMap() {
		Map<String, InterfaceDefinition> implementorMap = new HashMap<>();
		finalDefinitions.forEach(definition -> {
			definition.getImplementedBy().forEach(implementor -> {
				implementorMap.put(implementor, definition);
			});
		});
		return implementorMap;
	}

	private Set<GenericMethod> getAllMethods(InterfaceDefinition from) {
		// Will break on cyclic dependencies. But so will Java itself...
		Set<GenericMethod> result = new HashSet<GenericMethod>();
		result.addAll(from.getMethodSignatures());
		from.getMustExtend().forEach(extended->result.addAll(getAllMethods(extended)));
		return result;
	}

	private Set<GenericMethod> getIntersection(InterfaceDefinition newInterface, InterfaceDefinition oldInterface) {
		Set<GenericMethod> oldMethods = getAllMethods(oldInterface);
		Set<GenericMethod> newMethods = getAllMethods(newInterface);
		return oldMethods.stream().filter(newMethods::contains).collect(Collectors.toSet());
	}

	public Set<InterfaceDefinition> getFinalDefinitions() {
		return finalDefinitions;
	}
	
	//	public void collapse() {
	//		// An interface may recombine with another interface IFF the resultant structure
	//		// does not violate the ability for each type in "implementingTypes" to implement the recombined interface.
	//
	//		// This occurs in two scenarios:
	//
	//		// ========================================
	//		// 1) If interfaces A, B are under consideration, A,B can be merged if the following two sets are equivalent:
	//		//          Set 1: {type|type extends A or type implements A}
	//		//          Set 2: {type|type extends B or type implements B}
	//		// ========================================
	//
	//		HashMap<InterfaceDefinition, InterfaceDefinition> replacementPairs = new HashMap<>();
	//		for(int a=0; a<structureContents.size(); a++) {
	//			InterfaceDefinition interfaceA = structureContents.get(a);
	//			List<String> extendsOrImplementsA = implementingTypes.getOrDefault(interfaceA, new ArrayList<String>());
	//			if(!isEmpty(extendsOrImplementsA)) {
	//				for(int b=a+1; b<structureContents.size(); b++) {
	//					InterfaceDefinition interfaceB = structureContents.get(b);
	//					List<String> extendsOrImplementsB = implementingTypes.getOrDefault(interfaceB, new ArrayList<String>());
	//					if(isEqualCollection(extendsOrImplementsA, extendsOrImplementsB)) {
	//						replacementPairs.put(interfaceA, interfaceB);
	//					}
	//				}
	//			}
	//		}
	//		replaceAll(replacementPairs);
	//		replacementPairs.clear();
	//
	//		// ========================================
	//		// 2) If interfaces A, B are under consideration, then A only extends B, and A declares no additional methods
	//		// ========================================
	//		for(InterfaceDefinition definition : structureContents) {
	//			if(size(definition.getMustExtend()) == 1) {
	//				if(isEmpty(definition.getMethodSignatures())) {
	//					replacementPairs.put(
	//							definition, 
	//							CollectionUtils.extractSingleton(definition.getMustExtend()));
	//				}
	//			}
	//		}
	//		replaceAll(replacementPairs);
	//	}
}
