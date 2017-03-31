package com.github.marunowskia.interfacegenerator;
import static org.apache.commons.lang3.StringUtils.*;
import static com.google.common.base.Preconditions.*;
import static java.util.Optional.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
public class TypeUpdateUtility {

	public static Set<String> getAllReferencedTypes(String type) {
		return Arrays.stream(type.split("[^A-Za-z_0-9$]+"))
			  .filter(StringUtils::isNotBlank)
			  .filter(ref -> !"extends".equals(ref))
			  .collect(Collectors.toSet());
	}
	
	
	public static String updateType(String type, Map<String, InterfaceDefinition> replacements) {
		checkNotNull(type, "type not specified");
		type = type.trim();
		
		
		checkArgument(Character.isJavaIdentifierStart(type.charAt(0)), "Type is not a valid java type.");
		checkArgument(checkBalancedBrackets(type), "Brackets are not balanced");
		
		// FIXME: There are invalid java type string that can get past these input checks.

		if(MapUtils.isEmpty(replacements)) {
			return type;  
		}
		
		String outerComponent = substringBefore(type, "<");
//		System.out.println("Replacement for " + type + ": " + replacements.get(type));
		outerComponent = ofNullable(replacements.get(outerComponent.trim())).map(def -> def.getName()).orElse(outerComponent);
		
		StringBuilder resultBuilder = new StringBuilder();
		resultBuilder.append(outerComponent);
		if(type.matches(".+<.*>")) {
			String innerComponent =  getGenericComponent(type);
			innerComponent = updateGenericList(innerComponent, replacements);
			resultBuilder	.append('<')
							.append(innerComponent)
							.append('>');
		}
		String result = normalizeSpace(resultBuilder.toString());
//		System.out.printf("Replacement for %s: %s\n", type, result);
		
		return result;
	}
	
	public static String getGenericComponent(String type) {
		return substringBeforeLast(substringAfter(type, "<"), ">");
	}
	
	public static String updateGenericList(String genericList, Map<String, InterfaceDefinition> replacements) {
		List<String> updatedGenerics = new ArrayList<>();
		
		StringBuilder genericTypeBuffer = new StringBuilder();
		int depth = 0;
		for(char c : genericList.toCharArray()) {
			if(c == '<') depth++;
			if(c == '>') depth--;
			if(c == ',' && depth == 0) {
				updatedGenerics.add(updateGeneric(genericTypeBuffer.toString(), replacements));
				genericTypeBuffer.setLength(0);
			}
			else {
				genericTypeBuffer.append(c);
			}
		}
		updatedGenerics.add(updateGeneric(genericTypeBuffer.toString(), replacements));
		return String.join(", ", updatedGenerics);
	}
	
	public static String updateGeneric(String generic, Map<String, InterfaceDefinition> replacements) {
		
		if(generic.equals    ("?"))          return generic;
		if(generic.startsWith("? super "))   return generic;
		if(generic.startsWith("? extends ")) return "? extends " + updateType(substringAfter(generic, "? extends "), replacements);
		
		String outerComponent = substringBefore(generic, "<");
		Optional<String> replacedOuterComponent = ofNullable(replacements.get(outerComponent.trim())).map(def -> def.getName());
		
		if(replacedOuterComponent.isPresent()) {
			// generic is a knowable type that can't be replaced with one of our interfaces
			return "? extends " + updateType(generic, replacements);
		}
		// generic is a knowable type that can't be replaced with one of our interfaces
		return updateType(generic, replacements);
	}
	
	private static boolean checkBalancedBrackets(String input) {
		
		return checkBalancedCharacters('<','>',input);
	}
	
	private static boolean checkBalancedCharacters(char left, char right, String input) {
		int unmatchedLeftCount = 0;
		for(char c : input.toCharArray()) {
			if(c==left) {
				unmatchedLeftCount ++;
			}
			else if(c==right) {
				unmatchedLeftCount --;
			}
			
			if(unmatchedLeftCount<0) {
				return false;
			}
		}
		
		return unmatchedLeftCount == 0;
	}
}
