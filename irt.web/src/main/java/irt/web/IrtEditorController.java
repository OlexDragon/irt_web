package irt.web;

import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/editor")
public class IrtEditorController {
	private final Logger logger = LogManager.getLogger();

	@GetMapping("/login")
    String get(Model model) throws UnknownHostException {
		logger.error("LOGIN");
		return "editor/login";
    }
}
