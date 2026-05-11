package com.autosalone.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

public class VehicleTest {

    @Test
    public void generateStandardInspectionDeadline_SetsInspectionTo4YearsEndOfMonth() {
        LocalDate registrationDate = LocalDate.of(2022, 4, 15); // Data di immatricolazione: 15 Aprile 2022

        Vehicle car = new Vehicle.VehicleBuilder()
                .setRegistrationDate(registrationDate)
                .build();

        car.generateStandardInspectionDeadline();

        assertNotNull(car.getDeadlines(), "Deadlines list must not be null");
        assertEquals(1, car.getDeadlines().size(), "Exactly one deadline should have been generated");

        Deadline inspectionDeadline = car.getDeadlines().get(0);

        // Expected: 2022-04-15 + 4 years = 2026-04-15 -> End of month = 2026-04-30
        LocalDate expectedDate = LocalDate.of(2026, 4, 30);

        assertEquals(inspectionDeadline.getReason(), "Revisione Veicolo");
        assertEquals(expectedDate,
                inspectionDeadline.getStartDate(),
                "The first inspection for a vehicle must be 4 years later at the end of the month");
        assertEquals(2, inspectionDeadline.getRecurrence().getYears(), "The subsequent recurrence must be 2 years");
    }

    @Test
    public void generateStandardInspectionDeadline_WithoutRegistrationDate_ThrowsException() {
        // Vehicle without a registration date
        Vehicle car = new Vehicle.VehicleBuilder()
                .build();

        assertThrows(IllegalStateException.class, () -> {
            car.generateStandardInspectionDeadline();
        });
    }
}