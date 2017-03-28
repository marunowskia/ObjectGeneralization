package com.github.marunowskia.interfacegenerator;

import java.util.Map;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class GenericMethod {

	private String methodSignature;
	private MethodDeclaration originalDeclaration;
	
	public void updateMethodSignature(Map<String, InterfaceDefinition> implementorMap) {
		String updatedType = TypeUpdateUtility.updateType(originalDeclaration.getType().toStringWithoutComments(), implementorMap);
		methodSignature = "public " + updatedType + " " + originalDeclaration.getName() + "()"; 	
	}
	
	public void setOriginalDeclaration(MethodDeclaration original) {
		this.originalDeclaration = original;
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
