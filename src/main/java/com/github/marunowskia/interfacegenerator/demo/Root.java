package com.github.marunowskia.interfacegenerator.demo;

import com.github.marunowskia.interfacegenerator.demo.Leaf1;
import com.github.marunowskia.interfacegenerator.demo.Middle123;
import com.github.marunowskia.interfacegenerator.demo.Middle23;
import com.github.marunowskia.interfacegenerator.demo.Middle12;
import com.github.marunowskia.interfacegenerator.demo.Empty1;

public class Root {

    public Leaf1 getSomethingNew() {
        return new  Leaf1();
    }

    public Middle12 get12() {
        return new  Middle12();
    }

    public Middle123 get123() {
        return new  Middle123();
    }

    public Middle23 get23() {
        return new  Middle23();
    }

    public Empty1 getEmpty1() { return new Empty1();}
}
