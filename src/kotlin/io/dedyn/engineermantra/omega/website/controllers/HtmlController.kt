package io.dedyn.engineermantra.omega.website.controllers

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HtmlController {
    @GetMapping("/")
    fun root(model: Model): String{
        model["title"] = "Main"
        return "main"
    }

    @GetMapping("/clocktower")
    fun clocktower(model: Model): String{
        model["title"] = "Blood on the Clocktower"
        model["serverid"] = ""
        return "clocktower"
    }
}