package ir.piana.dev.sqlcreator.data.sql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mj.rahmati on 12/4/2019.
 */
public class SQLModelManager {
    private Map<String, SelectDef> selectMap;
    private Map<String, UpdateDef> updateMap;
    private Map<String, InsertDef> insertMap;

    public boolean hasSelectDef(String sourceName) {
        return selectMap.containsKey(sourceName);
    }

    public boolean hasUpdateDef(String sourceName) {
        return updateMap.containsKey(sourceName);
    }

    public boolean hasInsertDef(String sourceName) {
        return insertMap.containsKey(sourceName);
    }

    public SelectDef getSelectDef(String sourceName) {
        return selectMap.get(sourceName);
    }

    UpdateDef getUpdateDef(String sourceName) {
        return updateMap.get(sourceName);
    }

    InsertDef getInsertDef(String sourceName) {
        return insertMap.get(sourceName);
    }


    private SQLModelManager(Map<String, SelectDef> selectMap,
                            Map<String, UpdateDef> updateMap,
                            Map<String, InsertDef> insertMap) {
        this.selectMap = selectMap;
        this.updateMap = updateMap;
        this.insertMap = insertMap;
    }


    public static SQLModelManager getNewInstance(InputStream ...sqlInputStreams) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, SelectDef> selectMap = new LinkedHashMap<>();
        Map<String, UpdateDef> updateMap = new LinkedHashMap<>();
        Map<String, InsertDef> insertMap = new LinkedHashMap<>();
        for(InputStream is : sqlInputStreams) {
            String string = IOUtils.toString(is, "UTF-8");
            List<Map> modelList = objectMapper.readValue(string, List.class);
            List<Map> displayList = new ArrayList<>();
            for(Map map : modelList) {
                Object displayName = map.get("name");
                if(displayName != null) {
                    displayList.add(map);
                    continue;
                }
                String queryPattern = (String)map.get("query-pattern");
                SQLType sqlType = SQLType.fromName(queryPattern.substring(0, 6));
                if(sqlType == SQLType.SELECT) {
                    SelectDef selectDef = SelectDefCreator.createSQLDef(map);
                    selectMap.put(selectDef.name, selectDef);
                } else if(sqlType == SQLType.UPDATE) {
                    UpdateDef updateDef = UpdateDefCreator.createSQLDef(map);
                    updateMap.put(updateDef.name, updateDef);
                } else if(sqlType == SQLType.INSERT) {
                    InsertDef insertDef = InsertDefCreator.createSQLDef(map);
                    insertMap.put(insertDef.name, insertDef);
                }
            }
        }
        SQLModelManager sqlModelManager = new SQLModelManager(selectMap, updateMap, insertMap);
        return sqlModelManager;
    }

    public List<SelectColumnDef> getSelectColumns(String sourceName) {
        SelectDef selectDef = selectMap.get(sourceName);
        if(selectDef != null)
            return selectDef.getColumns();
        return null;
    }

    public List<SQLParamDef> getSQLParams(String sourceName) {
        SelectDef selectDef = selectMap.get(sourceName);
        if(selectDef != null) {
            List<SQLParamDef> params = selectDef.params.stream().collect(Collectors.toList());
            for(String dependentSourceName : selectDef.getSourceNames()) {
                SelectSourceDef dependentSourceDef = selectDef.getSourceMap().get(dependentSourceName);
                if(dependentSourceDef.type != null && dependentSourceDef.type.equals("source")) {
                    List<SQLParamDef> dependentParamDefs = getSQLParams(dependentSourceDef.name);
                    for(SQLParamDef dependentParamDef : dependentParamDefs) {
                        if(params.stream().filter(param -> param.key.equals(dependentParamDef.key)).collect(Collectors.toList()).isEmpty()) {
                            params.add(dependentParamDef);
                        }
                    }
                }
            }
            return params;
        }
        UpdateDef updateDef = updateMap.get(sourceName);
        if(updateDef != null) {
            List<SQLParamDef> params = updateDef.params.stream().collect(Collectors.toList());
            for(String dependentSourceName : updateDef.getSourceNames()) {
                UpdateSourceDef dependentSourceDef = updateDef.getSourceMap().get(dependentSourceName);
                if(dependentSourceDef.type != null && dependentSourceDef.type.equals("source")) {
                    List<SQLParamDef> dependentParamDefs = getSQLParams(dependentSourceName);
                    for(SQLParamDef dependentParamDef : dependentParamDefs) {
                        if(params.stream().filter(param -> param.key.equals(dependentParamDef.key)).collect(Collectors.toList()).isEmpty()) {
                            params.add(dependentParamDef);
                        }
                    }
                }
            }
            return params;
        }
        InsertDef insertDef = insertMap.get(sourceName);
        if(insertDef != null)
            return insertDef.params;
        return null;
    }

    public List<String> getAllSelectQueryNames() {
        return selectMap.keySet().stream().collect(Collectors.toList());
    }

    public List<String> getAllUpdateQueryNames() {
        return updateMap.keySet().stream().collect(Collectors.toList());
    }

    public List<String> getAllInsertQueryNames() {
        return insertMap.keySet().stream().collect(Collectors.toList());
    }

//    public static void main(String[] args) throws Exception {
//        SQLModelManager modelManager = SQLModelManager.getNewInstance(
//                SQLModelManager.class.getResourceAsStream("/queries/fund-question-query.json"),
//                SQLModelManager.class.getResourceAsStream("/queries/fund-question-form.json"));
//        SQLQueryManager sqlQueryManager = SQLQueryManager.createSQLQueryManager(modelManager, false);
//    }

    public List<String> getParents(String sourceName) {
        List<String> sources = new ArrayList<>();
        for (String key : selectMap.keySet()) {
            for(SelectSourceDef sourceDef : selectMap.get(key).getSources()) {
                if(sourceDef.name.equals(sourceName)) {
                    sources.add(key);
                    break;
                }
            }
        }
        return sources;
    }

    public SourceFlow getSourceFlow(String sourceName) {
        SourceFlow sourceFlow = new SourceFlow();
        SelectDef selectDef = selectMap.get(sourceName);
        sourceFlow.name = sourceName;
        if(selectDef != null) {
            sourceFlow.subSources = new ArrayList<>();
            for(SelectSourceDef source : selectDef.getSources()) {
//                if(source.type != null && source.type.equals("source")) {
                    sourceFlow.subSources.add(getSourceFlow(source.name));
//                }
            }
        }
        return sourceFlow;
    }

    public static class SourceFlow {
        public String name;
        public List<SourceFlow> subSources;
    }
}
