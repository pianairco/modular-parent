package ir.piana.dev.springmodular.component.first;

import ir.piana.dev.springmodular.core.action.Action;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component("FirstAction")
public class FirstAction extends Action {
    public Function<RequestEntity, ResponseEntity> x = (r) -> {
        List<Object> objects = sqlExecuter.executeQuery("select * from users", Propagation.REQUIRED);
        Map body = (Map) r.getBody();
        String firstName = (String)body.get("firstName");
        String lastName = (String)body.get("lastName");
        return ResponseEntity.ok("Hello ".concat(firstName).concat(" ").concat(lastName));
    };
}
