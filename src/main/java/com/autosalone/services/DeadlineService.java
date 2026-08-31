package com.autosalone.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.autosalone.dtos.requests.DeadlineRequest;
import com.autosalone.dtos.responses.DeadlineCompletionResponse;
import com.autosalone.dtos.responses.DeadlineResponse;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Deadline;
import com.autosalone.repositories.DeadlineRepository;
import com.autosalone.repositories.OwnerRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DeadlineService {

    @Inject
    private DeadlineRepository deadlineRepository;

    @Inject
    private OwnerRepository ownerRepository;

    // read

    public Deadline getDeadlineById(UUID id) {
        return deadlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deadline not found of id: " + id));
    }

    public List<DeadlineResponse> getDeadlinesByVehicleId(UUID vehicleId, boolean completed, UUID currentUserId,
            boolean isOwner) {

        List<Deadline> deadlines = completed
                ? deadlineRepository.findHistoryByVehicleId(vehicleId)
                : deadlineRepository.findPendingByVehicleId(vehicleId);

        Set<UUID> authorIds = deadlines.stream()
                .map(Deadline::getUpdatedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> ownerAuthorIds = isOwner && !authorIds.isEmpty()
                ? ownerRepository.filterOwnerIds(authorIds)
                : Set.of();

        return deadlines.stream().map(deadline -> {
            boolean includeNotes = false;
            UUID updatedBy = deadline.getUpdatedBy();

            if (isOwner) {
                includeNotes = updatedBy == null || ownerAuthorIds.contains(updatedBy);
            } else {
                includeNotes = currentUserId.equals(updatedBy);
            }

            return DeadlineResponse.fromEntity(deadline, includeNotes);
        }).toList();
    }

    public List<DeadlineResponse> getUrgentDeadlines(LocalDate upToDate) {
        return deadlineRepository.findUrgentDeadlines(upToDate).stream().map(deadline -> DeadlineResponse.fromEntity(
                deadline, true)).toList();
    }

    // write

    @Transactional
    public DeadlineCompletionResponse completeDeadline(UUID deadlineId, LocalDate completionDate, String notes) {
        Deadline deadline = getDeadlineById(deadlineId);

        Deadline nextRecurrence = deadline.complete(completionDate, notes);
        deadlineRepository.save(deadline);

        if (nextRecurrence != null) {
            deadlineRepository.save(nextRecurrence);
        }

        return new DeadlineCompletionResponse(
                DeadlineResponse.fromEntity(deadline, true),
                DeadlineResponse.fromEntity(nextRecurrence, true));
    }

    @Transactional
    public DeadlineResponse updateDeadline(UUID deadlineId, DeadlineRequest request) {
        Deadline deadline = getDeadlineById(deadlineId);

        deadline.setReason(request.reason());
        deadline.setDueDate(request.dueDate());
        deadline.setRecurrence(request.recurrence());
        deadline.setRecalculateFromCompletion(request.recalculateFromCompletion());

        deadlineRepository.save(deadline);
        return DeadlineResponse.fromEntity(deadline, true);
    }

    @Transactional
    public void deleteDeadline(UUID id) {
        Deadline deadline = getDeadlineById(id);
        deadlineRepository.delete(deadline);
    }
}