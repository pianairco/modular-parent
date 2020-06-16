package ir.piana.dev.springmodular.core.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

//@Controller
public class ActionRest {
//    @Autowired
    private ApplicationContext сontext;

//    @Autowired
    private SpringVueResource springVueResource;

    @GetMapping(value = "/vu-app",
            produces = "text/javascript; charset=UTF-8")
    public ResponseEntity getVueApp() throws ParserConfigurationException {
        boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().
                getInputArguments().toString().indexOf("jdwp") >= 0;
        if(isDebug)
            springVueResource.refresh();
        return ResponseEntity.status(200).body(springVueResource.getVueApp());
    }

    @PostMapping(path = "action")
    public ResponseEntity action(RequestEntity requestEntity) {
        List<String> beans = requestEntity.getHeaders().get("action");
        List<String> actions = requestEntity.getHeaders().get("activity");
        if(beans != null && !beans.isEmpty()) {
            Action bean = (Action)сontext.getBean(beans.get(0));
            if(actions != null && !actions.isEmpty()) {
                try {
                    Function<RequestEntity, ResponseEntity> field = bean.getField(actions.get(0));
                    return field.apply(requestEntity);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return notFound.apply(requestEntity);
    }

    @PostMapping(path = "action/servlet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity actionF(HttpServletRequest requestEntity) {
        String beans = requestEntity.getHeader("action");
        String actions = requestEntity.getHeader("activity");
        if(beans != null && !beans.isEmpty()) {
            Action bean = (Action)сontext.getBean(beans);
            if(actions != null && !actions.isEmpty()) {
                try {
                    Function<HttpServletRequest, ResponseEntity> field = bean.getFieldF(actions);
                    return field.apply(requestEntity);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return notFoundF.apply(requestEntity);
    }

//    @PostMapping(value = "/action/file",
//            consumes = {"multipart/form-data"})
//    public ResponseEntity fileUploadWithString(@RequestPart(value = "file", required = true) MultipartFile file,
//                                               @RequestHeader("action") List<String> beans,
//                                               @RequestHeader("activity") List<String> activities,
//                                               RedirectAttributes redirectAttributes) {
////        List<String> beans = requestEntity.getHeaders().get("action");
////        List<String> actions = requestEntity.getHeaders().get("activity");
//        if(beans != null && !beans.isEmpty()) {
//            Action bean = (Action)сontext.getBean(beans.get(0));
//            if(activities != null && !activities.isEmpty()) {
//                try {
//                    BiFunction<MultipartFile, RedirectAttributes, ResponseEntity> field = bean.getFieldFile(activities.get(0));
//                    return field.apply(file, redirectAttributes);
//                } catch (NoSuchFieldException e) {
//                    e.printStackTrace();
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        return notFoundSupplier.get();
////        return new ResponseEntity<>("jsdbj", HttpStatus.OK);
//    }

    Function<RequestEntity, ResponseEntity> notFound = (r) -> {
        return ResponseEntity.status(404).body("Not Found");
    };

    Function<HttpServletRequest, ResponseEntity> notFoundF = (r) -> {
        return ResponseEntity.status(404).body("Not Found");
    };

    Supplier<ResponseEntity> notFoundSupplier = () -> {
        return ResponseEntity.status(404).body("Not Found");
    };
}
