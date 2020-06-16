package ir.piana.dev.springmodular.core.sql;

import org.springframework.transaction.annotation.Propagation;

import java.util.List;

public interface SQLExecutor {
    <T> List<T> executeQuery(String query, Propagation propagation);
    int executeUpdate(String query, Propagation propagation);
}
