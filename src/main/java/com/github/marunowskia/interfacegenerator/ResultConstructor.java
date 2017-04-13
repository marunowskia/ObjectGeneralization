package com.github.marunowskia.interfacegenerator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import info.debatty.java.stringsimilarity.LongestCommonSubsequence;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResultConstructor {
	
	public static void main(String args[]) {
		Application.main(args);
	}

	public static int numIterations = 0;
	public Set<InterfaceDefinition> buildResult(Collection<InterfaceDefinition> originalInterfaces) {
		System.out.printf("Number of iterations: %s\tNumber of interfaces in this layer: %s\n", numIterations++, originalInterfaces.size());

		if(CollectionUtils.isEmpty(originalInterfaces)) {
			return new HashSet<>();
		}  

		updateAllMethodSignatures(originalInterfaces);

		Set<InterfaceDefinition> allSharedMethods = getAllCommonalities(originalInterfaces);
		addAllValidExtends(allSharedMethods);


		allSharedMethods.forEach(this::pruneRedundantSuperclasses);
		allSharedMethods.forEach(this::flattenHierarchy);
		eraseOrphanInterfaces(allSharedMethods);


		renameSingleMethodInterfaces(allSharedMethods);
		reorganizePackages(allSharedMethods);
		updateAllMethodSignatures(allSharedMethods);


		allSharedMethods.forEach(this::pruneRedundantSuperclasses);
		allSharedMethods.forEach(this::flattenHierarchy);
		eraseOrphanInterfaces(allSharedMethods);
		eraseUndesiredInterfaces(allSharedMethods);

		updateDependencies(allSharedMethods);
		

		return allSharedMethods;
	}

	private void eraseUndesiredInterfaces(Set<InterfaceDefinition> interfaceDefinitions) {
		Set<InterfaceDefinition> toRemove =
				interfaceDefinitions.stream()
						.filter(orphan -> orphan.getMethodSignatures().size()==1)
						.filter(orphan ->
								CollectionUtils.extractSingleton(orphan.getMethodSignatures()).getMethodSignature().endsWith("getInput()")
						)
						.collect(Collectors.toSet());

		if(!toRemove.isEmpty()) {
			interfaceDefinitions.forEach(iface -> iface.getExtendedBy().removeAll(toRemove));
			interfaceDefinitions.removeAll(toRemove);
		}
	}

	private void updateDependencies(Collection<InterfaceDefinition> allSharedMethods) {
		Map<String, InterfaceDefinition> implementorToInterfaceMap = getClassToInterfaceMap(allSharedMethods);

		allSharedMethods.forEach(iface ->{
			iface.getDependencies().clear();
			iface.getMethodSignatures().forEach(method -> {
				method.getFullyQualifiedDependencies().forEach(fullyQualifiedDependency -> {
					Optional.ofNullable(implementorToInterfaceMap.get(fullyQualifiedDependency)).ifPresent(implemented -> {
						iface.getDependencies().add(implemented.getPkg() + "." + implemented.getName());
					});
				});

			});
		});
	}

	private void renameSingleMethodInterfaces(Set<InterfaceDefinition> allSharedMethods) {
		allSharedMethods.forEach(def -> {
			if(def.getAllMethods().size()==1 && def.getMethodSignatures().size()==1) {

				if(CollectionUtils.size(def.getImplementedBy()) + CollectionUtils.size(def.getIndirectlyImplementedBy()) == 1) {
					return;
				}

				GenericMethod onlyMethod = CollectionUtils.extractSingleton(def.getMethodSignatures());
				String newName =
						"I"
						+ String.join("", onlyMethod.getOriginalDeclaration().getType().toStringWithoutComments().replaceAll("<.*>", ""))
						+ onlyMethod.getOriginalDeclaration().getName().substring(3);

				// Being lazy about hashmaps...
				def.getExtendedBy().forEach(sub->sub.getMustExtend().remove(def));
				def.setName(newName);
				def.getExtendedBy().forEach(sub->sub.getMustExtend().add(def));
			}
		});
	}

	private Set<InterfaceDefinition> getAllCommonalities(Collection<InterfaceDefinition> originalInterfaces) {
		System.out.println("getAllCommonalities");
		Set<InterfaceDefinition> allCommonalities = new HashSet<>();
		allCommonalities.addAll(originalInterfaces);

		Collection<InterfaceDefinition> addedInPreviousIteration = originalInterfaces;
		int startingSize = 0;
		do {
			System.out.println("Building commonalities");
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
								intersection.add(intersectionElement);
								allCommonalities.add(intersectionElement);
							}
						}
					}
				}
			}

			addedInPreviousIteration = intersection;
		} while(allCommonalities.size() > startingSize);
		
		return new HashSet<InterfaceDefinition>(allCommonalities);
	}
	
	private int interfaceCounter = 1;
	private InterfaceDefinition createCommonInterface(InterfaceDefinition originalInterface,
			InterfaceDefinition fromPreviousIteration) {

		if(CollectionUtils.isEqualCollection(originalInterface.getMethodSignatures(), fromPreviousIteration.getMethodSignatures())) {
			// This should never happen
			System.exit(1);
		}


		InterfaceDefinition result = new InterfaceDefinition();
		result.setName("IFace" + interfaceCounter++);
		result.setPkg("defaultpackage");
		result.getMethodSignatures().addAll(getIntersection(originalInterface, fromPreviousIteration));
		return result;
	}

	private Optional<InterfaceDefinition> findMatchingMethodSet(Set<GenericMethod> methodIntersection, Collection<InterfaceDefinition> allCommonalities) {
		return allCommonalities.stream().filter(iface -> CollectionUtils.isEqualCollection(iface.getAllMethods(), methodIntersection)).findAny();
	}

