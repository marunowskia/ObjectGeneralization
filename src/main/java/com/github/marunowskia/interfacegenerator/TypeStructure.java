package com.github.marunowskia.interfacegenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;

import lombok.Getter;

public class TypeStructure {

	public List<InterfaceDefinition> getTypeStructure(){
		// Lombok smh
		return typeStructure;
	}
	
	@Getter List<InterfaceDefinition> typeStructure = new ArrayList<>();
	
	public InterfaceDefinition add(InterfaceDefinition newInterface) {
		for(InterfaceDefinition oldInterface : typeStructure) {
			List<String> intersectingMethods = getIntersection(newInterface, oldInterface); 
			
			if(!CollectionUtils.isEmpty(intersectingMethods)) {
			
				if(intersectingMethods.size()==oldInterface.getMethodSignatures().size()) {
					if(intersectingMethods.size()==newInterface.getMethodSignatures().size()) {
						// Ensures idempotence? Maybe?
						return oldInterface;
					}
				}
				
				InterfaceDefinition sharedMethodInterface = new InterfaceDefinition();
				sharedMethodInterface.getDependencies().addAll(newInterface.getDependencies());
				sharedMethodInterface.getDependencies().addAll(oldInterface.getDependencies());
				sharedMethodInterface.name = newInterface.getName() + oldInterface.getName(); // TODO: Come up with a clever way to auto-name interfaces

				newInterface.getMethodSignatures().removeAll(intersectingMethods);
				newInterface.getMustExtend().add(sharedMethodInterface);
				
				oldInterface.getMethodSignatures().removeAll(intersectingMethods);
				oldInterface.getMustExtend().add(sharedMethodInterface);
				
				typeStructure.add(sharedMethodInterface);
			}
		}
		typeStructure.add(newInterface);	
		return newInterface;
	}
	
	private List<String> getAllMethods(InterfaceDefinition from) {
		// Will break on cyclic dependencies. But so will Java itself...
		List<String> result = new ArrayList<String>();
		result.addAll(from.getMethodSignatures());
		from.getMustExtend().forEach(extended->result.addAll(getAllMethods(extended)));
		return result;
	}
	
	private List<String> getIntersection(InterfaceDefinition newInterface, InterfaceDefinition oldInterface) {
		List<String> oldMethods = getAllMethods(oldInterface);
		List<String> newMethods = getAllMethods(newInterface);
		return oldMethods.stream().filter(newMethods::contains).collect(Collectors.toList());
	}
	
	public TypeStructure collapse() {
		
		
		boolean changesMade = false;
		do {
			for(InterfaceDefinition collapseCandidate : typeStructure) {
				
			}
		} while(changesMade);
		
		return this;
	}
	
	class MethodSignature {
		String fullyQualifiedReturnType;
		String methodName;
	}
}

