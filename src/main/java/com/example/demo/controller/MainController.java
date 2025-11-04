package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.ZonedDateTime;
import java.util.Optional;

@Controller
public class MainController {

    @GetMapping({"/", "/main"})
    public String main(Model model, HttpServletRequest request) {
        model.addAttribute("appName", "Hello Spring");
        model.addAttribute("now", ZonedDateTime.now());

        // 안전하게 헤더 값을 모델에 주입
        String imageTag = Optional.ofNullable(request.getHeader("X-Image-Tag"))
                                  .filter(s -> !s.isBlank())
                                  .orElse("N/A");
        model.addAttribute("imageTag", imageTag);
        return "main"; // templates/main.html
    }
}
