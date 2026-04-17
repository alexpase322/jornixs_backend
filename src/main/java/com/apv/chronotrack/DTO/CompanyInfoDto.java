package com.apv.chronotrack.DTO;

import com.apv.chronotrack.models.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyInfoDto {
    private Long id;
    private String companyName;
    private String address;
    private String phoneNumber;
    private String ein;
    private String logoUrl;
    private String subscriptionPlan;
    private SubscriptionStatus subscriptionStatus;
    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String planPriceId;
    private Double workLatitude;
    private Double workLongitude;
    private Double geofenceRadiusMeters;
}
