package com.apv.chronotrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;

    @Value("${geocoding.nominatim.base-url:https://nominatim.openstreetmap.org/search}")
    private String geocodingBaseUrl;

    @Value("${geocoding.user-agent:JornixsBackend/1.0 (contact@jornixs.com)}")
    private String geocodingUserAgent;

    public GeocodingResult geocodeAddress(String address) {
        if (!StringUtils.hasText(address)) {
            throw new IllegalArgumentException("You must provide a valid address to geocode.");
        }

        RestTemplate restTemplate = restTemplateBuilder.build();
        String url = UriComponentsBuilder.fromHttpUrl(geocodingBaseUrl)
                .queryParam("q", address)
                .queryParam("format", "jsonv2")
                .queryParam("limit", 1)
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", geocodingUserAgent);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.isArray() || root.isEmpty()) {
                throw new IllegalArgumentException("No location found for the provided address.");
            }

            JsonNode first = root.get(0);
            double latitude = Double.parseDouble(first.path("lat").asText());
            double longitude = Double.parseDouble(first.path("lon").asText());
            String normalizedAddress = first.path("display_name").asText(address);

            return new GeocodingResult(latitude, longitude, normalizedAddress);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not geocode the address at this time.", ex);
        }
    }

    public record GeocodingResult(Double latitude, Double longitude, String normalizedAddress) {
    }
}
