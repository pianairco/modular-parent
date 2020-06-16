package ir.piana.dev.sqlcreator.data.sql;

/**
 * Created by mj.rahmati on 12/4/2019.
 */
public class SelectColumnDef {
    String name;
    String as;
    String type;
    String property;
    boolean isShow;
    boolean isSortable;
    String title;

    public SelectColumnDef() {
    }

    public SelectColumnDef(String name, String type) {
        this(name, name, type);
    }

    public SelectColumnDef(String name, String as, String type) {
        this(name, as, type, null, false, null);
    }

    public SelectColumnDef(String name, String as, String type, String property, boolean isShow) {
        this(name, as, type, property, isShow, null);
    }

    public SelectColumnDef(String name, String as, String type, String property, boolean isShow, String title) {
        this.name = name;
        this.title = title;
        if(as == null || as.isEmpty())
            this.as = name;
        else
            this.as = as;
        this.type = type;
        if(property == null) {
//            this.property = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, this.as);
        } else {
            this.property = property;
        }
        if(this.property == null || this.property.isEmpty())
            this.isShow = false;
        else
            this.isShow = true;
    }

    public String getName() {
        return name;
    }

    public String getAs() {
        return as;
    }

    public String getType() {
        return type;
    }

    public String getProperty() {
        return property;
    }

    public boolean isShow() {
        return isShow;
    }

    public boolean isSortable() {
        return isSortable;
    }

    public String getTitle() {
        return title == null || title.isEmpty() ?  property : title;
    }
}
