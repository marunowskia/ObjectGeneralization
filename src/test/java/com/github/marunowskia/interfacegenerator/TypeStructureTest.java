package com.github.marunowskia.interfacegenerator;
//import static org.hamcrest.core.Is.*;
//import static org.junit.Assert.*;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;

public class TypeStructureTest {

	
	private static volatile int signatureCounter = 0 ; 
	public List<String> uniqueMethodSignatures(int count) {
	
		List<String> result = new ArrayList<>();
		char start = 'A';
		for(int a=0; a<count; a++) {
			result.add("get" + (start+a%26) + signatureCounter++);
		}
		return result;
	}
	
	@Test
	public void testAddToEmpty() {
		TypeStructure underTest = new TypeStructure();
		
		InterfaceDefinition input = new InterfaceDefinition();
		input.setName("input");
		input.getMethodSignatures().addAll(uniqueMethodSignatures(3));
		
		underTest.add(input);
		List<InterfaceDefinition> result = underTest.getTypeStructure();

		assertNotNull(result);		
		assertThat(result.size(), is(1));
		assertThat(result.get(0), is(input));
	}
	
	@Test
	public void testAddNewInterface() {
		TypeStructure underTest = new TypeStructure();
		
	}
	
	@Test
	public void testAddMultipleTimes() {
		TypeStructure underTest = new TypeStructure();
		
		List<String> repeatMethodSignatures = uniqueMethodSignatures(5);
		List<String> nonRepeatMethodSignatures = uniqueMethodSignatures(3);
		
		InterfaceDefinition input = new InterfaceDefinition();
		input.setName("input");
		input.getMethodSignatures().addAll(repeatMethodSignatures);
		underTest.add(input);
		
		input = new InterfaceDefinition();
		input.setName("input2");
		input.getMethodSignatures().addAll(nonRepeatMethodSignatures);
		underTest.add(input);
		
		input = new InterfaceDefinition();
		input.setName("input3");
		input.getMethodSignatures().addAll(repeatMethodSignatures);
		underTest.add(input);

		
		input = new InterfaceDefinition();
		input.setName("input3");
		input.getMethodSignatures().addAll(repeatMethodSignatures);
		underTest.add(input);

		List<InterfaceDefinition> result = underTest.getTypeStructure();
		assertNotNull(result);
		assertThat(result.size(), is(2));
	}
	
	@Test
	public void testOrderIndependence() {
		TypeStructure underTest = new TypeStructure();
	}
	
}
