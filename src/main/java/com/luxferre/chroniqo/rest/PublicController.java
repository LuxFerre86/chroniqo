package com.luxferre.chroniqo.rest;

import com.luxferre.chroniqo.service.CountrySubdivisionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Public REST endpoints that do not require authentication.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final CountrySubdivisionRegistry countryRegistry;

    @GetMapping("/countries")
    public ResponseEntity<List<CountryDTO>> getCountries() {
        List<CountryDTO> countries = countryRegistry.getAllCountries().entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> {
                    String code = entry.getKey();
                    String name = entry.getValue();
                    List<SubdivisionDTO> subdivisions = countryRegistry.getSubdivisions(code)
                            .entrySet().stream()
                            .sorted(Map.Entry.comparingByValue())
                            .map(s -> new SubdivisionDTO(s.getKey(), s.getValue()))
                            .toList();
                    return new CountryDTO(code, name, subdivisions);
                })
                .toList();
        return ResponseEntity.ok(countries);
    }

    public record SubdivisionDTO(String code, String name) {}
    public record CountryDTO(String code, String name, List<SubdivisionDTO> subdivisions) {}
}
