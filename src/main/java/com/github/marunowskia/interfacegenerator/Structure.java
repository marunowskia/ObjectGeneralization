package com.github.marunowskia.interfacegenerator;

import static org.apache.commons.collections4.CollectionUtils.*;

import java.util.*;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Getter @Setter
public class Structure {

	private List<InterfaceDefinition> structureContents = new ArrayList<>();
	private HashMap<InterfaceDefinition, Set<String>> implementingTypes = new HashMap<>();

	public Structure() {

	}

	public Structure(Structure copy) {
		structureContents = new ArrayList<>(copy.getStructureContents());
		implementingTypes = new HashMap<>(copy.getImplementingTypes());
	}

	public InterfaceDefinition add(InterfaceDefinition newInterface, List<String> implementors) {
		updateReturnTypes(newInterface);
		checkForTotalOverlap(newInterface);
		
		List<InterfaceDefinition> intersectionResults = new ArrayList<>();
		for(InterfaceDefinition existingInterface : structureContents) {
			Set<String> overlap = getIntersection(newInterface, existingInterface); 

			if(!CollectionUtils.isEmpty(overlap) && CollectionUtils.isNotEmpty(newInterface.getMethodSignatures().keySet())) { 
				
				if(CollectionUtils.isSubCollection(getAllMethods(existingInterface), getAllMethods(newInterface))) {
					newInterface.getMustExtend().add(existingInterface);
					overlap.forEach(newInterface.getMethodSignatures()::remove);
					continue;
				}

				if(CollectionUtils.isEqualCollection(getAllMethods(existingInterface), newInterface.getMethodSignatures().keySet())) {
					if(implementingTypes.containsKey(existingInterface)) {
						implementors.forEach(implementor -> {
							implementingTypes.get(existingInterface).add(implementor);
							structureContents.add(newInterface);
						});
					}
					else {
						implementingTypes.put(existingInterface, new HashSet<>(implementors));
						structureContents.add(newInterface);
					}
//					intersectionResults.forEach(toAdd -> add(toAdd, new ArrayList<>()));
					break;
				}

				InterfaceDefinition sharedMethodInterface = new InterfaceDefinition();
				sharedMethodInterface.setPkg(existingInterface.getPkg());
				sharedMethodInterface.setName(selectNameForSharedInterface(newInterface, existingInterface));
				if(sharedMethodInterface.getName().equals("I2")) {
					System.out.println("I2 starts here");
//					checkForTotalOverlap(newInterface);
				}
				sharedMethodInterface.getDependencies().addAll(newInterface.getDependencies());
				sharedMethodInterface.getDependencies().addAll(existingInterface.getDependencies());
				
				overlap.forEach(method-> {
					if(newInterface.getMethodSignatures().containsKey(method)) {
						sharedMethodInterface.getMethodSignatures().put(method, newInterface.getMethodSignatures().get(method));
					}
				});

				if(getAllMethods(sharedMethodInterface).isEmpty()) {
					continue;
				}
				
				overlap.forEach(newInterface.getMethodSignatures()::remove);
				newInterface.getMustExtend().add(sharedMethodInterface);

				overlap.forEach(existingInterface.getMethodSignatures()::remove);
				existingInterface.getMustExtend().add(sharedMethodInterface);
				
				intersectionResults.add(sharedMethodInterface);

				
			}
		}
		if(implementingTypes.containsKey(newInterface)) {
			implementingTypes.get(newInterface).addAll(implementors);
			structureContents.add(newInterface);
		}
		else {
			implementingTypes.put(newInterface, new HashSet<>(implementors));
			structureContents.add(newInterface);
		}
		
		structureContents.add(newInterface);
		structureContents.addAll(intersectionResults);
		newInterface.getMethodSignatures().values().forEach(method -> {
			if(method==null) {
				System.out.println("Why so null?");
			}
		});
		updateAllReturnTypes();
		collapse();
		return newInterface;
	}
	
