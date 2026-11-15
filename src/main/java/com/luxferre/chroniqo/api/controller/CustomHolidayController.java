package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.api.dto.CustomHolidayRequest;
import com.luxferre.chroniqo.dto.CustomHolidayDTO;
import com.luxferre.chroniqo.service.CustomHolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints for the custom days off of the authenticated user.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@RestController
@RequestMapping("/api/custom-holidays")
@RequiredArgsConstructor
public class CustomHolidayController {

    private final CustomHolidayService customHolidayService;

    @GetMapping
    public List<CustomHolidayDTO> list() {
        return customHolidayService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomHolidayDTO create(@Valid @RequestBody CustomHolidayRequest request) {
        return customHolidayService.create(request.name(), request.startDate(), request.endDate(), request.recurring());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        customHolidayService.delete(id);
    }
}
