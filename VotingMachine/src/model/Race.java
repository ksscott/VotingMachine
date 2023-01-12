package model;

import org.apache.commons.collections4.set.UnmodifiableSet;

import java.util.Collection;
import java.util.Set;

public record Race(String name, UnmodifiableSet<Option> options) {

    public Race(String name, Option... options) {
        this(name, (UnmodifiableSet<Option>) Set.of(options));
    }
    
    public Race(String name, Collection<Option> options) {
        this(name, (UnmodifiableSet<Option>) Set.copyOf(options));
    }
}
