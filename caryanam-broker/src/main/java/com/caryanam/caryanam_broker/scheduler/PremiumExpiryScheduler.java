package com.caryanam.caryanam_broker.scheduler;

import com.caryanam.caryanam_broker.entity.Property;
import com.caryanam.caryanam_broker.entity.PropertyOwner;
import com.caryanam.caryanam_broker.enums.PremiumStatus;
import com.caryanam.caryanam_broker.repository.PropertyOwnerRepository;
import com.caryanam.caryanam_broker.repository.PropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class PremiumExpiryScheduler {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyOwnerRepository propertyOwnerRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void expireSubscriptions() {
        System.out.println("Starting auto-expiry scheduler job...");
        LocalDateTime now = LocalDateTime.now();

        List<PremiumStatus> activeStatuses = Arrays.asList(PremiumStatus.FREE_ACTIVE, PremiumStatus.ACTIVE);
        List<Property> expiredProperties = propertyRepository
                .findByPremiumEndDateBeforeAndPremiumStatusIn(now, activeStatuses);

        for (Property property : expiredProperties) {
            property.setPremiumStatus(PremiumStatus.EXPIRED);
            property.setPremiumActive(false);
            propertyRepository.save(property);

            System.out.println("Expired premium subscription for property ID: " + property.getId());

            PropertyOwner owner = property.getPropertyOwner();
            if (owner != null) {
                // Check if owner has any remaining active premium properties
                boolean hasActivePremiumProperty = false;
                List<Property> ownerProperties = propertyRepository
                        .findByPropertyOwner_OwnerIdOrderByCreatedAtDesc(owner.getOwnerId());

                for (Property prop : ownerProperties) {
                    if (prop.getPremiumStatus() == PremiumStatus.FREE_ACTIVE
                            || prop.getPremiumStatus() == PremiumStatus.ACTIVE) {
                        hasActivePremiumProperty = true;
                        break;
                    }
                }

                if (!hasActivePremiumProperty) {
                    owner.setPremiumActive(false);
                    owner.setPremiumStatus("EXPIRED");
                    propertyOwnerRepository.save(owner);
                    System.out.println("Deactivated global premium status for owner ID: " + owner.getOwnerId());
                }
            }
        }
        System.out.println("Finished auto-expiry scheduler job.");
    }
}
