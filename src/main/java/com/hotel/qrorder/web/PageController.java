package com.hotel.qrorder.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
    /** QR scan karne pe /t/{token} khulta hai, customer page serve hota hai. */
    @GetMapping("/t/{token}")
    public String customer() {
        return "forward:/customer.html";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login.html";
    }
}
