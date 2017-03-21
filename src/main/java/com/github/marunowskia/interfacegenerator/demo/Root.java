package com.github.marunowskia.interfacegenerator.demo;
import com.github.marunowskia.interfacegenerator.demo.Leaf1;
import com.github.marunowskia.interfacegenerator.demo.Middle;
public class Root {
	public Leaf1 getSomethingNew() {return new Leaf1();}
	public Middle getMiddle() {return new Middle();}
}