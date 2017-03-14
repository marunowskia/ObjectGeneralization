package com.github.marunowskia.interfacegenerator;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

/**
 * Created by marunal on 3/13/17.
 */
public class StructureTest {

    @Test
    public void testCollapseEmpty() {
        Structure after = new Structure();
        after.collapse();
        Structure before = new Structure(after);
        after.collapse();
        assertNothingChanged(before, after);
    }

    @Test
    public void testCollapseNothingToChange() {
        Structure after = new Structure();
        // TODO: setup structure
        Structure before = new Structure(after);
        after.collapse();
        assertNothingChanged(before, after);
    }

    @Test
    public void testCollapseHorizontally() {
        Structure after = new Structure();
        // TODO: setup structure
        Structure before = new Structure(after);
        after.collapse();

        // TODO: assert horizontal collapse

    }

    @Test
    public void testCollapseVeritcally() {
        Structure after = new Structure();
        // TODO: setup structure
        Structure before = new Structure(after);
        after.collapse();
        // TODO: assert veritcal collapse
    }

    @Test
    public void testAdd() {
        Structure underTest = new Structure();
    }

    public void assertNothingChanged(Structure before, Structure after) {
        assertTrue(CollectionUtils.isEqualCollection(before.getStructureContents(), after.getStructureContents()));
        assertTrue(CollectionUtils.isEqualCollection(before.getImplementingTypes().keySet(), after.getImplementingTypes().keySet()));
        before.getImplementingTypes().keySet().forEach(key -> {
            assertTrue(CollectionUtils.isEqualCollection(before.getImplementingTypes().get(key), after.getImplementingTypes().get(key)));
        });
    }
}
