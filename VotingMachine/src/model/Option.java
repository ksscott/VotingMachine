package model;

import com.fasterxml.jackson.annotation.JsonValue;

public class Option {

    @JsonValue // necessary to remove the class name from the serialization of this object
    private final String name;

    public Option(String name) { this.name = name; }

    public String name() { return name; }
}
