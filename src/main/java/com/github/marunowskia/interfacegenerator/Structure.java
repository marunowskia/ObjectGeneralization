package com.github.marunowskia.interfacegenerator;

import static org.apache.commons.collections4.CollectionUtils.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
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

        // ========================================
        // 2) If interfaces A, B are under consideration, then A only extends B, and A declares no additional methods
        // ========================================
        for(InterfaceDefinition definition : structureContents) {
            if(size(definition.getMustExtend()) == 1) {
                if(isEmpty(definition.getMethodSignatures())) {
                    replace(definition, definition.getMustExtend().get(0));
                }
            }
        }
    }

    public void replace(InterfaceDefinition replaceThis, InterfaceDefinition with) {

    }

    public void replaceAll(HashMap<InterfaceDefinition, InterfaceDefinition> replacementPairs) {

    }
}