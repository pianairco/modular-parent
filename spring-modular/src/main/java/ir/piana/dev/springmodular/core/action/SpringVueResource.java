package ir.piana.dev.springmodular.core.action;

import java.util.Map;

public interface SpringVueResource {
    String getVueApp();
    Map<String, Map.Entry<String, String>> getBeanMap();
    void refresh();
}
