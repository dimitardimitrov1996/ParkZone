package bg.softuni.parkzone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.ControllerAdvice;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableCaching
@ControllerAdvice
public class ParkZoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkZoneApplication.class, args);
    }

}
