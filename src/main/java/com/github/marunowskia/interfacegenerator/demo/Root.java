package com.github.marunowskia.interfacegenerator.demo;
import com.github.marunowskia.interfacegenerator.demo.Leaf1;
import com.github.marunowskia.interfacegenerator.demo.Middle;
import com.github.marunowskia.interfacegenerator.demo.Middle12;
import com.github.marunowskia.interfacegenerator.demo.Middle123;
import com.github.marunowskia.interfacegenerator.demo.Middle23;
public class Root {
	public Leaf1 getSomethingNew() {return new Leaf1();}
	public Middle getMiddle() {return new Middle();}
	public Middle12 getMiddle12() {return new Middle12();}
	public Middle23 getMiddle23() {return new Middle23();}
	public Middle123 getMiddle123() {return new Middle123();}

}