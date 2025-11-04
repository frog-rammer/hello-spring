package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZonedDateTime;

@Controller
public class MainController {

    @GetMapping({"/", "/main"})
    public String main(Model model) {
        model.addAttribute("appName", "Hello Spring");
        model.addAttribute("now", ZonedDateTime.now());
        return "main";  // -> resources/templates/main.html
    }
}
