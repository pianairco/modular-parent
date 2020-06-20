package ir.piana.dev.springmodular.component.first;

import ir.piana.dev.springmodular.core.action.Action;
import ir.piana.dev.springmodular.core.action.VueApp;
import ir.piana.dev.springmodular.core.sql.SQLExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service("FirstAction")
@VueApp(appName = "firstComponent")
public class FirstAction extends Action {
    @Autowired
    protected SQLExecutor sqlExecutor;

    public Function<RequestEntity, ResponseEntity> x = (r) -> {
        List<Object> objects = sqlExecutor.executeQuery("select * from users", Propagation.REQUIRED);
        Map body = (Map) r.getBody();
        String firstName = (String)body.get("firstName");
        String lastName = (String)body.get("lastName");
        return ResponseEntity.ok("Hello ".concat(firstName).concat(" ").concat(lastName));
    };
}
