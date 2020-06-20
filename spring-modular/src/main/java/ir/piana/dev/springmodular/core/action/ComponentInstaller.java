package ir.piana.dev.springmodular.core.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.piana.dev.springmodular.core.group.GroupProvider;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

//@Component("componentInstaller")
//@Scope("Singleton")
//@DependsOn({"objectMapper", "groupProviderFromYaml"})
public class ComponentInstaller implements InitializingBean {
//    @Autowired
    private ObjectMapper mapper;

//    @Autowired
    private GroupProvider groupProvider;

//    @Autowired
    private List<VueComponentProvider> componentProviders;

    public ComponentInstaller(ObjectMapper mapper, GroupProvider groupProvider, List<VueComponentProvider> componentProviders) {
        this.mapper = mapper;
        this.groupProvider = groupProvider;
        this.componentProviders = componentProviders;
    }

    private Map<String, Object> springMap;
    private Map<String, Object> vueMap;
    private SpringVueResource springVueResource;
    private Map<String, String> componentMap = new TreeMap<>();
    private Map<String, String> storeMap = new TreeMap<>();
    private List<ComponentInstaller.RouteModel> routeMap = new ArrayList<>();

    private static String appComponent;
    private static String notFoundComponent;
    private static String vLinkComponent;

    static {
        try {
            InputStream inputStream = ComponentInstaller.class.getResourceAsStream("/app.vue.js");
            appComponent = IOUtils.toString(inputStream, "UTF-8");

            inputStream = ComponentInstaller.class.getResourceAsStream("/not-found.vue.js");
            notFoundComponent = IOUtils.toString(inputStream, "UTF-8");

            inputStream = ComponentInstaller.class.getResourceAsStream("/v-link.vue.js");
            vLinkComponent = IOUtils.toString(inputStream, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        InputStream inputStream = ActionListener.class.getResourceAsStream("/piana/cfg/spring-vue.yaml");
        if (inputStream == null)
            throw new RuntimeException("config file required!");
        String error = null;
        Map<String, Object> map = mapper.readValue(inputStream, Map.class);
        map = (Map<String, Object>) map.get("app");
        springMap = (Map<String, Object>) map.get("spring");
        vueMap = (Map<String, Object>) map.get("vue");

        try {
            loadComponents();
            loadRoutesFromResource();
            install();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void loadComponents() {
        for (VueComponentProvider provider : componentProviders) {
            try {
                String componentHtml = provider.getComponentHtml();
                String componentJs = provider.getComponentJs();
                StringBuffer jsAppBuffer = new StringBuffer();
                String appName = provider.getVueApp().appName();
                String jsApp = jsAppBuffer.append("var " + appName + " = ")
                        .append(componentJs).toString()
                        .replaceAll("\\$app\\$", appName)
                        .replaceAll("\\$template\\$",
                                Arrays.stream(componentHtml.split(System.getProperty("line.separator")))
                                        .map(line -> line.trim()).collect(Collectors.joining("")));
                componentMap.put(appName, jsApp);
                String componentStore = provider.getComponentStore();
                storeMap.put(appName, componentStore);
//                    installComponent(((VueComponentProvider)loadable.newInstance()).getComponentJs(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadRoutesFromResource() {
        route(this.getClass().getResourceAsStream("/piana/cfg/route.yaml"));
    }

    private ComponentInstaller route(InputStream inputStream) {
        String error = null;
        try {
            Map<String, Object> map = mapper.readValue(inputStream, Map.class);
            routeMap.addAll(route((Map)map.get("route"), ""));
        } catch (IOException e) {
            error = e.getMessage();
        }
        if(error != null)
            throw new RuntimeException(error);
        return this;
    }

    private List<ComponentInstaller.RouteModel> route(Map<String, Object> map, String parentPath) {
        List<ComponentInstaller.RouteModel> routeMap = new ArrayList<>();
        parentPath = parentPath.equals("//") ? "" : parentPath;
        for (String key : map.keySet()) {
            if (map.get(key) instanceof String) {
                String sign = "";
                String val = (String) map.get(key);
                if(val.startsWith("$")) {
                    if(val.substring(1, val.indexOf("(")).equalsIgnoreCase("redirect-to")) {
                        val = val.substring(val.indexOf("(") + 1, val.indexOf(")"));
                        sign = "@";
                    }
                }
                if(sign.equalsIgnoreCase("@"))
                    routeMap.add(new ComponentInstaller.RouteModel(sign.concat(parentPath).concat(key), val, null, null, null));
                else
                    routeMap.add(new ComponentInstaller.RouteModel(sign.concat(parentPath).concat(key), null, val, null, null));
            } else {
                Map<String, Object> childMap = (Map<String, Object>) map.get(key);
                LinkedHashMap props = null;
                if(childMap.containsKey("props")) {
                    props = (LinkedHashMap) childMap.get("props");
                }
                String component = null;
                String path = null;
                if(childMap.containsKey("component")) {
                    component = (String) childMap.get("component");
                } else {
                    path = parentPath.concat(key).concat("/");
                }

                Map<String, Object> children = (Map<String, Object>) childMap.get("children");
                if(children != null && !children.isEmpty()) {
                    routeMap.add(new ComponentInstaller.RouteModel(parentPath.concat(key), null, component, route(children, ""), props));
                } else {
                    if(component != null)
                        routeMap.add(new ComponentInstaller.RouteModel(parentPath.concat(key), null, component, null, props));
                    else {
                        map.get(key);
                        routeMap.add(new ComponentInstaller.RouteModel(path, null, null, route((Map<String, Object>)map.get(key), path), props));
                    }

                }
            }
        }
        return routeMap;
    }

    private void install() {
        StringBuffer appBuffer = new StringBuffer();
        for(String key : componentMap.keySet()) {
            appBuffer.append(componentMap.get(key)).append("\r\n");
        }
        appBuffer.append("const routes = [");
        appBuffer.append(installRouter(routeMap));
        if(routeMap.size() > 0)
            appBuffer.deleteCharAt(appBuffer.length() - 1);
        appBuffer.append("];");
        appBuffer.append("const router = new VueRouter({hash: false, routes: routes});\r\n");
        appBuffer.append("Vue.mixin({data: function() { return {get groups() {return ")
                .append(groupProvider.getGroupsJsonString())
                .append("; }, get activeParent() { return {\"code\": \"\" }; }}}});")
                .append("\r\n");

        appBuffer.append(notFoundComponent).append("\r\n");
        appBuffer.append("const groups = ").append(groupProvider.getGroupsJsonString()).append(";\r\n");

        appBuffer.append("const store = {");
        appBuffer.append("state: {");
        for(String stateName : storeMap.keySet()) {
            String state = storeMap.get(stateName).replaceAll("\\$state\\$ = ", stateName.concat(": "));
            appBuffer.append(state).append(",");
        }
        appBuffer.append("getState (stateName) { return Object.assign({}, this.state[stateName]); },\r\n");
        appBuffer.append("setState (stateName, obj) { this.state[stateName] = obj;  this.state = Object.assign({}, this.state);}\r\n");
//        appBuffer.deleteCharAt(appBuffer.length() - 1);
        appBuffer.append("}};").append("\r\n");

        appBuffer.append(appComponent).append("\n");
        springVueResource = new VueComponentResource(appBuffer.toString(), groupProvider);
    }

    StringBuffer installRouter(List<ComponentInstaller.RouteModel> models) {
        StringBuffer routerBuffer = new StringBuffer();
        for(ComponentInstaller.RouteModel model : models) {
            if(model.path.startsWith("@"))
                routerBuffer.append("{path:'" + model.path.substring(1) + "', redirect:'").append(model.redirect).append("'},");
            else {
                if(model.childeren == null || model.childeren.isEmpty()) {
                    routerBuffer.append("{path:'" + model.path + "', component:").append(model.component)
                            .append(",");
                    if(model.props != null) {
                        Map<String, Object> pMap = (Map<String, Object>)model.props;
                        routerBuffer.append("props: {");
                        for(String key: pMap.keySet()) {
                            routerBuffer.append(key).append(":");
                            if(pMap.get(key) instanceof String)
                                routerBuffer.append("'" + pMap.get(key) + "'");
                            else
                                routerBuffer.append(pMap.get(key));
                            routerBuffer.append(",");
                        }
                        routerBuffer.deleteCharAt(routerBuffer.length() - 1);
                        routerBuffer.append("},");
                    }
                    routerBuffer.deleteCharAt(routerBuffer.length() - 1);
                    routerBuffer.append("},");
                } else {
                    routerBuffer.append("{path:'" + model.path + "', component:").append(model.component)
                            .append(",");
                    if(model.props != null) {
                        Map<String, Object> pMap = (Map<String, Object>)model.props;
                        routerBuffer.append("props: {");
                        for(String key: pMap.keySet()) {
                            routerBuffer.append(key).append(":");
                            if(pMap.get(key) instanceof String)
                                routerBuffer.append("'" + pMap.get(key) + "'");
                            else
                                routerBuffer.append(pMap.get(key));
                            routerBuffer.append(",");
                        }
                        routerBuffer.deleteCharAt(routerBuffer.length() - 1);
                        routerBuffer.append("},");
                    }
                    routerBuffer.append("children:[");
                    routerBuffer.append(installRouter(model.childeren));

                    routerBuffer.deleteCharAt(routerBuffer.length() - 1);
                    routerBuffer.append("]},");
                }
            }
        }
        return routerBuffer;
    }

    public SpringVueResource getSpringVueResource() {
        return springVueResource;
    }

    private static class VueComponentResource implements SpringVueResource {
        private String vueApp;
        private GroupProvider groupProvider;

        public VueComponentResource (String vueApp, GroupProvider groupProvider) {
            this.vueApp = vueApp;
            this.groupProvider = groupProvider;
        }

        @Override
        public String getVueApp() {
            return vueApp;
        }

        @Override
        public Map<String, Map.Entry<String, String>> getBeanMap() {
            return null;
        }

        @Override
        public void refresh() {
//            SpringVueResource springVueResource = ComponentInstaller.refreshSpringVueResource(this, groupProvider);
//            this.vueApp = springVueResource.getVueApp();
        }
    }

    public static class StateModel {
        private String name;
        private String type;
        private String value;

        public StateModel() {
        }

        public StateModel(String name, String type, String value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class RouteModel {
        private String path;
        private String redirect;
        private String component;
        private List<ComponentInstaller.RouteModel> childeren;
        private Object props;

        public RouteModel() {
        }

        public RouteModel(String path, String redirect, String component, List<ComponentInstaller.RouteModel> childeren, Object props) {
            this.path = path;
            this.redirect = redirect;
            this.component = component;
            this.childeren = childeren;
            this.props = props;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getRedirect() {
            return redirect;
        }

        public void setRedirect(String redirect) {
            this.redirect = redirect;
        }

        public String getComponent() {
            return component;
        }

        public void setComponent(String component) {
            this.component = component;
        }

        public List<ComponentInstaller.RouteModel> getChilderen() {
            return childeren;
        }

        public void setChilderen(List<ComponentInstaller.RouteModel> childeren) {
            this.childeren = childeren;
        }

        public Object getProps() {
            return props;
        }

        public void setProps(Object props) {
            this.props = props;
        }
    }
}
