package com.github.marunowskia.interfacegenerator;

import static org.apache.commons.collections4.CollectionUtils.*;

import java.util.*;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class Structure {

    private List<InterfaceDefinition> structureContents = new ArrayList<>();
    HashMap<InterfaceDefinition, List<String>> implementingTypes = new HashMap<>();

    public Structure() {

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
            List<String> extendsOrImplementsA = implementingTypes.get(interfaceA);
            for(int b=a+1; b<structureContents.size(); b++) {
                InterfaceDefinition interfaceB = structureContents.get(a);
                List<String> extendsOrImplementsB = implementingTypes.get(interfaceB);;

                if(isEqualCollection(extendsOrImplementsA, extendsOrImplementsB)) {
                    replacementPairs.put(interfaceA, interfaceB);
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

    private void updateStructure(InterfaceDefinition replaceThis, InterfaceDefinition with) {
        structureContents.stream().map(id->id.getMustExtend())
                .filter(mustExtend -> mustExtend.contains(replaceThis))
                .forEach(mustExtend -> {
                    mustExtend.remove(  replaceThis);
                    mustExtend.add(     with);
                });
    }

    private void updateImplementors(InterfaceDefinition replaceThis, InterfaceDefinition with) {
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
}