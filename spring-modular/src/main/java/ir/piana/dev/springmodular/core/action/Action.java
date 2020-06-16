package ir.piana.dev.springmodular.core.action;

import ir.piana.dev.springmodular.core.sql.SQLExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Action extends VueComponentProvider{
    @Autowired
    protected SQLExecutor sqlExecuter;

    public Function<RequestEntity, ResponseEntity> getField(String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = this.getClass().getField(fieldName);
        return (Function<RequestEntity, ResponseEntity>)field.get(this);
    }

    public Function<HttpServletRequest, ResponseEntity> getFieldF(String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = this.getClass().getField(fieldName);
        return (Function<HttpServletRequest, ResponseEntity>)field.get(this);
    }

    public BiFunction<MultipartFile, RedirectAttributes, ResponseEntity> getFieldFile(String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = this.getClass().getField(fieldName);
        return (BiFunction<MultipartFile, RedirectAttributes, ResponseEntity>)field.get(this);
    }
}
