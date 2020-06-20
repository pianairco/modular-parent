package ir.piana.dev.springmodular.core.group;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.piana.dev.springmodular.core.action.Action;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import javax.annotation.PostConstruct;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupFromYamlService implements GroupProvider {
    private ObjectMapper yamlObjectMapper;
    private ObjectMapper objectMapper;
    private boolean debug;

    private String lPatternString = "l\\(.*\\)";
    private String tPatternString = "t\\(.*\\)";
    private String rPatternString = "r\\(.*\\)";
    private String iPatternString = "i\\(.*\\)";
    private String cPatternString = "c\\(.*\\)";

    Pattern lPattern;
    Pattern tPattern;
    Pattern rPattern;
    Pattern cPattern;
    Pattern iPattern;

    private List<GroupModel> groupCFGList = new ArrayList<>();
    private Map<String, GroupModel> groupCFGMap = new LinkedHashMap<>();


    public GroupFromYamlService(ObjectMapper objectMapper, ObjectMapper yamlObjectMapper, boolean debug) {
        this.objectMapper = objectMapper;
        this.yamlObjectMapper = yamlObjectMapper;
        this.debug = debug;
    }

    @PostConstruct
    public void init() {
        lPattern = Pattern.compile(lPatternString);
        tPattern = Pattern.compile(tPatternString);
        rPattern = Pattern.compile(rPatternString);
        iPattern = Pattern.compile(iPatternString);
        cPattern = Pattern.compile(cPatternString);
        reloadGroups();
    }

    public void reloadGroups() {
        InputStream inputStream = ActionListener.class.getResourceAsStream("/piana/cfg/group.yaml");
        if (inputStream == null)
            throw new RuntimeException("config file required!");
        String error = null;
        try {
            groupCFGList.clear();
            groupCFGMap.clear();
            Map<String, Map> map = yamlObjectMapper.readValue(inputStream, Map.class);
            map = (Map<String, Map>) map.get("groups");
            if(map != null)
                map.forEach((k, v) -> {groupCFGList.add(parseGroup(v, ""));});
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private GroupModel parseGroup (Map<String, Object> kMap, String parentCode) {
        GroupModel item = new GroupModel();
        item.setParentCode(parentCode);
        String info = (String)kMap.get("info");
        Matcher lMatcher = lPattern.matcher(info);
        if (lMatcher.find()) {
            item.setLink(lMatcher.group().substring(2, lMatcher.group().length() - 1));
            info = info.substring(0, info.length() - lMatcher.group().length());
        } else {
            item.setLink("");
        }
        Matcher tMatcher = tPattern.matcher(info);
        if (tMatcher.find()) {
            item.setTitle(tMatcher.group().substring(2, tMatcher.group().length() - 1));
            info = info.substring(0, info.length() - tMatcher.group().length());
        }
        Matcher rMatcher = rPattern.matcher(info);
        if (rMatcher.find()) {
            item.setRole(rMatcher.group().substring(2, rMatcher.group().length() - 1));
            info = info.substring(0, info.length() - rMatcher.group().length());
        }
        Matcher iMatcher = iPattern.matcher(info);
        if (iMatcher.find()) {
            item.setIcon(iMatcher.group().substring(2, iMatcher.group().length() - 1));
            info = info.substring(0, info.length() - iMatcher.group().length());
        }
        Matcher cMatcher = cPattern.matcher(info);
        if (cMatcher.find()) {
            item.setCode(parentCode.concat(cMatcher.group().substring(2, cMatcher.group().length() - 1)));
            groupCFGMap.put(item.getCode(), item);
        }

        kMap.remove("info");
        if(kMap.size() > 1) {
            for(String k : kMap.keySet()) {
                Map<String, Object> m = (Map<String, Object>)kMap.get(k);
                GroupModel g = parseGroup(m, item.getCode());
                item.getGroups().add(g);
            }
        }
        return item;
    }

    public Function<RequestEntity, ResponseEntity> groups = (r) -> {
        return ResponseEntity.status(200).body(getGroups());
    };

    public List<GroupModel> getGroups() {
        if(debug)
            reloadGroups();
        return groupCFGList;
    }

    @Override
    public String getGroupsJsonString() throws RuntimeException {
        try {
            if(debug)
                reloadGroups();
            return objectMapper.writeValueAsString(groupCFGList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getGroupsYamlString() throws RuntimeException {
        try {
            if(debug)
                reloadGroups();
            return yamlObjectMapper.writeValueAsString(groupCFGList);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public GroupModel getGroupByCode(String code) {
        if(debug)
            reloadGroups();
        return groupCFGMap.get(code);
    }

//    public static class GroupCFG {
//        private String title;
//        private String icon;
//        private String role;
//        private String code;
//        private boolean open;
//        private List<GroupCFG> groups = new ArrayList<>();
//
//        public GroupCFG() {
//        }
//
//        public String getTitle() {
//            return title;
//        }
//
//        public String getIcon() {
//            return icon;
//        }
//
//        public String getRole() {
//            return role;
//        }
//
//        public String getCode() {
//            return code;
//        }
//
//        public List<GroupCFG> getGroups() {
//            return groups;
//        }
//
//        public boolean isOpen() {
//            return open;
//        }
//    }
}
