package ru.dpoliwhi.authservice.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ManagerController {

    @GetMapping("/manager-data")
    @PreAuthorize("hasRole('MANAGER')")
    public String getManagerData() {
        return "Данные только для менеджеров";
    }

    @GetMapping("/user-data")
    @PreAuthorize("hasRole('USER')")
    public String getUserData() {
        return "Данные только для пользователей";
    }
}