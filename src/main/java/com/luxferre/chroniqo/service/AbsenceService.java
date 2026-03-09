package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.AbsenceRepository;
import com.luxferre.chroniqo.service.user.UserService;
import com.luxferre.chroniqo.util.IsWeekendQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AbsenceService {

    private final AbsenceRepository repository;
    private final UserService userService;

    @Transactional
    public void saveAbsence(AbsenceRequest absenceRequest) {
        User user = userService.getCurrentUser();

        List<Absence> existingAbsence = repository.findByUserAndDateBetween(user, absenceRequest.startDate(), absenceRequest.endDate());
        if (!existingAbsence.isEmpty()) {
            repository.deleteAll(existingAbsence);
        }

        repository.saveAll(absenceRequest.startDate().datesUntil(absenceRequest.endDate().plusDays(1)).filter(date -> !date.query(new IsWeekendQuery())).map(date -> {
            Absence absence = new Absence();
            absence.setUser(user);
            absence.setDate(date);
            absence.setType(absenceRequest.absenceType());
            return absence;
        }).toList());
    }

    @Transactional
    public void deleteAbsence(LocalDate date) {
        deleteAbsences(date, date);
    }

    @Transactional
    public void deleteAbsences(LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentUser();
        repository.deleteAll(repository.findByUserAndDateBetween(user, startDate, endDate));
    }

    public List<Absence> getAbsences(LocalDate start, LocalDate end) {
        User user = userService.getCurrentUser();
        return repository.findByUserAndDateBetween(user, start, end);
    }

    public Absence getAbsence(LocalDate date) {
        User user = userService.getCurrentUser();
        return repository.findByUserAndDate(user, date);
    }
}
