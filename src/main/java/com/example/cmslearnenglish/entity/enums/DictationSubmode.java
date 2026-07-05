package com.example.cmslearnenglish.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the dictation exercise submode.
 * Supports JSON serialization/deserialization with custom values.
 */
public enum DictationSubmode {
    FULL("full"),
    FILL_BLANK("fill-blank");
    
    private final String value;
    
    DictationSubmode(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Serialize enum to JSON using the custom value.
     */
    @JsonValue
    public String toJson() {
        return value;
    }
    
    /**
     * Deserialize JSON string to enum.
     * @param value the JSON string value
     * @return the corresponding DictationSubmode
     * @throws IllegalArgumentException if the value is invalid
     */
    @JsonCreator
    public static DictationSubmode fromJson(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Submode cannot be null");
        }
        
        for (DictationSubmode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        
        throw new IllegalArgumentException("Invalid submode: " + value + ". Valid values are: full, fill-blank");
    }
}
