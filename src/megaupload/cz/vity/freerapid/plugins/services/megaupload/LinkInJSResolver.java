package cz.vity.freerapid.plugins.services.megaupload;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ludek Zika
 */

public class LinkInJSResolver {
    static Logger logger;
    private String vars[][] = new String[2][2];


    public LinkInJSResolver(Logger log) {
        logger = log;
    }

    public String FindUrl(String contentAsString) {
        Matcher matcher = Pattern.compile("download</font></a>';\\s*var (.) = ([^;]*);\\s*var (.) = ([^;]*);", Pattern.MULTILINE).matcher(contentAsString);
        if (matcher.find()) {

            vars[0][0] = matcher.group(1);
            vars[0][1] = Expression(matcher.group(2));
            vars[1][0] = matcher.group(3);
            vars[1][1] = Expression(matcher.group(4));


            matcher = Pattern.compile("dlbutton\"\\).innerHTML \\= '<a href=\"([^\"]*)", Pattern.MULTILINE).matcher(contentAsString);
            if (!matcher.find()) return "";
            String rawlink = matcher.group(1);
            String link = parseLink(rawlink);

            return link;
        } else return "";
    }

    char StringToAChar(String s) {
        int i = (int) Integer.valueOf(s);
        return (char) i;

    }

    String Expression(String exp) {
        Matcher matcher;
        if (exp.contains("+")) {

            matcher = Pattern.compile("([^+]*)\\+(.*)", Pattern.MULTILINE).matcher(exp);
            if (matcher.find()) {


                String j = Expression(matcher.group(1));

                String d = Expression(matcher.group(2));
                return j + d;
            }
            return "";
        } else if (exp.contains("Math.abs")) {
            matcher = Pattern.compile("Math.abs\\(-?([^)]*)", Pattern.MULTILINE).matcher(exp);

            if (matcher.find()) {

                char v1c = StringToAChar(matcher.group(1));


                return String.valueOf(v1c);
            }
            return "";
        } else if (exp.contains("Math.sqrt")) {
            matcher = Pattern.compile("Math.sqrt\\(-?([^)]*)", Pattern.MULTILINE).matcher(exp);

            if (matcher.find()) {

                int i = (int) Integer.valueOf(matcher.group(1));
                i = (int) Math.sqrt(i);
                char v2c = (char) i;

                return String.valueOf(v2c);
            }
            return "";

        } else if (exp.contains("'")) {

            matcher = Pattern.compile("'([^']*)'", Pattern.MULTILINE).matcher(exp);
            if (matcher.find()) {

                return matcher.group(1);
            }
            return "";
        } else
            return "";

    }


    String parseLink(String rawlink) {

        String link = "";

        Matcher matcher = Pattern.compile("([^']*)'([^']*)'", Pattern.MULTILINE).matcher(rawlink);
        while (matcher.find()) {
            link = link + matcher.group(1);
            Matcher matcher1 = Pattern.compile("\\+\\s*(\\w+)", Pattern.MULTILINE).matcher(matcher.group(2));
            while (matcher1.find()) {

                link = link + (replaceVar(matcher1.group(1)));
            }


        }
        matcher = Pattern.compile("'([^']*)$", Pattern.MULTILINE).matcher(rawlink);
        if (matcher.find()) {

            link = link + matcher.group(1);


        }

        return link;
    }

    String replaceVar(String v) {
        int varsl = vars.length;
        int i = 0;
        while (i < varsl) {
            if (vars[i][0].equals(v)) return vars[i][1];
            i++;
        }

        return v;
    }

}
