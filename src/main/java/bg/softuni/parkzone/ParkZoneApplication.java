package bg.softuni.parkzone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ParkZoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkZoneApplication.class, args);
    }

}
