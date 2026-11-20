package com.rit.token.ritz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebSpaController {

    @GetMapping(value = {"/dev-portal", "/dev-portal/", "/dev-portal/{path:[^\\.]*}"})
    public String forwardDevPortal() {
        return "forward:/index.html";
    }

    @GetMapping(value = {"/admin-portal", "/admin-portal/", "/admin-portal/{path:[^\\.]*}"})
    public String forwardAdminPortal() {
        return "forward:/admin-portal/index.html";
    }
}
