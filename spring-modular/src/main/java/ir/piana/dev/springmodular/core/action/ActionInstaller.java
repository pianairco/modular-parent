package ir.piana.dev.springmodular.core.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ir.piana.dev.springmodular.core.group.GroupProvider;
import ir.piana.dev.springmodular.core.reflection.ClassGenerator;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

public class ActionInstaller {
    private static DocumentBuilder dBuilder;
    private static ActionInstaller actionInstaller = null;
    private GroupProvider groupProvider;
    private List<String> components = new ArrayList<>();
    private String routePath;
    private List<RouteModel> routeMap = new ArrayList<>();
    private String beanBaseName;
    private String beanPackage;
    private List<String> loadFrom;
    StringBuffer buffer = new StringBuffer();
    Set<String> stateNames = new HashSet<>();
    Map<String, StateModel> stateNameMap = new LinkedHashMap<>();
    Map<String, Map.Entry<String, String>> beanMap = new LinkedHashMap<>();

    private static String appComponent;
    private static String notFoundComponent;
    private static String vLinkComponent;

    static {
        try {
            dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            InputStream inputStream = ActionInstaller.class.getResourceAsStream("/app.vue.jsp");
            String theString = IOUtils.toString(inputStream, "UTF-8");
            appComponent = theString.substring(theString.indexOf("<script>") + 8, theString.indexOf("</script>"));

            inputStream = ActionInstaller.class.getResourceAsStream("/not-found.vue.jsp");
            theString = IOUtils.toString(inputStream, "UTF-8");
            notFoundComponent = theString.substring(theString.indexOf("<script>") + 8, theString.indexOf("</script>"));

            inputStream = ActionInstaller.class.getResourceAsStream("/v-link.vue.jsp");
            theString = IOUtils.toString(inputStream, "UTF-8");
            vLinkComponent = theString.substring(theString.indexOf("<script>") + 8, theString.indexOf("</script>"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
    private static final String NUMBER = "0123456789";
    private static final String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
    private static final SecureRandom random = new SecureRandom();
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private static long counter = 1;
    private static final String baseBeanName = "springVue";

//    private ActionInstaller(DocumentBuilder dBuilder, Map<String, Object> map) {
//        Map<String, String> springMap = (Map<String, String>) map.get("spring");
//        Map<String, String> vueMap = (Map<String, String>) map.get("vue");
//        beanBaseName = springMap.get("bean-base-name");
//        beanPackage = springMap.get("bean-package");
//        String loadFrom = vueMap.get("load-from");
//        if(loadFrom.equals("resource")) {
//
//        }
//        this.dBuilder = dBuilder;
//        this.prop = prop;
//        this.buffer.append(vLinkComponent).append("\n").append(notFoundComponent).append("\n");
//    }

    private static String generateRandomString() {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 8; i++) {
            // 0-62 (exclusive), random returns 0-61
            int rndCharAt = random.nextInt(DATA_FOR_RANDOM_STRING.length());
            char rndChar = DATA_FOR_RANDOM_STRING.charAt(rndCharAt);
            sb.append(rndChar);
        }
        return sb.toString();
    }

    private List<File> getResourceFiles(String folderName) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = this.getClass().getResource(folderName);
        if(url != null) {
            String path = url.getPath();
            File[] files = new File(path).listFiles();
            return Arrays.asList(files);
        } else {
            return new ArrayList<>();
        }
    }

    public static synchronized SpringVueResource getSpringVueResource(GroupProvider groupProvider) {
        InputStream inputStream = ActionListener.class.getResourceAsStream("/piana/cfg/spring-vue.yaml");
        if (inputStream == null)
            throw new RuntimeException("config file required!");
        String error = null;
        try {
            Map<String, Object> map = mapper.readValue(inputStream, Map.class);
            map = (Map<String, Object>) map.get("app");
            ActionInstaller actionInstaller = new ActionInstaller();
            actionInstaller.setGroupProvider(groupProvider);
            actionInstaller.setSpringProperties((Map<String, Object>) map.get("spring"));
            actionInstaller.setVueProperties((Map<String, Object>) map.get("vue"));
            actionInstaller.loadComponents();
            actionInstaller.loadRoutes();
            return actionInstaller.install();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    static synchronized SpringVueResource refreshSpringVueResource(SpringVueResource springVueResource, GroupProvider groupProvider) {
        InputStream inputStream = ActionListener.class.getResourceAsStream("/piana/cfg/spring-vue.yaml");
        if (inputStream == null)
            throw new RuntimeException("config file required!");
        String error = null;
        try {
            Map<String, Object> map = mapper.readValue(inputStream, Map.class);
            map = (Map<String, Object>) map.get("app");
            ActionInstaller actionInstaller = new ActionInstaller();
            actionInstaller.setGroupProvider(groupProvider);
            actionInstaller.setSpringProperties((Map<String, Object>) map.get("spring"));
            actionInstaller.setVueProperties((Map<String, Object>) map.get("vue"));
            actionInstaller.loadComponents();
            actionInstaller.loadRoutes();
            return actionInstaller.refresh(springVueResource, groupProvider);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    static synchronized ActionInstaller getInstance() {
        if(actionInstaller == null)
            throw new RuntimeException("actionInstaller not initialized!");
        return actionInstaller;
    }

    private void setGroupProvider(GroupProvider groupProvider) {
        this.groupProvider = groupProvider;
    }

    private void setSpringProperties(Map<String, Object> springMap) {
        beanBaseName = (String) springMap.get("bean-base-name");
        beanPackage = (String) springMap.get("bean-package");
    }

    private void setVueProperties(Map<String, Object> vueMap) {
        loadFrom = Arrays.asList(((String) vueMap.get("load-from")).split(","))
                .stream().map(s -> s.trim()).collect(Collectors.toList());
//        routeMap.putAll(route((Map)vueMap.get("route"), ""));
    }

    private void loadComponents() {
        if (loadFrom.contains("resource")) {
            loadComponentsFromResource();
        }
    }

    private void loadComponentsFromResource() {
        List<File> componentFiles = getResourceFiles("/piana/component");
        List<File> pageFiles = getResourceFiles("/piana/page");
        for(File f : componentFiles)
            component("/piana/component/".concat(f.getName()));
        for(File f : pageFiles)
            component("/piana/page/".concat(f.getName()));
    }

    private void loadRoutes() {
//        if (loadFrom.contains("resource")) {
        loadRoutesFromResource();
//        }
    }

    private void loadRoutesFromResource() {
        route(this.getClass().getResourceAsStream("/piana/cfg/route.yaml"));
    }

    public ActionInstaller component(String resourcePath) {
        components.add(resourcePath);
        return this;
    }

    private ActionInstaller installComponent(InputStream inputStream) {
        String error = null;
//        String jsApp = null;
        try {
            String theString = IOUtils.toString(inputStream, "UTF-8");
            this.installComponent(theString, null);
        } catch (IOException e) {
            error = e.getMessage();
        }
        if(error != null)
            throw new RuntimeException(error);
//        buffer.append(jsApp).append("\n");
        return this;
    }

    private ActionInstaller installComponent(String theString, SpringVueResource springVueResource) {
        String error = null;
        String jsApp = null;
        try {
            String appString = theString.substring(theString.indexOf("<app "), theString.indexOf("</app>") + 6);
            Document doc = dBuilder.parse(new InputSource(new StringReader(appString)));
            NodeList nList = doc.getElementsByTagName("app");
            Node item = nList.item(0);
            String appName = null;
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) item;
                appName = eElement.getAttribute("name");
            }

            String beanName = "";
            if(springVueResource != null) {
                final String appName2 = appName;
                List<Map.Entry<String, String>> collect = springVueResource.getBeanMap().keySet().stream()
                        .filter(key -> springVueResource.getBeanMap().get(key).getValue().equals(appName2))
                        .map(key -> springVueResource.getBeanMap().get(key))
                        .collect(Collectors.toList());
                if(collect != null && !collect.isEmpty())
                    beanName = collect.get(0).getKey();
            } else {
                beanName = beanBaseName.concat(generateRandomString()).concat(String.valueOf(counter++));
            }

            String templateString = theString.substring(theString.indexOf("<html-template>") + 15, theString.indexOf("</html-template>"))
                    .replaceAll("\\$bean\\$", beanName);

            String vueScriptStringContent = theString.substring(theString.indexOf("<vue-script>") + 12, theString.indexOf("</vue-script>"));
            String firstScript = vueScriptStringContent.substring(vueScriptStringContent.indexOf("<script"), vueScriptStringContent.indexOf(">") + 1);
            String script = vueScriptStringContent.substring(vueScriptStringContent.indexOf("<script") + firstScript.length(), vueScriptStringContent.indexOf("</script>"));

            Document vueScriptDoc = dBuilder.parse(new InputSource(new StringReader(firstScript.concat("</script>"))));
            NodeList entries = vueScriptDoc.getElementsByTagName("script");
            for (int i = 0; i < entries.getLength(); i++) {
                Element element = (Element) entries.item(i);
                if(element.getAttribute("for").equalsIgnoreCase("component")) {
                    String scriptString = script;
                    StringBuffer jsAppBuffer = new StringBuffer();
                    jsApp = jsAppBuffer.append(scriptString).toString().replaceAll("\\$app\\$", appName)
                            .replaceAll("\\$bean\\$", beanName)
                            .replace("$template$",
                                    Arrays.stream(templateString.split(System.getProperty("line.separator")))
                                            .map(line -> line.trim()).collect(Collectors.joining("")));
                } else if(element.getAttribute("for").equalsIgnoreCase("state")) {
                    Document stateDoc = dBuilder.parse(new InputSource(new StringReader("<script>" + script + "</script>")));
                    NodeList stateEntries = stateDoc.getElementsByTagName("state");
                    for (int j = 0; j < stateEntries.getLength(); j++) {
                        Element stateElement = (Element) stateEntries.item(j);
                        String name = stateElement.getAttribute("name");
                        String type = stateElement.getAttribute("type");
                        String value = stateElement.getAttribute("value");
                        if(name != null && !name.isEmpty() && !stateNames.contains(name)) {
                            stateNames.add(name);
                            stateNameMap.put(name, new StateModel(name, type, value));
                        }
                    }
                }
            }

            String secondScriptContent = vueScriptStringContent.substring(vueScriptStringContent.indexOf("</script>") + 9);
            String secondScript = secondScriptContent.substring(
                    vueScriptStringContent.indexOf("<script"), vueScriptStringContent.indexOf(">") + 1);
            script = secondScriptContent.substring(secondScriptContent.indexOf("<script") + secondScript.length(),
                    secondScriptContent.indexOf("</script>"));
            vueScriptDoc = dBuilder.parse(new InputSource(new StringReader(secondScript.concat("</script>"))));
            entries = vueScriptDoc.getElementsByTagName("script");
            for (int i = 0; i < entries.getLength(); i++) {
                Element element = (Element) entries.item(i);
                if(element.getAttribute("for").equalsIgnoreCase("component")) {
                    String scriptString = script;
                    StringBuffer jsAppBuffer = new StringBuffer();
                    jsApp = jsAppBuffer.append(scriptString).toString().replaceAll("\\$app\\$", appName)
                            .replaceAll("\\$bean\\$", beanName)
                            .replace("$template$",
                                    Arrays.stream(templateString.split(System.getProperty("line.separator")))
                                            .map(line -> line.trim()).collect(Collectors.joining("")));
                } else if(element.getAttribute("for").equalsIgnoreCase("state")) {
                    Document stateDoc = dBuilder.parse(new InputSource(new StringReader("<script>" + script + "</script>")));
                    NodeList stateEntries = stateDoc.getElementsByTagName("state");
                    for (int j = 0; j < stateEntries.getLength(); j++) {
                        Element stateElement = (Element) stateEntries.item(j);
                        String name = stateElement.getAttribute("name");
                        String type = stateElement.getAttribute("type");
                        String value = stateElement.getAttribute("value");
                        if(name != null && !name.isEmpty() && !stateNames.contains(name)) {
                            stateNames.add(name);
                            stateNameMap.put(name, new StateModel(name, type, value));
                        }
                    }
                }
            }

            if(springVueResource == null) {
                int startOfBeanIfExist = theString.indexOf("</vue-script>") + 13;

                if (theString.length() > startOfBeanIfExist) {
                    String javaString = theString.substring(startOfBeanIfExist);
                    if (javaString == null || javaString.isEmpty() || !javaString.contains("<bean>")) {
                        buffer.append(jsApp).append("\n");
                        return this;
                    }
                    javaString = javaString.replaceAll("<%@ page import=\"", "import ");
                    javaString = javaString.replaceAll("\" %>", ";");
                    javaString = javaString.replaceAll("<%", "");
                    javaString = javaString.replaceAll("%>", "");

                    String beanString = javaString.substring(0, javaString.indexOf("<import>")).concat("</bean>");
                    String importString = javaString.substring(javaString.indexOf("<import>") + 8, javaString.indexOf("</import>"));
                    String classString = javaString.substring(javaString.indexOf("<action>") + 8, javaString.indexOf("</action>"));

                    StringBuffer classSourceBuffer = new StringBuffer();

                    String aPackage = beanPackage;
                    String aClassName = beanName.substring(0, 1).toUpperCase() + beanName.substring(1);

                    classSourceBuffer.append("package ".concat(aPackage).concat(";\n"));
                    classSourceBuffer.append(Arrays.stream(importString.split(System.getProperty("line.separator")))
                            .map(line -> line.trim()).collect(Collectors.joining("\n")));
                    classSourceBuffer.append("import org.springframework.stereotype.Component;\n");
                    classSourceBuffer.append("\n");

                    classSourceBuffer.append("@Component(\"").append(beanName).append("\")\n");
                    classSourceBuffer.append(Arrays.stream(classString.split(System.getProperty("line.separator"))).map(line -> line.trim())
                            .filter(line -> !line.isEmpty()).collect(Collectors.joining("\n")));
                    String classSource = classSourceBuffer.toString().replace("class $VUE$", "public class ".concat(aClassName));
                    Class aClass = ClassGenerator.registerClass(aPackage.concat(".").concat(aClassName).replaceAll("\\.", "/"), classSource.replace("$bean$", beanName));
                    beanMap.put(aPackage.concat(".").concat(aClassName), new AbstractMap.SimpleEntry<>(beanName, appName));
                }
            }
        } catch (IOException e) {
            error = e.getMessage();
        } catch (IllegalAccessException e) {
            error = e.getMessage();
        } catch (SAXException e) {
            error = e.getMessage();
        } catch (NoSuchFieldException e) {
            error = e.getMessage();
        }
        if(error != null)
            throw new RuntimeException(error);
        buffer.append(jsApp).append("\n");
        return this;
    }

    private ActionInstaller refreshComponent(InputStream inputStream, SpringVueResource springVueResource) {
        String error = null;
//        String jsApp = null;
        try {
            String theString = IOUtils.toString(inputStream, "UTF-8");
            installComponent(theString, springVueResource);
//            refreshComponent(theString, springVueResource);
        } catch (IOException e) {
            error = e.getMessage();
        }
        if(error != null)
            throw new RuntimeException(error);
//        buffer.append(jsApp).append("\n");
        return this;
    }

//    private ActionInstaller refreshComponent(String theString, SpringVueResource springVueResource) {
//        String error = null;
//        String jsApp = null;
//        try {
//            String appString = theString.substring(theString.indexOf("<app "), theString.indexOf("</app>") + 6);
//            Document doc = dBuilder.parse(new InputSource(new StringReader(appString)));
//            NodeList nList = doc.getElementsByTagName("app");
//            Node item = nList.item(0);
//            String appName = null;
//            if (item.getNodeType() == Node.ELEMENT_NODE) {
//                Element eElement = (Element) item;
//                appName = eElement.getAttribute("name");
//            }
//
//            final String appName2 = appName;
//            List<Map.Entry<String, String>> collect = springVueResource.getBeanMap().keySet().stream()
//                    .filter(key -> springVueResource.getBeanMap().get(key).getValue().equals(appName2))
//                    .map(key -> springVueResource.getBeanMap().get(key))
//                    .collect(Collectors.toList());
//
//            String beanName = "";
//            if(collect != null && !collect.isEmpty())
//                beanName = collect.get(0).getKey();
//
//            String templateString = theString.substring(theString.indexOf("<html-template>") + 15, theString.indexOf("</html-template>"))
//                    .replaceAll("\\$bean\\$", beanName);
//            String scriptString = theString.substring(theString.indexOf("<script>") + 8, theString.indexOf("</script>"));
//
//
//
//
//            StringBuffer jsAppBuffer = new StringBuffer();
//            jsApp = jsAppBuffer.append(scriptString).toString().replaceAll("\\$app\\$", appName)
//                    .replaceAll("\\$bean\\$", beanName)
//                    .replace("$template$",
//                            Arrays.stream(templateString.split(System.getProperty("line.separator")))
//                                    .map(line -> line.trim()).collect(Collectors.joining("")));
//        } catch (IOException e) {
//            error = e.getMessage();
//        } catch (SAXException e) {
//            error = e.getMessage();
//        }
//        if(error != null)
//            throw new RuntimeException(error);
//        buffer.append(jsApp).append("\n");
//        return this;
//    }

    public List<RouteModel> route(Map<String, Object> map, String parentPath) {
        List<RouteModel> routeMap = new ArrayList<>();
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
                    routeMap.add(new RouteModel(sign.concat(parentPath).concat(key), val, null, null, null));
                else
                    routeMap.add(new RouteModel(sign.concat(parentPath).concat(key), null, val, null, null));
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
                    routeMap.add(new RouteModel(parentPath.concat(key), null, component, route(children, ""), props));
                } else {
                    if(component != null)
                        routeMap.add(new RouteModel(parentPath.concat(key), null, component, null, props));
                    else {
                        map.get(key);
                        routeMap.add(new RouteModel(path, null, null, route((Map<String, Object>)map.get(key), path), props));
                    }

                }
            }
        }
        return routeMap;
    }

    private ActionInstaller route(InputStream inputStream) {
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

    public ActionInstaller route(String resourcePath) {
        routePath = resourcePath;
        return this;
    }

    StringBuffer installRouter(List<RouteModel> models) {
        StringBuffer routerBuffer = new StringBuffer();
        for(RouteModel model : models) {
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

    public SpringVueResource install() throws RuntimeException {
        if(loadFrom.contains("interface")) {
            Reflections reflections = new Reflections("ir.piana");
            Set<Class<? extends VueComponentLoadable>> classes = reflections.getSubTypesOf(VueComponentLoadable.class);
            for (Class loadable : classes) {
                try {
                    installComponent(((VueComponentLoadable)loadable.newInstance()).getComponentString(), null);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        for(String resourcePath : components) {
            installComponent(this.getClass().getResourceAsStream(resourcePath));
        }

        StringBuffer routerBuffer = new StringBuffer();
//        route(this.getClass().getResourceAsStream(routePath));
        routerBuffer.append("const routes = [");
        routerBuffer.append(installRouter(routeMap));
        if(routeMap.size() > 0)
            routerBuffer.deleteCharAt(routerBuffer.length() - 1);
        routerBuffer.append("];");
        buffer.append(routerBuffer).append("\n");
//        buffer.append("const router = new VueRouter({mode: 'history', hash: false, routes: routes});");
        buffer.append("const router = new VueRouter({hash: false, routes: routes});");
        buffer.append(notFoundComponent).append("\n");
        buffer.append("const groups = ").append(groupProvider.getGroupsJsonString()).append(";");
//        buffer.append(notFoundComponent).append("\n");
//        buffer.append(vLinkComponent).append("\n");
        buffer.append("const store = {");
        buffer.append("state: {");
        for(String stateName : stateNames) {
            StateModel stateModel = stateNameMap.get(stateName);
            buffer.append(stateName).append(":");
            if (stateModel.getValue() != null && !stateModel.getValue().isEmpty()) {
                if (stateModel.getValue().equalsIgnoreCase("null")) {
                    buffer.append("null,");
                } else if (stateModel.getType().equalsIgnoreCase("string")) {
                    buffer.append("'" + stateModel.getValue() + "',");
                } else {
                    buffer.append(stateModel.getValue() + ",");
                }
            } else if (stateModel.getType() != null && !stateModel.getType().isEmpty()) {
                buffer.append(stateModel.getType() + ",");
            } else {
                buffer.append("Object,");
            }
        }
        buffer.deleteCharAt(buffer.length() - 1);
        buffer.append("}};");

        buffer.append(appComponent).append("\n");
        return new DefaultSpringVueResource(buffer.toString(), beanMap, groupProvider);
    }

    public SpringVueResource refresh(SpringVueResource springVueResource, GroupProvider groupProvider) {
        if(loadFrom.contains("interface")) {
            Reflections reflections = new Reflections("ir.piana");
            Set<Class<? extends VueComponentLoadable>> classes = reflections.getSubTypesOf(VueComponentLoadable.class);
            for (Class loadable : classes) {
                try {
                    installComponent(((VueComponentLoadable)loadable.newInstance()).getComponentString(), springVueResource);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        for(String resourcePath : components) {
//            try {
//                URL resURL = this.getClass().getResource(resourcePath);
//                URLConnection resConn = resURL.openConnection();
//                resConn.setUseCaches(false);
//                refreshComponent(resConn.getInputStream(), springVueResource);
            refreshComponent(this.getClass().getResourceAsStream(resourcePath), springVueResource);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        StringBuffer routerBuffer = new StringBuffer();
//        route(this.getClass().getResourceAsStream(routePath));
        routerBuffer.append("const routes = [");
        routerBuffer.append(installRouter(routeMap));
        if(routeMap.size() > 0)
            routerBuffer.deleteCharAt(routerBuffer.length() - 1);
        routerBuffer.append("];");
        buffer.append(routerBuffer).append("\n");
//        buffer.append("const router = new VueRouter({routes});");
        buffer.append("const router = new VueRouter({hash: false, routes: routes});");

        buffer.append("Vue.mixin({data: function() { return {get groups() {return ")
                .append(groupProvider.getGroupsJsonString()).append("; }, get activeParent() { return {\"code\": \"\" }; }}}});").append("\n");

//        buffer.append("const groups = ").append(groupProvider.getGroupsJsonString()).append(";");
        buffer.append(notFoundComponent).append("\n");
//        buffer.append(vLinkComponent).append("\n");

        buffer.append("const store = {");
        buffer.append("state: {");
        for(String stateName : stateNames) {
            StateModel stateModel = stateNameMap.get(stateName);
            buffer.append(stateName).append(":");
            if (stateModel.getValue() != null && !stateModel.getValue().isEmpty()) {
                if (stateModel.getValue().equalsIgnoreCase("null")) {
                    buffer.append("null,");
                } else if (stateModel.getType().equalsIgnoreCase("string")) {
                    buffer.append("'" + stateModel.getValue() + "',");
                } else {
                    buffer.append(stateModel.getValue() + ",");
                }
            } else if (stateModel.getType() != null && !stateModel.getType().isEmpty()) {
                buffer.append(stateModel.getType() + ",");
            } else {
                buffer.append("Object,");
            }
        }
        buffer.deleteCharAt(buffer.length() - 1);
        buffer.append("}};");

        buffer.append(appComponent).append("\n");
        return new DefaultSpringVueResource(buffer.toString(), beanMap, groupProvider);
    }

    private static class DefaultSpringVueResource implements SpringVueResource {
        private String vueApp;
        private Map<String, Map.Entry<String, String>> beanMap;
        private GroupProvider groupProvider;

        public DefaultSpringVueResource(String vueApp, Map<String, Map.Entry<String, String>> beanMap, GroupProvider groupProvider) {
            this.vueApp = vueApp;
            this.beanMap = beanMap;
            this.groupProvider = groupProvider;
        }

        @Override
        public String getVueApp() {
            return vueApp;
        }

        @Override
        public Map<String, Map.Entry<String, String>> getBeanMap() {
            return beanMap;
        }

        @Override
        public void refresh() {
            SpringVueResource springVueResource = ActionInstaller.refreshSpringVueResource(this, groupProvider);
            this.vueApp = springVueResource.getVueApp();
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
        private List<RouteModel> childeren;
        private Object props;

        public RouteModel() {
        }

        public RouteModel(String path, String redirect, String component, List<RouteModel> childeren, Object props) {
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

        public List<RouteModel> getChilderen() {
            return childeren;
        }

        public void setChilderen(List<RouteModel> childeren) {
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
