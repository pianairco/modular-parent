package ir.piana.dev.springmodular;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication(scanBasePackages = {
		"ir.piana.dev.springmodular"
//		"ir.piana.dev.springmodular.cfg",
//		"ir.piana.dev.springmodular.core",
//		"ir.piana.dev.springmodular.component"
})
public class SpringVueApplicationTests {
	public static void main(String[] args) {
		SpringApplication.run(SpringVueApplicationTests.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void doSomethingAfterStartup() {
//		try {
//			componentProvider.getComponentHtml();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		System.out.println("hello world, I have just started up");
	}
}