	private void checkForTotalOverlap(InterfaceDefinition incoming) {
		structureContents.forEach(other -> {
			Set<String> allMethodsFromOther = getAllMethods(other);
			if(CollectionUtils.isSubCollection(
					allMethodsFromOther,
					getAllMethods(incoming))) {

				incoming.getMustExtend().add(other);
				allMethodsFromOther.forEach(incoming.getMethodSignatures()::remove);
			}
		});
	}
	
	private static int counter = 0;
	private String selectNameForSharedInterface(InterfaceDefinition newInterface, InterfaceDefinition existingInterface) {
		return "I" + counter++;
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


	public Set<String> getAllMethods(InterfaceDefinition from) {
		// Will break on cyclic dependencies. But so will Java itself...
		Set<String> result = new HashSet<String>();
		result.addAll(from.getMethodSignatures().keySet());
		from.getMustExtend().forEach(extended->result.addAll(getAllMethods(extended)));
		return result;
	}

	private Set<String> getIntersection(InterfaceDefinition newInterface, InterfaceDefinition oldInterface) {
		Set<String> oldMethods = getAllMethods(oldInterface);
		Set<String> newMethods = getAllMethods(newInterface);
		return oldMethods.stream().filter(newMethods::contains).collect(Collectors.toSet());
	}

	class MethodSignature {
		String fullyQualifiedReturnType;
		String methodName;
	}

	public void collapse() {
		// An interface may recombine with another interface IFF the resultant structure
		// does not violate the ability for each type in "implementingTypes" to implement the recombined interface.

		// This occurs in two scenarios:

		// ========================================
		// 1) If interfaces A, B are under consideration, A,B can be merged if the following two sets are equivalent:
		//          Set 1: {type|type extends A or type implements A}
		//          Set 2: {type|type extends B or type implements B}
		// ========================================

		HashMap<InterfaceDefinition, InterfaceDefinition> replacementPairs = new HashMap<>();
		for(int a=0; a<structureContents.size(); a++) {
			InterfaceDefinition interfaceA = structureContents.get(a);
			Set<String> extendsOrImplementsA = implementingTypes.getOrDefault(interfaceA, new HashSet<String>());
			if(!isEmpty(extendsOrImplementsA)) {
				for(int b=a+1; b<structureContents.size(); b++) {
					InterfaceDefinition interfaceB = structureContents.get(b);
					Set<String> extendsOrImplementsB = implementingTypes.getOrDefault(interfaceB, new HashSet<String>());
					if(isEqualCollection(extendsOrImplementsA, extendsOrImplementsB)) {
						replacementPairs.put(interfaceA, interfaceB);
					}
				}
			}
		}
		replaceAll(replacementPairs);
		replacementPairs.clear();

		// ========================================
		// 2) If interfaces A, B are under consideration, then A only extends B, and A declares no additional methods
		// ========================================
		for(InterfaceDefinition definition : structureContents) {
			if(size(definition.getMustExtend()) == 1) {
				if(isEmpty(definition.getMethodSignatures().keySet())) {
					replacementPairs.put(definition, definition.getMustExtend().get(0));
				}
			}
		}
		replaceAll(replacementPairs);
	}

	public void replace(InterfaceDefinition replaceThis, InterfaceDefinition with, HashMap<InterfaceDefinition, InterfaceDefinition> plannedReplacements) {
		updateImplementors(replaceThis, with);
		updateStructure(replaceThis, with);
	}

	public void updateStructure(InterfaceDefinition replaceThis, InterfaceDefinition with) {
		structureContents.stream().map(id->id.getMustExtend())
		.filter(mustExtend -> mustExtend.contains(replaceThis))
		.forEach(mustExtend -> {
			mustExtend.remove(  replaceThis);
			mustExtend.add(     with);
		});
		structureContents.remove(replaceThis);
	}

	private void updateImplementors(InterfaceDefinition replaceThis, InterfaceDefinition with) {
		HashMap<InterfaceDefinition, Set<String>> newImplementingTypes = new HashMap<>();
		newImplementingTypes.putAll(implementingTypes);

		implementingTypes = newImplementingTypes;
		Set<String> implementsReplaceThis = implementingTypes.getOrDefault(replaceThis, new HashSet<>());
		Set<String> implementsWith =        implementingTypes.getOrDefault(with, new HashSet<>());
		implementsWith.addAll(implementsReplaceThis);
		implementingTypes.remove(replaceThis);
		implementingTypes.put(with, implementsWith);

	}
	
	private void updateAllReturnTypes() {
		Map<String, InterfaceDefinition> implementorMap = createImplementorMap();
		structureContents.forEach(existing -> {
			updateReturnTypes(existing, implementorMap);
		});
		
		structureContents.forEach(existing -> {
			if(existing==null) {
				System.out.println("Why so null?");
			}
		});
	}
	
	private void updateReturnTypes(InterfaceDefinition incoming) {
		Map<String, InterfaceDefinition> implementorMap = createImplementorMap();
		updateReturnTypes(incoming, implementorMap);
	}
	
	private Map<String, InterfaceDefinition> createImplementorMap() {
		Map<String, InterfaceDefinition> implementorMap = new HashMap<>();
		implementingTypes.forEach((type, implementors)-> {
			implementors.forEach(implementor -> {
				if(type==null) {
					System.out.println("Why so null?");
				}
				implementorMap.put(implementor, type);				
			});
		});
		return implementorMap;
	}
	
	private void updateReturnTypes(InterfaceDefinition incoming, Map<String, InterfaceDefinition> implementorMap) {
		HashMap<String, MethodDeclaration> updatedMethodSignatures = new HashMap<>();
		
		incoming.getMethodSignatures().values().forEach(method -> {
			if(method==null) {
				System.out.println("Why so null?");
			}
			String updatedType = TypeUpdateUtility.updateType(method.getType().toStringWithoutComments(), implementorMap);
			updatedMethodSignatures.put("public " + updatedType + " " + method.getName() + "()", method);
		});
		
		incoming.setMethodSignatures(updatedMethodSignatures);
		
		incoming.getMethodSignatures().values().forEach(method -> {
			if(method==null) {
				System.out.println("Why so null?");
			}
		});
	}

	public void replaceAll(@NonNull  HashMap<InterfaceDefinition, InterfaceDefinition> plannedReplacements) {
		while(!plannedReplacements.isEmpty()) {
			InterfaceDefinition thisInterface = plannedReplacements.keySet().iterator().next();
			InterfaceDefinition withThisInterface = plannedReplacements.get(thisInterface);
			replace(thisInterface, withThisInterface, plannedReplacements);

			// Update plannedReplacements so we don't re-introduce an interface we want to eliminate
			getKeysByValue(plannedReplacements, thisInterface).forEach(key -> plannedReplacements.put(key, withThisInterface));
			plannedReplacements.remove(thisInterface);
		}
	}

	//http://stackoverflow.com/a/2904266
	public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
		Set<T> keys = new HashSet<T>();
		for (Map.Entry<T, E> entry : map.entrySet()) {
			if (Objects.equals(value, entry.getValue())) {
				keys.add(entry.getKey());
			}
		}
		return keys;
	}

