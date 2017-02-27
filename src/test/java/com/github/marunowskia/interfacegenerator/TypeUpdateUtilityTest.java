package com.github.marunowskia.interfacegenerator;

import java.util.Hashtable;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsEqual.*;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.marunowskia.interfacegenerator.InterfaceComposer.InterfaceDefinition;

public class TypeUpdateUtilityTest {

	@Rule
	public ExpectedException expectedException;
	
	private static Hashtable<String, InterfaceDefinition> replacements;
	@BeforeClass
	public static void init() {
		replacements = new Hashtable<>();
		replacements.put("Leaf", TestObjectSupplier.getInterface("ILeaf"));
		replacements.put("Node", TestObjectSupplier.getInterface("INode"));
		replacements.put("Root", TestObjectSupplier.getInterface("IRoot"));
	}
	
	@Test
	public void testUpdateSingleInnerTypeWithMatch() {
		String input        = "Leaf";
		String expected     = "? extends ILeaf";
		String theOutput    = TypeUpdateUtility.updateSingleInnerType(input, replacements);
		assertThat(theOutput, is(expected));
	}
	
	@Test
	public void testUpdateSingleInnerTypeWithNoMatch() {
		String input     = "Missing";
		String expected  = "Missing";
		String theOutput = TypeUpdateUtility.updateSingleInnerType(input, replacements);
		assertThat(theOutput, is(expected));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testUpdateSingleInnerTypeInvalid() {
		String input     = "Leaf<Root>";
		String expected  = "Leaf<Root>";
		String theOutput = TypeUpdateUtility.updateSingleInnerType(input, replacements);
		assertThat(theOutput, is(expected));
	}
	
	@Test
	public void testUpdateSingleInnerTypeWildcardWithMatch() {
		String input, expected, theOutput;

		input            = "? extends Leaf";
		expected         = "? extends ILeaf";
		theOutput        = TypeUpdateUtility.updateSingleInnerType(input, replacements);

		assertThat(theOutput, is(expected));

		input            = "? super Leaf";
		expected         = "? super Leaf";
		theOutput        = TypeUpdateUtility.updateSingleInnerType(input, replacements);
		
		assertThat(theOutput, is(expected));

		input            = "?";
		expected         = "?";
		theOutput        = TypeUpdateUtility.updateSingleInnerType(input, replacements);
		
		assertThat(theOutput, is(expected));
	}
	
	@Test
	public void testUpdateSingleInnerTypeWildcardNoMatch() {
		String input, expected, theOutput;
		
		input            = "? extends Missing";
		expected         = "? extends Missing";
		theOutput        = TypeUpdateUtility.updateSingleInnerType(input, replacements);
		
		assertThat(theOutput, is(expected));
		
		input            = "? super Missing";
		expected         = "? super Missing";
		theOutput        = TypeUpdateUtility.updateSingleInnerType(input, replacements);
		
		assertThat(theOutput, is(expected));
	}
	
	@Test
	public void testUpdateFullTypeWithMatches() {
		String input, expected, theOutput;
		
		input            = "Root<Leaf<Node>>";
		expected         = "IRoot<? extends ILeaf<? extends INode>>";
		theOutput        = TypeUpdateUtility.updateType(input, replacements);
		
		assertThat(theOutput, is(expected));
		
		input            = "Root<Leaf, Leaf>";
		expected         = "IRoot<? extends ILeaf, ? extends ILeaf>";
		theOutput        = TypeUpdateUtility.updateType(input, replacements);
		
		assertThat(theOutput, is(expected));
		
		input            = "Root<Leaf, Missing<Root>, Missing>";
		expected         = "IRoot<? extends ILeaf, Missing<? extends IRoot>, Missing";
		theOutput        = TypeUpdateUtility.updateType(input, replacements);
		
		assertThat(theOutput, is(expected));
		
		input            = "Missing<Missing<Leaf, Node>>";
		expected         = "Missing<Missing<? extends ILeaf, ? extends INode>>";
		theOutput        = TypeUpdateUtility.updateType(input, replacements);
		
		assertThat(theOutput, is(expected));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testUpdateFullTypeNotWellFormed() {
		String input = "? extends Object";
//		expectedException.expect(IllegalArgumentException.class);
		TypeUpdateUtility.updateType(input, replacements);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testUpdateFullTypeNotWellFormed0() {
		String input = "<>";
//		expectedException.expect(IllegalArgumentException.class);
		TypeUpdateUtility.updateType(input, replacements);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testUpdateFullTypeNotWellFormed1() {
		String input = "Root<? extends Leaf";
//		expectedException.expect(IllegalArgumentException.class);
		TypeUpdateUtility.updateType(input, replacements);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUpdateFullTypeNotWellFormed2() {
		String input = ">Root<? extends Leaf";
//		expectedException.expect(IllegalArgumentException.class);
		TypeUpdateUtility.updateType(input, replacements);
	}
	
	
	
	
	
}
