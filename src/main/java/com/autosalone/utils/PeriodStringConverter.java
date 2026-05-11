package com.autosalone.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Period;

@Converter
public class PeriodStringConverter implements AttributeConverter<Period, String> {

    @Override
    public String convertToDatabaseColumn(Period attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public Period convertToEntityAttribute(String dbColumnValue) {
        return dbColumnValue == null || dbColumnValue.isEmpty() ? null : Period.parse(dbColumnValue);
    }
}
