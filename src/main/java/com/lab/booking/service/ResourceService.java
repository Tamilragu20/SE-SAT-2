package com.lab.booking.service;

import com.lab.booking.model.Resource;
import com.lab.booking.model.Schedule;
import com.lab.booking.repository.ResourceRepository;
import com.lab.booking.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ScheduleRepository scheduleRepository;

    public ResourceService(ResourceRepository resourceRepository, ScheduleRepository scheduleRepository) {
        this.resourceRepository = resourceRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public List<Resource> getAllActiveResources() {
        return resourceRepository.findByActiveTrue();
    }

    public List<Resource> searchResources(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllActiveResources();
        }
        return resourceRepository.searchActiveResources(keyword.trim());
    }

    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id).orElse(null);
    }

    public Resource getResourceByReference(String reference) {
        return resourceRepository.findByReference(reference).orElse(null);
    }

    public List<Schedule> getSchedulesForResource(Long resourceId) {
        return scheduleRepository.findByResourceIdAndActiveTrue(resourceId);
    }

    @Transactional
    public Resource updateResourceRate(Long resourceId, BigDecimal newRate) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));
        resource.setRate(newRate);
        return resourceRepository.save(resource);
    }

    @Transactional
    public Resource updateResourceAvailability(Long resourceId, int newAvailability) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Resource not found"));
        resource.setAvailability(newAvailability);
        return resourceRepository.save(resource);
    }
}
