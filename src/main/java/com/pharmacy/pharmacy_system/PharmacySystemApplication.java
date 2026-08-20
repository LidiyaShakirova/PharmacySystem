package com.pharmacy.pharmacy_system;

import com.pharmacy.pharmacy_system.Service.UserService;
import com.pharmacy.pharmacy_system.Util.SpringFXMLLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication

public class PharmacySystemApplication {
	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(PharmacySystemApplication.class, args);
		UserService userService = context.getBean(UserService.class);
		userService.createDefaultAdmin();

		SpringFXMLLoader.setApplicationContext(context);

		JavaFxApplication.launchApp(args);
	}

}
