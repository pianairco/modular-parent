package ir.piana.dev.sqlcreator.data.sql;

import java.util.Map;

/**
 * Created by mj.rahmati on 1/5/2020.
 */
public class DefaultParameterProvider implements ParameterProvider {
    private Map<String, Object> paramMap;
    private boolean preferredParamMap;

    public DefaultParameterProvider(Map<String, Object> paramMap) {
        this(paramMap, false);
    }

    public DefaultParameterProvider(Map<String, Object> paramMap, boolean preferredParamMap) {
        this.paramMap = paramMap;
        this.preferredParamMap = preferredParamMap;
    }

    @Override
    public <T> T get(String paramName) {
        if(preferredParamMap)
            return getPreferredParamMap(paramName);
        else
            return getPreferredRequest(paramName);
    }

    public <T> T getPreferredParamMap(String paramName) {
        Object obj = null;
        if(paramMap != null)
            obj = paramMap.get(paramName);
        return (T)obj;
    }

    public <T> T getPreferredRequest(String paramName) {
        Object obj = null;
        if(obj == null && paramMap != null)
            obj = paramMap.get(paramName);
        return (T)obj;
    }

    @Override
    public <T> T getValue(String key, String type) {
        if(preferredParamMap)
            return getValuePreferredParamMap(key, type);
        else
            return getValuePreferredRequest(key, type);
    }

    public <T> T getValuePreferredParamMap(String key, String type) {
        Object obj = null;
        if(paramMap != null)
            obj = paramMap.get(key);
        if(obj == null) {
            if(type.equals("string"))
                return (T) "";
            else
                return null;
        }
        return (T) obj;
    }

    public <T> T getValuePreferredRequest(String key, String type) {
        Object obj = null;
        if(obj == null && paramMap != null)
            obj = paramMap.get(key);
        if(obj == null) {
            if(type.equals("string"))
                return (T) "";
            else
                return null;
        }
        return (T) obj;
    }
}
