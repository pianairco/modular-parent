package ir.piana.dev.springmodular.cfg;

import ir.piana.dev.springmodular.core.action.ActionConfig;
import ir.piana.dev.springmodular.core.sql.VueSQLExecutor;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.context.annotation.*;

@Configuration
@PropertySource("classpath:application.yaml")
@Import(VueSQLExecutor.class)
public class CFGActionConfig extends ActionConfig {

}
