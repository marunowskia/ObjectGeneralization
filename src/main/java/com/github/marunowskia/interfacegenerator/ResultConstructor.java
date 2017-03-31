package com.github.marunowskia.interfacegenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResultConstructor {

	public Set<InterfaceDefinition> buildResult(Collection<InterfaceDefinition> originalInterfaces) {
		if(CollectionUtils.isEmpty(originalInterfaces)) {
			return new HashSet<>();
		}  
		updateAllMethodSignatures(originalInterfaces);
		Set<InterfaceDefinition> allSharedMethods = getAllCommonalities(originalInterfaces);
		addAllValidExtends(allSharedMethods);
		allSharedMethods.forEach(this::pruneRedundantSuperclasses);
		
		
		allSharedMethods.forEach(def -> {
			if(def.getMethodSignatures().size()==1 && def.getName().startsWith("IFace")) {
				
				GenericMethod onlyMethod = CollectionUtils.extractSingleton(def.getMethodSignatures());
				String newName = 
						"I" 
						+ String.join("", onlyMethod.getOriginalDeclaration().getType().toStringWithoutComments().replaceAll("[^A-Za-z_$0-9]", "_")) 
						+ onlyMethod.getOriginalDeclaration().getName().substring(3);
				def.setName(newName);
			}
		});
		
		updateAllMethodSignatures(allSharedMethods);
		return allSharedMethods;
	}

	private Set<InterfaceDefinition> getAllCommonalities(Collection<InterfaceDefinition> originalInterfaces) {
		Set<InterfaceDefinition> allCommonalities = new HashSet<>();
		allCommonalities.addAll(originalInterfaces);

		Collection<InterfaceDefinition> addedInPreviousIteration = originalInterfaces;
		int startingSize = 0;
		do {
			startingSize = allCommonalities.size();
			Collection<InterfaceDefinition> intersection = new ArrayList<>();
			for(InterfaceDefinition originalInterface : originalInterfaces) {
				for(InterfaceDefinition fromPreviousIteration : addedInPreviousIteration) {
					Set<GenericMethod> methodIntersection = getIntersection(originalInterface, fromPreviousIteration);
					if(CollectionUtils.isNotEmpty(methodIntersection)) {
						Optional<InterfaceDefinition> duplicateInterface = findMatchingMethodSet(methodIntersection, allCommonalities);
						
						if(!duplicateInterface.isPresent()) {
							InterfaceDefinition intersectionElement = createCommonInterface(originalInterface, fromPreviousIteration);
							if(intersectionElement!=null) {
								log.info("Adding interface {}", intersectionElement);
								intersection.add(intersectionElement);
								allCommonalities.add(intersectionElement);
							}
						}
						else {
							duplicateInterface.get().getImplementedBy().addAll(originalInterface.getImplementedBy());
						}
					}
				}
			}

			addedInPreviousIteration = intersection;
		} while(allCommonalities.size() > startingSize);
		return allCommonalities;
	}
	
	private int interfaceCounter = 1;
	private InterfaceDefinition createCommonInterface(InterfaceDefinition originalInterface,
			InterfaceDefinition fromPreviousIteration) {

		if(CollectionUtils.isEqualCollection(originalInterface.getMethodSignatures(), fromPreviousIteration.getMethodSignatures())) {
			// This should never happen
			log.error("TWO INTERFACES SHOULD NEVER BE USED TO CREATE A COMMON INTERFACE IF THEY HAVE IDENTICAL METHODS");
			System.exit(1);
		}
		
		InterfaceDefinition result = new InterfaceDefinition();
		result.setName("IFace" + interfaceCounter++);
		result.setPkg("");
		result.getMethodSignatures().addAll(getIntersection(originalInterface, fromPreviousIteration));
		return result;
	}

	private Optional<InterfaceDefinition> findMatchingMethodSet(Set<GenericMethod> methodIntersection, Collection<InterfaceDefinition> allCommonalities) {
		return allCommonalities.stream().filter(iface -> CollectionUtils.isEqualCollection(iface.getMethodSignatures(), methodIntersection)).findAny();
	}

	private void addAllValidExtends(Collection<InterfaceDefinition> interfaces) {
		for(InterfaceDefinition interfaceA : interfaces) {
			Collection<GenericMethod> interfaceAMethods = getAllMethods(interfaceA);
			for(InterfaceDefinition interfaceB : interfaces) {
				if(interfaceA!=interfaceB) {
					Collection<GenericMethod> interfaceBMethods = getAllMethods(interfaceB);
					if(CollectionUtils.isSubCollection(interfaceAMethods, interfaceBMethods)) {
						// A only declares methods which are also present in B. Therefore, it is valid for B to extend A
						if(!getAllSuperTypes(interfaceA).collect(Collectors.toSet()).contains(interfaceB)) {
								//.noneMatch(interfaceB::equals)) { // Prevents cycles when two sets are equal
							interfaceB.getMustExtend().add(interfaceA);
							interfaceA.getExtendedBy().add(interfaceB);
							interfaceB.getMethodSignatures().removeAll(interfaceAMethods);
						}
					}
				}
			}	
		}
	}


	private Set<GenericMethod> getAllMethods(InterfaceDefinition from) {
		// Will break on cyclic dependencies. But so will Java itself...
		Set<GenericMethod> result = new HashSet<GenericMethod>();
		result.addAll(from.getMethodSignatures());
		from.getMustExtend().forEach(extended->result.addAll(getAllMethods(extended)));
		return result;
	}

	private Set<GenericMethod> getIntersection(InterfaceDefinition newInterface, InterfaceDefinition oldInterface) {
		Set<GenericMethod> oldMethods = new HashSet<>(oldInterface.getMethodSignatures()); // Recompute hashes... This should be avoided. Currently this is just a horrible hack.
		oldInterface.setMethodSignatures(oldMethods);
		
		Set<GenericMethod> newMethods = new HashSet<>(newInterface.getMethodSignatures());
		newInterface.setMethodSignatures(newMethods);
		
		return oldMethods.stream().filter(newMethods::contains).collect(Collectors.toSet());
	}

	public void updateAllMethodSignatures(Collection<InterfaceDefinition> originalInterfaces) {
		Map<String, InterfaceDefinition> classToInterfaceMap = getClassToInterfaceMap(originalInterfaces);
		for(InterfaceDefinition originalInterface : originalInterfaces) {
			for(GenericMethod originalInterfaceMethod : originalInterface.getMethodSignatures()) {
				originalInterfaceMethod.updateMethodSignature(classToInterfaceMap);
			}
		}
	}

	public void pruneRedundantSuperclasses(InterfaceDefinition forThisInterface) {
		List<InterfaceDefinition> toRemove = new ArrayList<>();
		for(InterfaceDefinition extended : forThisInterface.mustExtend) {
			for(InterfaceDefinition otherExtended : forThisInterface.mustExtend) {
				if(extended != otherExtended) {
					if(getAllSuperTypes(otherExtended).anyMatch(supertype -> supertype == extended)) {
						toRemove.add(extended);
					}
				}
			}
		}
		forThisInterface.getMustExtend().removeAll(toRemove);
	}

	public Stream<InterfaceDefinition> getAllSuperTypes(InterfaceDefinition ofThisInterface) {
		return Stream.concat(	ofThisInterface.getMustExtend().stream(), 
								ofThisInterface.getMustExtend().stream().flatMap(this::getAllSuperTypes));
	}


	public Map<String, InterfaceDefinition> getClassToInterfaceMap(Collection<InterfaceDefinition> originalInterfaces) {
		// implementor map can be built directly from originalInterfaces. 
		// Each interface definition needs to track fully qualified types in the return type, so we can match that type against the set of implementing classes
		// As implementor class names are fully qualified, it should be easy to perform matching.
		HashMap<String, InterfaceDefinition> result = new HashMap<>();
		originalInterfaces.forEach(iface -> {
			iface.getImplementedBy().forEach(cls -> {
				result.put(cls, iface);
				String nameOnly = StringUtils.substringAfterLast(cls, ".");
				result.put(nameOnly, iface);
			});
		});
		return result;
	}

}
