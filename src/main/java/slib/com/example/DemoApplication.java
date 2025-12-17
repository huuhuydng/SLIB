package slib.com.example; // Quan trọng: Phải đúng tên package này

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import slib.com.example.service.EmailSenderService;

@SpringBootApplication

public class DemoApplication {
    @Autowired
    private EmailSenderService senderService;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sendMail() {
        senderService.sendEmail(
                "hbui110604@gmail.com",
                "Test Subject",
                "This is the body of the test email."
        );
    }
}