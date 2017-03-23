package com.github.marunowskia.interfacegenerator;

import static org.apache.commons.collections4.CollectionUtils.*;

import java.util.*;
import java.util.stream.Collectors;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Getter @Setter
public class Structure {

	

	private List<InterfaceDefinition> structureContents = new ArrayList<>();
    private HashMap<InterfaceDefinition, List<String>> implementingTypes = new HashMap<>();

    public Structure() {
    	
    }

    public Structure(Structure copy) {
        structureContents = new ArrayList<>(copy.getStructureContents());
        implementingTypes = new HashMap<>(copy.getImplementingTypes());
    }
    
    public InterfaceDefinition add(InterfaceDefinition newInterface, List<String> implementors) {
    	
    	if(implementingTypes.containsKey(newInterface)) {
    		implementingTypes.get(newInterface).addAll(implementors);
    	}
    	else {
    		implementingTypes.put(newInterface, implementors);
    	}
    	List<InterfaceDefinition> intersectionResults = new ArrayList<>();
		for(InterfaceDefinition oldInterface : structureContents) {
			List<String> intersectingMethods = getIntersection(newInterface, oldInterface); 
			
			if(!CollectionUtils.isEmpty(intersectingMethods)) {
				InterfaceDefinition sharedMethodInterface = new InterfaceDefinition();
				sharedMethodInterface.getDependencies().addAll(newInterface.getDependencies());
				sharedMethodInterface.getDependencies().addAll(oldInterface.getDependencies());
				String commonPrefix = StringUtils.getCommonPrefix(newInterface.getName(), oldInterface.getName()); // TODO: Come up with a clever way to auto-name interfaces
				
				String remainingNew = StringUtils.substringAfter(newInterface.getName(), commonPrefix);
				String remainingOld = StringUtils.substringAfter(oldInterface.getName(), commonPrefix);
				
				if(remainingNew.compareTo(remainingOld) < 0) {
					sharedMethodInterface.name = commonPrefix + "_" + remainingNew + "_" + remainingOld;
				}
				else {
					sharedMethodInterface.name = commonPrefix + "_" + remainingOld + "_" + remainingNew;
				}
								  
				sharedMethodInterface.pkg = oldInterface.getPkg();
				sharedMethodInterface.setMethodSignatures(intersectingMethods);

				newInterface.getMethodSignatures().removeAll(intersectingMethods);
				newInterface.getMustExtend().add(sharedMethodInterface);
				
				oldInterface.getMethodSignatures().removeAll(intersectingMethods);
				oldInterface.getMustExtend().add(sharedMethodInterface);
				
				intersectionResults.add(sharedMethodInterface);
			}
		}
		structureContents.addAll(intersectionResults);
		structureContents.add(newInterface);	
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
            List<String> extendsOrImplementsA = implementingTypes.getOrDefault(interfaceA, new ArrayList<String>());
            if(!isEmpty(extendsOrImplementsA)) {
	            for(int b=a+1; b<structureContents.size(); b++) {
	                InterfaceDefinition interfaceB = structureContents.get(b);
	                List<String> extendsOrImplementsB = implementingTypes.getOrDefault(interfaceB, new ArrayList<String>());
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
                if(isEmpty(definition.getMethodSignatures())) {
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
         HashMap<InterfaceDefinition, List<String>> newImplementingTypes = new HashMap<>();
         newImplementingTypes.putAll(implementingTypes);
         
    	implementingTypes = newImplementingTypes;
        List<String> implementsReplaceThis = implementingTypes.getOrDefault(replaceThis, new ArrayList<>());
        List<String> implementsWith =        implementingTypes.getOrDefault(with, new ArrayList<>());
        implementsWith.addAll(implementsReplaceThis);
        implementingTypes.remove(replaceThis);
        implementingTypes.put(with, implementsWith);

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
    
    public HashMap<InterfaceDefinition, List<String>> getImplementingTypes() {
    	return implementingTypes;
    }
    
    public void setImplementingTypes(HashMap<InterfaceDefinition, List<String>> implementingTypes) {
    	this.implementingTypes = implementingTypes;
    }
}