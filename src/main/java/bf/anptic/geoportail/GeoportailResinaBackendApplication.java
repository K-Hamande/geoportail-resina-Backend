package bf.anptic.geoportail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GeoportailResinaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeoportailResinaBackendApplication.class, args);
	}

}