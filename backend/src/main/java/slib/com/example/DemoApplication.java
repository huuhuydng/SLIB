package slib.com.example;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

    public static void main(String[] args) {
        loadEnvFile("..");
        loadEnvFile(".");

        SpringApplication.run(DemoApplication.class, args);
    }

    private static void loadEnvFile(String directory) {
        Dotenv.configure()
                .directory(directory)
                .filename(".env")
                .ignoreIfMissing()
                .systemProperties()
                .load();
    }
}
