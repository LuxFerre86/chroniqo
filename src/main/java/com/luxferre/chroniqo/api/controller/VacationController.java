package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.api.dto.VacationEntitlementRequest;
import com.luxferre.chroniqo.dto.VacationAccountDTO;
import com.luxferre.chroniqo.service.VacationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for the vacation account of the authenticated user.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@RestController
@RequestMapping("/api/vacation")
@RequiredArgsConstructor
public class VacationController {

    private final VacationService vacationService;

    /**
     * Returns entitlement, taken, planned and remaining vacation days of a year.
     *
     * @param year the calendar year
     * @return the vacation account
     */
    @GetMapping
    public VacationAccountDTO account(@RequestParam int year) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100");
        }
        return vacationService.getAccount(year);
    }

    /**
     * Sets the annual vacation entitlement of a year.
     *
     * @param request the year and the entitlement in whole days
     */
    @PutMapping("/entitlement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setEntitlement(@Valid @RequestBody VacationEntitlementRequest request) {
        vacationService.setEntitlement(request.year(), request.days());
    }
}
