package com.autosalone.services;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.autosalone.dtos.DeadlineRequest;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Deadline;
import com.autosalone.repositories.DeadlineRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DeadlineService {

    @Inject
    private DeadlineRepository deadlineRepository;

    // read

    public Deadline getDeadlineById(UUID id) {
        return deadlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deadline not found of id: " + id));
    }

    public List<Deadline> getDeadlinesByVehicleId(UUID vehicleId, boolean completed) {
        if (completed)
            return deadlineRepository.findHistoryByVehicleId(vehicleId);
        else
            return deadlineRepository.findPendingByVehicleId(vehicleId);
    }

    public List<Deadline> getUrgentDeadlines(LocalDate upToDate) {
        return deadlineRepository.findUrgentDeadlines(upToDate);
    }

    // write

    @Transactional
    public void completeDeadline(UUID deadlineId, LocalDate completionDate, String notes) {
        Deadline deadline = getDeadlineById(deadlineId);

        Deadline nextRecurrence = deadline.complete(completionDate, notes);
        deadlineRepository.save(deadline);

        if (nextRecurrence != null) {
            deadlineRepository.save(nextRecurrence);
        }
    }

    @Transactional
    public Deadline updateDeadline(UUID deadlineId, DeadlineRequest request) {
        Deadline deadline = getDeadlineById(deadlineId);

        deadline.setReason(request.reason());
        deadline.setDueDate(request.dueDate());
        deadline.setRecurrence(request.recurrence());
        deadline.setRecalculateFromCompletion(request.recalculateFromCompletion());

        deadlineRepository.save(deadline);
        return deadline;
    }
}