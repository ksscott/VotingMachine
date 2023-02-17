package model;

import com.fasterxml.jackson.annotation.JsonValue;

public record Option(String name) {

    @JsonValue // necessary to remove the class name from the serialization of this object
    public String getName() { return name; }
}
