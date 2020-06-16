package ir.piana.dev.modular.common;

import java.util.HashMap;
import java.util.Map;

public class NumberUtils {
    static Map baseStrings = new HashMap();
    static {
        baseStrings.put(new Long(0), "صفر");
        baseStrings.put(new Long(1), "يک");
        baseStrings.put(new Long(2), "دو");
        baseStrings.put(new Long(3), "سه");
        baseStrings.put(new Long(4), "چهار");
        baseStrings.put(new Long(5), "پنج");
        baseStrings.put(new Long(6), "شش");
        baseStrings.put(new Long(7), "هفت");
        baseStrings.put(new Long(8), "هشت");
        baseStrings.put(new Long(9), "نه");
        baseStrings.put(new Long(10), "ده");
        baseStrings.put(new Long(11), "يازده");
        baseStrings.put(new Long(12), "دوازده");
        baseStrings.put(new Long(13), "سيزده");
        baseStrings.put(new Long(14), "چهارده");
        baseStrings.put(new Long(15), "پانزده");
        baseStrings.put(new Long(16), "شانزده");
        baseStrings.put(new Long(17), "هفده");
        baseStrings.put(new Long(18), "هيجده");
        baseStrings.put(new Long(19), "نوزده");
        baseStrings.put(new Long(20), "بيست");
        baseStrings.put(new Long(30), "سي");
        baseStrings.put(new Long(40), "چهل");
        baseStrings.put(new Long(50), "پنجاه");
        baseStrings.put(new Long(60), "شصت");
        baseStrings.put(new Long(70), "هفتاد");
        baseStrings.put(new Long(80), "هشتاد");
        baseStrings.put(new Long(90), "نود");
        baseStrings.put(new Long(100), "یکصد");
        baseStrings.put(new Long(200), "دويست");
        baseStrings.put(new Long(300), "سيصد");
        baseStrings.put(new Long(400), "چهارصد");
        baseStrings.put(new Long(500), "پانصد");
        baseStrings.put(new Long(600), "ششصد");
        baseStrings.put(new Long(700), "هفتصد");
        baseStrings.put(new Long(800), "هشتصد");
        baseStrings.put(new Long(900), "نهصد");
        baseStrings.put(new Long(1000), "هزار");
        baseStrings.put(new Long(1000000), "ميليون");
        baseStrings.put(new Long(1000000000), "ميليارد");
        baseStrings.put(new Long(1000000000000L), " بیلیون ");
        baseStrings.put(new Long(1000000000000000L), "ميليون ميليارد");
        baseStrings.put(new Long(1000000000000000000L), "ميليارد ميليارد");
    }

    public static long getX(long x) {
        long c = 0;
        while((x = x / 1000) > 0)
            c++;
        c = (long) Math.pow(1000, c);
        return c;
    }

    public static StringBuffer getAsString(long n) {
        if (n >= 1000) {
            String s = (String) baseStrings.get(new Long(n));
            if (s != null) {
                StringBuffer sb = new StringBuffer();
                sb.append("یک ");
                sb.append(s);
                return sb;
            }
        }
        return getAsStringInternal(n);
    }

    private static StringBuffer getAsStringInternal(long n) {
        StringBuffer sb = new StringBuffer();
        String s = (String) baseStrings.get(new Long(n));
        if (s != null) {
            sb.append(s);
            return sb;
        }
        if (n < 0) {
            sb.append("-");
            return sb.append(getAsStringInternal(-n));
        } else if (n < 1000) {
            long n3 = (n < 100) ? 10 : 100;
            long n1 = n % n3;
            long n2 = n - n1;
            sb.append(getAsStringInternal(n2));
            if (n1 > 0) {
                sb.append(" و ").append(getAsStringInternal(n1));
            }
        } else {
            long n3 = getX(n);
            long n1 = n % n3;
            long n2 = n / n3;
            sb.append(getAsStringInternal(n2)).append(' ').append(getAsStringInternal(n3));
            if (n1 > 0) {
                sb.append(" و ").append(getAsStringInternal(n1));
            }
        }
        return sb;
    }

    public static boolean isNumber(String s) {
        try {
            Long.parseLong(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        System.out.println(getAsString(100));
        System.out.println(getAsString(100000));
        System.out.println(getAsString(136890));
        System.out.println(getAsString(1000));
        System.out.println(getAsString(1020));
        System.out.println(getAsString(1121));
    }
}

