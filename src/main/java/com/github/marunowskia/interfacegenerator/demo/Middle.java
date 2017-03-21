package com.github.marunowskia.interfacegenerator.demo;
import com.github.marunowskia.interfacegenerator.demo.Leaf1;
import com.github.marunowskia.interfacegenerator.demo.Leaf2;
import com.github.marunowskia.interfacegenerator.demo.Leaf3;

public class Middle {
	public Leaf1 getLeaf1() {return new Leaf1();}
	public Leaf2 getLeaf2() {return new Leaf2();}
	public Leaf3 getOther() {return new Leaf3();}
}
