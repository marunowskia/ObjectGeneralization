//package com.github.marunowskia.interfacegenerator;
//
//import java.util.Hashtable;
//
//import org.apache.commons.lang3.StringUtils;
//
//import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;
//
//public class TypeEquivalenceUtility {
//	
//	public static boolean typesAreEquivalent(String typeLeft, String typeRight) {
//		
//	}
//
//	public static boolean methodsAreEquivalent(String signatureLeft, String signatureRight, Hashtable<String, InterfaceDefinition> replacements) {
//		// TODO: validate inputs
//		
//		
//		String[] signatureLeftParts = getMethodSignatureParts(signatureLeft);
//		String[] signatureRightParts = getMethodSignatureParts(signatureRight);
//
//		String returnTypeLeft = signatureLeftParts[0];
//		String returnTypeRight = signatureRightParts[0];
//		
//		String methodNameLeft = signatureLeftParts[1];
//		String methodNameRight = signatureRightParts[1];
//		
//		if(!StringUtils.equals(methodNameLeft, methodNameRight)) {
//			return false;
//		}
//
//		returnTypeLeft = TypeUpdateUtility.updateType(returnTypeLeft, replacements);
//		returnTypeRight = TypeUpdateUtility.updateType(returnTypeRight, replacements);
//		return StringUtils.equals(returnTypeLeft, returnTypeRight)
//	}
//	
//	// TODO: replace this with a parser library? Can we make use of "JavaParser"?
//	private static String[] getMethodSignatureParts(String methodSignature) {
//		String paredSignature = StringUtils.normalizeSpace(methodSignature);
//		//public ReturnType methodName(); ==> ReturnType methodName();
//		if(paredSignature.startsWith("public")) {
//			paredSignature.replaceFirst("public", "");
//		}
//		
//		//ReturnType methodName' '?(.* ==> returnType methodName
//		paredSignature = StringUtils.substringBefore(paredSignature, "(")
//						  .trim(); // Remove possible space after methodName
//		
//		return paredSignature.split(" ");
//	}
//}