	public List<InterfaceDefinition> getStructureContents() {
		return structureContents;
	}

	public void setStructureContents(List<InterfaceDefinition> structureContents) {
		this.structureContents = structureContents;
	}

	public HashMap<InterfaceDefinition, Set<String>> getImplementingTypes() {
		return implementingTypes;
	}

	public void setImplementingTypes(HashMap<InterfaceDefinition, Set<String>> implementingTypes) {
		this.implementingTypes = implementingTypes;
	}
}

//package com.github.marunowskia.interfacegenerator;
//
//import static org.apache.commons.collections4.CollectionUtils.*;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//import com.github.javaparser.ast.body.MethodDeclaration;
//import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
//import lombok.Getter;
//import lombok.NonNull;
//import lombok.Setter;
//import org.apache.commons.collections4.CollectionUtils;
//import org.apache.commons.lang3.StringUtils;
//
//@Getter @Setter
//public class Structure {
//
//	private List<InterfaceDefinition> structureContents = new ArrayList<>();
//	private HashMap<InterfaceDefinition, Set<String>> implementingTypes = new HashMap<>();
//
//	public Structure() {
//
//	}
//
//	public Structure(Structure copy) {
//		structureContents = new ArrayList<>(copy.getStructureContents());
//		implementingTypes = new HashMap<>(copy.getImplementingTypes());
//	}
//
//	public InterfaceDefinition add(InterfaceDefinition newInterface, List<String> implementors) {
//		newInterface.getMethodSignatures().values().forEach(value -> {
//			if(value==null) {
//				System.out.println("Why so null");
//			}
//		});
//
//		if(newInterface.getMethodSignatures().containsKey("public IDeviceIdentifier getDevice()")) {
//			System.out.println("breaking on public IDeviceIdentifier getDevice();");
//		}
//		
//		updateReturnTypes(newInterface);
////		checkForTotalOverlap(newInterface);
//		
//		List<InterfaceDefinition> intersectionResults = new ArrayList<>();
//		for(InterfaceDefinition existingInterface : structureContents) {
//			Set<String> overlap = getIntersection(newInterface, existingInterface); 
//
//			if(!CollectionUtils.isEmpty(overlap) && CollectionUtils.isNotEmpty(newInterface.getMethodSignatures().keySet())) { 
//				
//				if(CollectionUtils.isSubCollection(getAllMethods(existingInterface), getAllMethods(newInterface))) {
//					newInterface.getMustExtend().add(existingInterface);
//					overlap.forEach(newInterface.getMethodSignatures()::remove);
//					continue;
//				}
//
//				if(CollectionUtils.isEqualCollection(getAllMethods(existingInterface), newInterface.getMethodSignatures().keySet())) {
//					if(implementingTypes.containsKey(existingInterface)) {
//						implementors.forEach(implementor -> {
//							implementingTypes.get(existingInterface).add(implementor);
//							structureContents.add(newInterface);
//						});
//					}
//					else {
//						implementingTypes.put(existingInterface, new HashSet<>(implementors));
//						structureContents.add(newInterface);
//					}
////					intersectionResults.forEach(toAdd -> add(toAdd, new ArrayList<>()));
//					break;
//				}
//
//				InterfaceDefinition sharedMethodInterface = new InterfaceDefinition();
//				sharedMethodInterface.setPkg(existingInterface.getPkg());
//				sharedMethodInterface.setName(selectNameForSharedInterface(newInterface, existingInterface));
//				if(sharedMethodInterface.getName().equals("I2")) {
//					System.out.println("I2 starts here");
////					checkForTotalOverlap(newInterface);
//				}
//				sharedMethodInterface.getDependencies().addAll(newInterface.getDependencies());
//				sharedMethodInterface.getDependencies().addAll(existingInterface.getDependencies());
//				
//				overlap.forEach(method-> {
//					if(newInterface.getMethodSignatures().containsKey(method)) {
//						sharedMethodInterface.getMethodSignatures().put(method, newInterface.getMethodSignatures().get(method));
//					}
//				});
//
//				if(getAllMethods(sharedMethodInterface).isEmpty()) {
//					continue;
//				}
//				
//				overlap.forEach(newInterface.getMethodSignatures()::remove);
//				newInterface.getMustExtend().add(sharedMethodInterface);
//
//				overlap.forEach(existingInterface.getMethodSignatures()::remove);
//				existingInterface.getMustExtend().add(sharedMethodInterface);
//				
//				intersectionResults.add(sharedMethodInterface);
//
//				
//			}
//		}
//		if(implementingTypes.containsKey(newInterface)) {
//			implementingTypes.get(newInterface).addAll(implementors);
//			structureContents.add(newInterface);
//		}
//		else {
//			implementingTypes.put(newInterface, new HashSet<>(implementors));
//			structureContents.add(newInterface);
//		}
//		
//		structureContents.add(newInterface);
//		structureContents.addAll(intersectionResults);
//		newInterface.getMethodSignatures().values().forEach(method -> {
//			if(method==null) {
//				System.out.println("Why so null?");
//			}
//		});
//		updateAllReturnTypes();
//		return newInterface;
//	}
//	
//	private void checkForTotalOverlap(InterfaceDefinition incoming) {
//		structureContents.forEach(other -> {
//			Set<String> allMethodsFromOther = getAllMethods(other);
//			if(CollectionUtils.isSubCollection(
//					allMethodsFromOther,
//					getAllMethods(incoming))) {
//
//				incoming.getMustExtend().add(other);
//				allMethodsFromOther.forEach(incoming.getMethodSignatures()::remove);
//			}
//		});
//	}
//	
//	private static int counter = 0;
//	private String selectNameForSharedInterface(InterfaceDefinition newInterface, InterfaceDefinition existingInterface) {
//		return "I" + counter++;
////		String commonPrefix = StringUtils.getCommonPrefix(newInterface.getName(), existingInterface.getName()); // TODO: Come up with a clever way to auto-name interfaces
////		String remainingNew = StringUtils.substringAfter(newInterface.getName(), commonPrefix);
////		String remainingOld = StringUtils.substringAfter(existingInterface.getName(), commonPrefix);
////
////		String selectedName;
////		if(remainingNew.compareTo(remainingOld) < 0) {
////			selectedName = commonPrefix + "_" + remainingNew + "_" + remainingOld;
////		}
////		else {
////			selectedName = commonPrefix + "_" + remainingOld + "_" + remainingNew;
////		}
////		return selectedName;
//	}
//
//
//	public Set<String> getAllMethods(InterfaceDefinition from) {
//		// Will break on cyclic dependencies. But so will Java itself...
//		Set<String> result = new HashSet<String>();
//		result.addAll(from.getMethodSignatures().keySet());
//		from.getMustExtend().forEach(extended->result.addAll(getAllMethods(extended)));
//		return result;
//	}
//
//	private Set<String> getIntersection(InterfaceDefinition newInterface, InterfaceDefinition oldInterface) {
//		Set<String> oldMethods = getAllMethods(oldInterface);
//		Set<String> newMethods = getAllMethods(newInterface);
//		return oldMethods.stream().filter(newMethods::contains).collect(Collectors.toSet());
//	}
//
//	class MethodSignature {
//		String fullyQualifiedReturnType;
//		String methodName;
//	}
//
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
//			Set<String> extendsOrImplementsA = implementingTypes.getOrDefault(interfaceA, new HashSet<String>());
//			if(!isEmpty(extendsOrImplementsA)) {
//				for(int b=a+1; b<structureContents.size(); b++) {
//					InterfaceDefinition interfaceB = structureContents.get(b);
//					Set<String> extendsOrImplementsB = implementingTypes.getOrDefault(interfaceB, new HashSet<String>());
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
//				if(isEmpty(definition.getMethodSignatures().keySet())) {
//					replacementPairs.put(definition, definition.getMustExtend().get(0));
//				}
//			}
//		}
//		replaceAll(replacementPairs);
//	}
//
//	public void replace(InterfaceDefinition replaceThis, InterfaceDefinition with, HashMap<InterfaceDefinition, InterfaceDefinition> plannedReplacements) {
//		updateImplementors(replaceThis, with);
//		updateStructure(replaceThis, with);
//	}
//
//	public void updateStructure(InterfaceDefinition replaceThis, InterfaceDefinition with) {
//		structureContents.stream().map(id->id.getMustExtend())
//		.filter(mustExtend -> mustExtend.contains(replaceThis))
//		.forEach(mustExtend -> {
//			mustExtend.remove(  replaceThis);
//			mustExtend.add(     with);
//		});
//		structureContents.remove(replaceThis);
//	}
//
//	private void updateImplementors(InterfaceDefinition replaceThis, InterfaceDefinition with) {
//		HashMap<InterfaceDefinition, Set<String>> newImplementingTypes = new HashMap<>();
//		newImplementingTypes.putAll(implementingTypes);
//
//		implementingTypes = newImplementingTypes;
//		Set<String> implementsReplaceThis = implementingTypes.getOrDefault(replaceThis, new HashSet<>());
//		Set<String> implementsWith =        implementingTypes.getOrDefault(with, new HashSet<>());
//		implementsWith.addAll(implementsReplaceThis);
//		implementingTypes.remove(replaceThis);
//		implementingTypes.put(with, implementsWith);
//
//	}
//	
//	private void updateAllReturnTypes() {
//		Map<String, InterfaceDefinition> implementorMap = createImplementorMap();
//		structureContents.forEach(existing -> {
//			updateReturnTypes(existing, implementorMap);
//		});
//		
//		structureContents.forEach(existing -> {
//			if(existing==null) {
//				System.out.println("Why so null?");
//			}
//		});
//	}
//	
//	private void updateReturnTypes(InterfaceDefinition incoming) {
//		Map<String, InterfaceDefinition> implementorMap = createImplementorMap();
//		updateReturnTypes(incoming, implementorMap);
//	}
//	
//	private Map<String, InterfaceDefinition> createImplementorMap() {
//		Map<String, InterfaceDefinition> implementorMap = new HashMap<>();
//		implementingTypes.forEach((type, implementors)-> {
//			implementors.forEach(implementor -> {
//				if(type==null) {
//					System.out.println("Why so null?");
//				}
//				implementorMap.put(implementor, type);				
//			});
//		});
//		return implementorMap;
//	}
//	
//	private void updateReturnTypes(InterfaceDefinition incoming, Map<String, InterfaceDefinition> implementorMap) {
//		HashMap<String, MethodDeclaration> updatedMethodSignatures = new HashMap<>();
//		
//		incoming.getMethodSignatures().values().forEach(method -> {
//			if(method==null) {
//				System.out.println("Why so null?");
//			}
//			String updatedType = TypeUpdateUtility.updateType(method.getType().toStringWithoutComments(), implementorMap);
//			updatedMethodSignatures.put("public " + updatedType + " " + method.getName() + "()", method);
//		});
//		
//		incoming.setMethodSignatures(updatedMethodSignatures);
//		
//		incoming.getMethodSignatures().values().forEach(method -> {
//			if(method==null) {
//				System.out.println("Why so null?");
//			}
//		});
//	}
//
//	public void replaceAll(@NonNull  HashMap<InterfaceDefinition, InterfaceDefinition> plannedReplacements) {
//		while(!plannedReplacements.isEmpty()) {
//			InterfaceDefinition thisInterface = plannedReplacements.keySet().iterator().next();
//			InterfaceDefinition withThisInterface = plannedReplacements.get(thisInterface);
//			replace(thisInterface, withThisInterface, plannedReplacements);
//
//			// Update plannedReplacements so we don't re-introduce an interface we want to eliminate
//			getKeysByValue(plannedReplacements, thisInterface).forEach(key -> plannedReplacements.put(key, withThisInterface));
//			plannedReplacements.remove(thisInterface);
//		}
//	}
//
//	//http://stackoverflow.com/a/2904266
//	public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
//		Set<T> keys = new HashSet<T>();
//		for (Map.Entry<T, E> entry : map.entrySet()) {
//			if (Objects.equals(value, entry.getValue())) {
//				keys.add(entry.getKey());
//			}
//		}
//		return keys;
//	}
//
//	public List<InterfaceDefinition> getStructureContents() {
//		return structureContents;
//	}
//
//	public void setStructureContents(List<InterfaceDefinition> structureContents) {
//		this.structureContents = structureContents;
//	}
//
//	public HashMap<InterfaceDefinition, Set<String>> getImplementingTypes() {
//		return implementingTypes;
//	}
//
//	public void setImplementingTypes(HashMap<InterfaceDefinition, Set<String>> implementingTypes) {
//		this.implementingTypes = implementingTypes;
//	}
//}