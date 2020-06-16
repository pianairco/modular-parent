package ir.piana.dev.springmodular.core.action;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public abstract class VueComponentLoadable {
    public abstract InputStream getResource();

    final String getComponentString() {
        String s = null;
        try {
            s = IOUtils.toString(getResource(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }
}
