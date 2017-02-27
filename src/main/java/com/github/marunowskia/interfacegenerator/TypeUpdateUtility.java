package com.github.marunowskia.interfacegenerator;


import static com.google.common.base.Preconditions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
import com.google.common.base.Preconditions;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TypeUpdateUtility {
	private static final Logger log = LoggerFactory.getLogger(TypeUpdateUtility.class);
	
	
//	public static <? extends Object> test() {return null};
	public static String updateType(String oldType, Hashtable<String, InterfaceDefinition> replacements) {
		checkArgument(bracketsAreBalanced(oldType));
		checkArgument(!StringUtils.isEmpty(StringUtils.substringBefore(oldType, "<")));
		// Every non-keyword and non-symbol in the type-string needs to be replaced with the interface version of the same type.
		// Genericizaction parameters need to be replaced with their lower-bounded wildcard alternatives ("read only generic")
		
		// XXX: Remove comments strings from type definitions
		
		StringBuilder updatedTypeBuilder = new StringBuilder();
		updatedTypeBuilder.append(updateOuterTypeComponent(StringUtils.substringBefore(oldType, "<"), replacements));
		
		if(oldType.matches(".+<.*>")) {
			updatedTypeBuilder.append(updateInnerTypeComponent("<"+StringUtils.substringAfter(oldType, "<"), replacements));
		}
		
		return updatedTypeBuilder.toString();  
	}
	
	/**
	 * Inner type 
	 * @param oldType. Always starts with < and ends with >
	 * @param replacements
	 * @return
	 */
	private static String updateInnerTypeComponent(String oldType, Hashtable<String, InterfaceDefinition> replacements) {
		// Example: oldType = "<T0, ? extends T1<T2 extends T3>>"
		
		
		String bracketsRemoved = StringUtils.substringBeforeLast(StringUtils.substringAfter(oldType, "<"), ">");
		// Example: bracketsRemoved = "T0, ? extends T1<T2 extends T3>"
		
		
		if(oldType.matches(".*<.*>.*")) {
			checkArgument(bracketsAreBalanced(oldType));
			
			String subType = StringUtils.substringAfter(bracketsRemoved, "<");
			String updatedSubTypes = Arrays.stream(subType.split(","))
					.map(s->s.trim())
					.map(s->updateInnerTypeComponent(s, replacements))
					.reduce("", (result, data) -> result + ", " + data);
			updatedSubTypes.replaceFirst(",", "");
			
			return
				"<"
			+	updateFirstInnerType(StringUtils.substringBefore(bracketsRemoved, "<"), replacements) // UPDATE THE TYPE BEING GENERICIZED
			+	(StringUtils.isEmpty(subType) ? "":updateInnerTypeComponent("<"+StringUtils.substringAfter(bracketsRemoved, "<"), replacements)) // UPDATE THE TYPE OF THE GENERIC COMPONENTS
			+	">";	
		}
		else {
			
		}
		
		return
				updateFirstInnerType(StringUtils.substringBefore(bracketsRemoved, "<"), replacements);
	}
	
	private static String updateFirstInnerType(String innerType, Hashtable<String, InterfaceDefinition> replacements) {
		return updateSingleInnerType(innerType, replacements);
	}
	
	private static String updateRemainingInnerTypes(String innerType, Hashtable<String, InterfaceDefinition> replacements) {
		if(StringUtils.isEmpty(innerType)) {
			return innerType;
		}
		
		StringUtils.substringBefore(innerType, "<");
		return null;
	}
	
	public static String updateOuterTypeComponent(String outerType, Hashtable<String, InterfaceDefinition> replacements) {
		
		checkArgument(Character.isJavaIdentifierStart(outerType.charAt(0)));
		checkArgument(outerType.substring(1).matches("[A-Za-z0-9_$]*"));
		if(outerType.matches(".*<.*>.*")) {
			throw new IllegalArgumentException("Tried to perform a lookup-replacement on a Type string that included generic parameters");
		}
		outerType = outerType.replaceAll("[^A-Za-z0-9_$]", "");
		Optional<String> replacementOuterType = Optional.ofNullable(replacements.get(outerType)).map(i->i.getName());
		String newOuterType = replacementOuterType.orElse(outerType);
		return newOuterType;
	}
	
	public static String updateSingleInnerType(String singleType, Hashtable<String, InterfaceDefinition> replacements) {
		if(singleType.startsWith("? super ")) {
			return singleType;
		}
		
		if(singleType.equals("?")) {
			return "?";
		}
		
		if(singleType.startsWith("? extends ")) {
			String typeSubOf = StringUtils.substringAfter(singleType, "? extends ");
			String result = "? extends " + updateOuterTypeComponent(typeSubOf, replacements);
			log.info("updateSingleInnerType: {} ==> {}", singleType, result);
			return result;
		}
		
		String replacementType = updateOuterTypeComponent(singleType, replacements);
		if(replacementType.equals(singleType)) {
			return singleType;
		}
		return "? extends " + updateOuterTypeComponent(singleType, replacements);
	}
	
	private static boolean bracketsAreBalanced(String input) {
		return checkBalancedCharacters('<', '>', input);
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
