package model;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public class Option {

    @JsonValue // necessary to remove the class name from the serialization of this object
    private final String name;

    public Option(String name) { this.name = name; }

    public String name() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Option option)) return false;
        return Objects.equals(name, option.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
