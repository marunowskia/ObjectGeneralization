package com.github.marunowskia.interfacegenerator;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;

import lombok.Getter;
import lombok.Setter;

@Getter
public class GenericMethod {

	private Map<String, String> classToFullyQualifiedClassMap;
	private Set<String> fullyQualifiedDependencies = new HashSet<>();
	private String fullyQualifiedTypeString;
	private String methodSignature;
	private String returnTypeString;
	private MethodDeclaration originalDeclaration;

	
	public synchronized void updateMethodSignature(Map<String, InterfaceDefinition> implementorMap) {
		returnTypeString = TypeUpdateUtility.updateType(fullyQualifiedTypeString, implementorMap);
		methodSignature = "public " + returnTypeString + " " + originalDeclaration.getName() + "()"; 	
	}
	
	public void setOriginalDeclaration(MethodDeclaration original, Map<String, String> classToFullyQualifiedClassMap) {
		this.originalDeclaration = original;
		this.classToFullyQualifiedClassMap = classToFullyQualifiedClassMap;
		this.fullyQualifiedTypeString = "";
		String originalTypeString = originalDeclaration.getType().toStringWithoutComments();
		
		String[] subtypes = originalTypeString.split("[^A-Za-z_0-9$]+");
		fullyQualifiedDependencies.clear();
		for(int a=0; a<subtypes.length; a++) {
			String originalSubtype = subtypes[a];
			fullyQualifiedTypeString += StringUtils.substringBefore(originalTypeString, originalSubtype);
			fullyQualifiedTypeString += classToFullyQualifiedClassMap.getOrDefault(originalSubtype, originalSubtype);
			fullyQualifiedDependencies.add(classToFullyQualifiedClassMap.getOrDefault(originalSubtype, originalSubtype));
			originalTypeString = StringUtils.substringAfter(originalTypeString, originalSubtype);
		}
		fullyQualifiedTypeString += originalTypeString;
		
		updateMethodSignature(null);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((methodSignature == null) ? 0 : methodSignature.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GenericMethod other = (GenericMethod) obj;
		if (methodSignature == null) {
			if (other.methodSignature != null)
				return false;
		} else if (!methodSignature.equals(other.methodSignature))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "GenericMethod [methodSignature=" + methodSignature + "]";
	}
	
	

	
}
