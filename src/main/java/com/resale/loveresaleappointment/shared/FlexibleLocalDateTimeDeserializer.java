package com.resale.loveresaleappointment.shared;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
            DateTimeFormatter.ISO_DATE_TIME,                      // 2025-11-07T14:30:00.000+00:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),   // 2025-11-14 01:01:52
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),   // 2025/11/14 01:01:52
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),   // 14-11-2025 01:01:52
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")    // 14/11/2025 01:01:52
    );

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String date = p.getText().trim();

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                // For ISO formats containing zone offset
                if (date.contains("+") || date.contains("Z")) {
                    try {
                        return OffsetDateTime.parse(date).toLocalDateTime();
                    } catch (Exception ignored) {}
                    try {
                        return ZonedDateTime.parse(date).toLocalDateTime();
                    } catch (Exception ignored) {}
                }

                return LocalDateTime.parse(date, formatter);
            } catch (Exception ignored) {
            }
        }

        throw new RuntimeException("Unsupported date format: " + date);
    }
}


