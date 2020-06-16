package ir.piana.dev.sqlcreator.data.sql;

import ir.piana.dev.modular.common.DateUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Function;

/**
 * Created by mj.rahmati on 12/29/2019.
 */
public enum SQLUtilityType {
    TODAY_JALALI("date.today-jalali", SQLUtilityType::getTodayJalali),
    DATE_NOW("date.now", SQLUtilityType::getDateNow),
    TIME_NOW("time.now", SQLUtilityType::getTimeNow);

    private String name;
    private Function<ParameterProvider, String> funtion;

    SQLUtilityType(String name, Function<ParameterProvider, String> funtion) {
        this.name = name;
        this.funtion = funtion;
    }

    public static String execute(String sqlUtilityTypeName, ParameterProvider parameterProvider) {
        String name = sqlUtilityTypeName;
        String type = "s";
        String strings[];
        if((strings = sqlUtilityTypeName.split(":")).length > 1) {
            name = strings[0];
            type = strings[1];
        }
        for (SQLUtilityType sqlUtilityType : SQLUtilityType.values()) {
            if (sqlUtilityType.name.equals(name)) {
                return type.equals("s") ? "'" + sqlUtilityType.funtion.apply(parameterProvider) + "'" : sqlUtilityType.funtion.apply(parameterProvider);
            }
        }
        return "null";
    }

    private static String getTodayJalali(ParameterProvider parameterProvider) {
        return DateUtils.getTodayJalali();
    }

    private static String getDateNow(ParameterProvider parameterProvider) {
        return DateUtils.getTodayJalali();
    }

    static final String TIME_NOW_PATTERN = "HH:mm";
    static final SimpleDateFormat TIME_NOW_FORMATTER =
            new SimpleDateFormat(TIME_NOW_PATTERN);

    private static String getTimeNow(ParameterProvider parameterProvider) {
        return TIME_NOW_FORMATTER.format(new Date());
    }


}
