package ir.piana.dev.springmodular.core.action;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

abstract class VueComponentProvider {
    final String getComponentHtml() throws IOException {
        String pName = this.getClass().getPackage().getName();
        String html = "/" + pName.replaceAll("\\.", "\\/").concat("/").concat("app.vue.html");
        return IOUtils.toString(this.getClass().getResourceAsStream(html), "UTF-8");
    }

    final String getComponentJs() throws IOException {
        String pName = this.getClass().getPackage().getName();
        String html = "/" + pName.replaceAll("\\.", "\\/").concat("/").concat("app.vue.js");
        return IOUtils.toString(this.getClass().getResourceAsStream(html), "UTF-8");
    }

    final String getComponentStore() throws IOException {
        String pName = this.getClass().getPackage().getName();
        String html = "/" + pName.replaceAll("\\.", "\\/").concat("/").concat("store.vue.js");
        return IOUtils.toString(this.getClass().getResourceAsStream(html), "UTF-8");
    }

    final VueApp getVueApp() {
        return this.getClass().getDeclaredAnnotation(VueApp.class);
    }
}
