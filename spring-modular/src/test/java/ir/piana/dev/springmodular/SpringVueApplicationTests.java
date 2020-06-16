package ir.piana.dev.springmodular;

import ir.piana.dev.springmodular.component.first.ComponentProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;

import java.io.IOException;

//@SpringBootTest()
//@SpringBootConfiguration
@SpringBootApplication(scanBasePackages = "ir.piana.dev.springmodular.component")
@PropertySource("classpath:application.yaml")
public class SpringVueApplicationTests {
	@Autowired
	ComponentProvider componentProvider;

	public static void main(String[] args) {
		SpringApplication.run(SpringVueApplicationTests.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void doSomethingAfterStartup() {
		try {
			componentProvider.getComponentHtml();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("hello world, I have just started up");
	}
}
