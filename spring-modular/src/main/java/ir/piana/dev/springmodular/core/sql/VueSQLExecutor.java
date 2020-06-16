package ir.piana.dev.springmodular.core.sql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;
import java.util.List;

@Service("vueSqlExecuter")
public class VueSQLExecutor implements SQLExecutor {
    @Autowired
    private EntityManager entitManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public <T> List<T> executeQuery(String query, Propagation propagation) {
        transactionTemplate.setPropagationBehavior(propagation.value());
        List res = transactionTemplate.execute(status -> {
            Query namedQuery = entitManager.createNativeQuery(query);
            List resultList = namedQuery.getResultList();
            return resultList;
        });
        return res;
    }

    public int executeUpdate(String query, Propagation propagation) {
        transactionTemplate.setPropagationBehavior(propagation.value());
        int res = transactionTemplate.execute(status -> {
            Query namedQuery = entitManager.createNativeQuery(query);
            int result = namedQuery.executeUpdate();
            return result;
        });
        return res;
    }
}
