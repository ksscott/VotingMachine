package model;

import java.util.Collection;
import java.util.Set;

public record Race(String name, Set<Option> options) {

    // public Race(String name, Option... options) {
    //     this(name, Set.of(options));
    // }
    
    // public Race(String name, Collection<Option> options) {
    //     this(name, Set.copyOf(options));
    // }
}