//	private Optional<InterfaceDefinition> findMatchingMethodSet(Set<GenericMethod> methodIntersection, Collection<InterfaceDefinition> allCommonalities) {
//		final HashSet<GenericMethod> updatedHashes = new HashSet<>();
//		methodIntersection.forEach(updatedHashes::add);
//		methodIntersection = updatedHashes;
//
//		allCommonalities.forEach(iface-> {
//			HashSet<GenericMethod> ifacehashes = new HashSet<>();
//			iface.getMethodSignatures().forEach(ifacehashes::add);
//			iface.setMethodSignatures(ifacehashes);
//		});
//		return allCommonalities.stream().filter(iface -> CollectionUtils.isEqualCollection(iface.getAllMethods(), updatedHashes)).findAny();
//	}

	private void addAllValidExtends(Collection<InterfaceDefinition> interfaces) {
		System.out.println("addAllValidExtends");
		HashMap<InterfaceDefinition, String> newPackages = new HashMap<>();
		HashMap<InterfaceDefinition, Collection<GenericMethod>> allMethods = new HashMap<>();
		for(InterfaceDefinition interfaceA : interfaces) {
			Collection<GenericMethod> interfaceAMethods = allMethods.getOrDefault(interfaceA, interfaceA.getAllMethods());
			if (!allMethods.containsKey(interfaceA)) {
				allMethods.put(interfaceA, interfaceAMethods);
			}
		}
		for(InterfaceDefinition interfaceA : interfaces) {
			for(InterfaceDefinition interfaceB : interfaces) {
				if(interfaceA.getName().equals("IFace15") && interfaceB.getName().equals("IFace19")) {
					System.out.print("");
				}
				Collection<GenericMethod> interfaceAMethods = allMethods.get(interfaceA);
				if(interfaceA!=interfaceB) {
					Collection<GenericMethod> interfaceBMethods = allMethods.get(interfaceB);
					if(CollectionUtils.isSubCollection(interfaceAMethods, interfaceBMethods)) {
						// A only declares methods which are also present in B. Therefore, it is valid for B to extend A
						if(!getAllSuperTypes(interfaceA).anyMatch(interfaceB::equals)) {
							if(!interfaceB.isRequired()) {
								interfaceB.getMustExtend().add(interfaceA);
								interfaceA.getExtendedBy().add(interfaceB);
								interfaceB.getMethodSignatures().removeAll(interfaceAMethods);

							}
						}
					}
				}
			}	
		}
		System.out.println("done with addAllValidExtends");

		newPackages.forEach((iface, pkg) -> iface.setPkg(pkg));
	}


	private Set<GenericMethod> getIntersection(InterfaceDefinition newInterface, InterfaceDefinition oldInterface) {
		Set<GenericMethod> oldMethods = new HashSet<>(oldInterface.getAllMethods()); // Recompute hashes... This should be avoided. Currently this is just a horrible hack.
		oldInterface.setMethodSignatures(oldMethods);
		
		Set<GenericMethod> newMethods = new HashSet<>(newInterface.getAllMethods());
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

		Set<InterfaceDefinition> toRemove = new HashSet<>();
		Set<InterfaceDefinition> toInsert = new HashSet<>();
		for (InterfaceDefinition extended : forThisInterface.mustExtend) {
			for (InterfaceDefinition otherExtended : forThisInterface.mustExtend) {
				if (extended != otherExtended) {
					if (getAllSuperTypes(otherExtended).anyMatch(supertype -> supertype == extended)) {
						toRemove.add(extended);
					}
				}
			}
		}
		forThisInterface.getMustExtend().removeAll(toRemove);
	}
	
	public boolean isPassthrough(InterfaceDefinition passthrough) {
		return  !passthrough.isRequired()
				&&
				passthrough.getMustExtend().size() == 1
				&&
				passthrough.getMethodSignatures().isEmpty();
	}
	
	public void flattenHierarchy(InterfaceDefinition passthrough) {
		
		if(isPassthrough(passthrough)) {

			InterfaceDefinition passthroughSuperInterface = CollectionUtils.extractSingleton(passthrough.getMustExtend());
			
//			passthroughSuperInterface.getDependencies().addAll(passthrough.getDependencies());
			passthroughSuperInterface.getImplementedBy().addAll(passthrough.getImplementedBy());
			passthrough.getImplementedBy().clear();
			
			HashSet<InterfaceDefinition> newExtendedBy = new HashSet<>();
			newExtendedBy.addAll(passthrough.getExtendedBy());
			newExtendedBy.addAll(passthroughSuperInterface.getExtendedBy());
			passthroughSuperInterface.setExtendedBy(newExtendedBy);

			System.out.println("new ExtendedBy size: " + newExtendedBy.size());

			passthrough.getExtendedBy().forEach(newSubInterface -> newSubInterface.getMustExtend().remove(passthrough));
			passthrough.getExtendedBy().forEach(newSubInterface -> newSubInterface.getMustExtend().add(passthroughSuperInterface));
			passthrough.getMustExtend().clear();;
			passthrough.getExtendedBy().clear();
		}
	}
	
	private void eraseOrphanInterfaces(Set<InterfaceDefinition> interfaceDefinitions) {
		Set<InterfaceDefinition> toRemove =
				interfaceDefinitions.stream()
						.filter(orphan -> orphan.getExtendedBy().isEmpty())
						.filter(orphan -> orphan.getImplementedBy().isEmpty())
						.filter(orphan -> !orphan.isRequired())
						.collect(Collectors.toSet());


		if(!toRemove.isEmpty()) {
			interfaceDefinitions.forEach(iface -> iface.getExtendedBy().removeAll(toRemove));
			interfaceDefinitions.removeAll(toRemove);
		}
	}

	public static Stream<InterfaceDefinition> getAllSuperTypes(InterfaceDefinition ofThisInterface) {
		return Stream.concat(	ofThisInterface.getMustExtend().stream(),
								ofThisInterface.getMustExtend().stream().flatMap(ext->getAllSuperTypes(ext)));
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

	private static Set<InterfaceDefinition> reorganizePackages(Set<InterfaceDefinition> allInterfaces) {
		allInterfaces.forEach(iface -> {

			Collection<String> allImplementors = new ArrayList<String>();
			allImplementors.addAll(Arrays.asList(iface.getIndirectlyImplementedBy().toArray(new String[]{})));
			allImplementors.addAll(Arrays.asList(iface.getImplementedBy().toArray(new String[]{})));
			allImplementors = allImplementors
					.stream()
					.map(fullyQualified -> StringUtils.substringBeforeLast(fullyQualified, "."))
					.collect(Collectors.toList());


			String newPackage = StringUtils.getCommonPrefix(allImplementors.toArray(new String[]{}));
			if(newPackage.endsWith(".")) {
				newPackage = StringUtils.substringBeforeLast(newPackage,".");
			}
			if(newPackage.isEmpty()) {
				newPackage = "defaultpackage";
			}

			// Being lazy about hashset hashes... TODO: FIX THIS! THERE IS AN OBVIOUS BUG HERE.
			iface.getExtendedBy().forEach(sub->sub.getMustExtend().remove(iface));
			iface.setPkg(newPackage);
			iface.getExtendedBy().forEach(sub->sub.getMustExtend().add(iface));
		});
		return allInterfaces;
	}
}