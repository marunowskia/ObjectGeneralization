package com.github.marunowskia.interfacegenerator;

import java.util.Collection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.google.common.io.Files;
import org.junit.Assert;

public interface IFakeInterface<E> {

    boolean add(E arg0);

    boolean remove(Object arg0);

    boolean containsAll(Collection<?> arg0);

    boolean addAll(Collection<? extends E> arg0);

    boolean addAll(int arg0, Collection<? extends E> arg1);

    boolean removeAll(Collection<?> arg0);

    boolean retainAll(Collection<?> arg0);
}