package com.lifelink.util;

import com.lifelink.enums.BloodGroup;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Stores the human-readable blood group value (e.g. "A+") in the database.
 */
@Converter(autoApply = true)
public class BloodGroupConverter implements AttributeConverter<BloodGroup, String> {

    @Override
    public String convertToDatabaseColumn(BloodGroup attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public BloodGroup convertToEntityAttribute(String dbData) {
        return dbData == null ? null : BloodGroup.from(dbData);
    }
}
