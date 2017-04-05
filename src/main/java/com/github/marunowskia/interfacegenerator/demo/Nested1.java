package com.github.marunowskia.interfacegenerator.demo;

public class Nested1 {

    public String getNested1String() {
        return "Nested1String";
    }

    public NestedInner2 getNestedInner2() {
        return new  NestedInner2();
    }

    public static class NestedInner1 {

        public String getNestedInner1String() {
            return "NestedInner1String";
        }
    }

    public static class NestedInner2 {

        public String getNestedInner1String() {
            return "NestedInner1String";
        }
    }
}
