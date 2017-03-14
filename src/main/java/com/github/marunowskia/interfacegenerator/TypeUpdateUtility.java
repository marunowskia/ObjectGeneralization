package com.github.marunowskia.interfacegenerator;
import static org.apache.commons.lang3.StringUtils.*;
import static com.google.common.base.Preconditions.*;
import static java.util.Optional.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
public class TypeUpdateUtility {

	public static String updateType(String type, Hashtable<String, InterfaceDefinition> replacements) {
		checkNotNull(type, "type not specified");
		type = type.trim();
		checkArgument(Character.isJavaIdentifierStart(type.charAt(0)), "Type is not a valid java type.");
		checkArgument(checkBalancedBrackets(type), "Brackets are not balanced");
		
		// FIXME: There are invalid java type string that can get past these input checks.
		
		String outerComponent = substringBefore(type, "<");
		outerComponent = ofNullable(replacements.get(outerComponent.trim())).map(def -> def.getName()).orElse(outerComponent);
		
//<<<<<<< Updated upstream
		StringBuilder resultBuilder = new StringBuilder();
		resultBuilder.append(outerComponent);
//=======
//		if(oldType.matches(".*<.*>.*")) {
//			checkArgument(bracketsAreBalanced(oldType));
//			
//			String subType = StringUtils.substringAfter(bracketsRemoved, "<");
//			String updatedSubTypes = Arrays.stream(subType.split(","))
//					.map(s->s.trim())
//					.map(s->updateInnerTypeComponent(s, replacements))
//					.reduce("", (result, data) -> result + ", " + data);
//			updatedSubTypes.replaceFirst(",", "");
//			
//			return
//				"<"
//			+	updateFirstInnerType(StringUtils.substringBefore(bracketsRemoved, "<"), replacements) // UPDATE THE TYPE BEING GENERICIZED
//			+	(StringUtils.isEmpty(subType) ? "":updateInnerTypeComponent("<"+StringUtils.substringAfter(bracketsRemoved, "<"), replacements)) // UPDATE THE TYPE OF THE GENERIC COMPONENTS
//			+	">";	
//		}
//		else {
//			 
//		}
//>>>>>>> Stashed changes
		
		if(type.matches(".+<.*>")) {
			String innerComponent =  substringBeforeLast(substringAfter(type, "<"), ">");
			innerComponent = updateGenericList(innerComponent, replacements);
			resultBuilder	.append('<')
							.append(innerComponent)
							.append('>');
		}
		
		return normalizeSpace(resultBuilder.toString());
	}
	
	public static String updateGenericList(String genericList, Hashtable<String, InterfaceDefinition> replacements) {
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
	
	public static String updateGeneric(String generic, Hashtable<String, InterfaceDefinition> replacements) {
		
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
