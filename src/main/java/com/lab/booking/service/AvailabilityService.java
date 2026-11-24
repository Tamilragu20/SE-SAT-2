package com.lab.booking.service;

import com.lab.booking.exception.ResourceUnavailableException;
import com.lab.booking.model.Resource;
import com.lab.booking.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private final ResourceRepository resourceRepository;

    public AvailabilityService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public boolean checkAvailability(Long resourceId, int requestedQuantity) {
        if (resourceId == null || requestedQuantity <= 0) {
            return false;
        }
        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null || !resource.isActive()) {
            return false;
        }
        return resource.getAvailability() >= requestedQuantity;
    }

    public Resource getAvailableResourceOrThrow(Long resourceId, int requestedQuantity) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceUnavailableException("Equipment slot not found with id: " + resourceId));

        if (!resource.isActive()) {
            throw new ResourceUnavailableException("Equipment slot " + resource.getReference() + " is currently inactive and cannot be booked.");
        }

        if (resource.getAvailability() < requestedQuantity) {
            throw new ResourceUnavailableException("Insufficient availability for " + resource.getReference() + " (" + resource.getName() + "). " +
                    "Requested: " + requestedQuantity + ", Available: " + resource.getAvailability());
        }

        return resource;
    }

    @Transactional
    public void updateAvailability(Long resourceId, int quantityToDeduct) {
        Resource resource = getAvailableResourceOrThrow(resourceId, quantityToDeduct);
        resource.setAvailability(resource.getAvailability() - quantityToDeduct);
        resourceRepository.save(resource);
    }

    @Transactional
    public void restoreAvailability(Long resourceId, int quantityToRestore) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceUnavailableException("Equipment slot not found with id: " + resourceId));
        resource.setAvailability(resource.getAvailability() + quantityToRestore);
        resourceRepository.save(resource);
    }
}
