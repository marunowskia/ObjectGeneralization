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
		if(!checkForPassthroughInterface(incoming)) return;
		checkForOverlap(incoming);
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
			return false;
		}
		
		return true;
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
			
			Set<GenericMethod> overlap = getIntersection(newInterface, existingInterface);
			if(CollectionUtils.isNotEmpty(overlap)) {

				if(CollectionUtils.isSubCollection(getAllMethods(existingInterface), getAllMethods(newInterface))) {
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
		overlapResolutions.forEach(this::checkForTotalOverlap);
		overlapResolutions.stream().filter(this::checkForPassthroughInterface).forEach(finalDefinitions::add);
	}

	private volatile int interfaceCounter = 1;
	private String selectNameForSharedInterface(InterfaceDefinition newInterface, InterfaceDefinition existingInterface) {
		return "I" + interfaceCounter++;
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
		Set<GenericMethod> oldTypes = new HashSet<>(incoming.getMethodSignatures());
		incoming.getMethodSignatures().clear();
		
		// We change the value used to compute equals and hash. Big nono! Should probably fix this ick.
		oldTypes.forEach(sig -> sig.updateMethodSignature(implementorMap));
		incoming.getMethodSignatures().addAll(oldTypes);
	}

	private Map<String, InterfaceDefinition> createImplementorMap() {
		Map<String, InterfaceDefinition> implementorMap = new HashMap<>();
		finalDefinitions.forEach(definition -> {
			definition.getImplementedBy().forEach(implementor -> {
				implementorMap.put(
						StringUtils.substringAfterLast(
								implementor
								, ".")
						, definition);
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
}
