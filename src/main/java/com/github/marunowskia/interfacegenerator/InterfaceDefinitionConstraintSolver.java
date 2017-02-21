package com.github.marunowskia.interfacegenerator;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;

public class InterfaceDefinitionConstraintSolver {

	public static List<InterfaceDefinition> satisfyConstraints(ValueGraph<String, List<String>> methodGraph) {
		// Constraints:
		// 1) All methods callable using a reference to the original object must be callable using a reference to the interface version of that object.
		// 2) Any type reference which can be replaced with an equivalent reference must be replaced.
		// 3) No method signature may occur in more than one InterfaceDefintion of the FINAL return value.
		// 4) Genericized methods in the original type definition must be genericized in its InterfaceDefintion in the FINAL return value.
		// 5) Mergeable InterfaceDefintion objects must be merged. Two InterfaceDefinitions may be merged if they contain the same method signatures and extend set of InterfaceDefinitions
		
		// Step 1: Create an InterfaceDefinition for each TypeDefinition from methodGraph.
		// These must already be genericized. 
		List<InterfaceDefinition> topLevelInterfaces = new ArrayList<>();
		InterfaceDefinition def = new InterfaceDefinition();
		def.setName("Sample<Type1, Type2 extends Type3>");
		Hashtable<String, InterfaceDefinition> typeToInterfaceMap = new Hashtable<>();
		
		// Step 2: Replace type references with their equivalent interfaces. 
		List<InterfaceDefinition>  interfaceOnlyTopLevelInterfaces = new ArrayList<>();
		for(InterfaceDefinition topLevelInterface : topLevelInterfaces) {
			
			// InternScopeType<InternScopeGeneric> ==> IInternScopeType<? extends IInternScopeGeneric>
			// InternScopeType<ExternScopeGeneric> ==> IInternScopeType<ExternScopeGeneric>
			// ExternScopeType<InternScopeGeneric> ==>  ExternScopeType<? extends IInternScopeGeneric>
			// ExternScopeType<ExternScopeGeneric> ==>  ExternScopeType<ExternScopeGeneric>
			
			topLevelInterface.setMethodSignatures(
			topLevelInterface.getMethodSignatures()
					.stream()
					.map(signature -> updateSignature(signature, typeToInterfaceMap))
					.collect(Collectors.toList())
			);

			// Generic Return type Something<Generic> become Something<? extends IGeneric>
			//  
		}  
	}
	
	private static String updateSignature(String methodSignature, Hashtable<String, InterfaceDefinition> replacements) {
		
	}
}
