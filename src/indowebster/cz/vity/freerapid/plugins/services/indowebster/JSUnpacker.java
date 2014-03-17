
package cz.vity.freerapid.plugins.services.indowebster;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides unpacking for packed javascript via Dean Edwards packer
 *
 * Currently not all variants of packing are supported.
 *
 * @author JPEXS
 */
public class JSUnpacker {

    private static String e(int a,int c){
        return (c<a?"":e(a,c/a))+(char)(c%a+161);
    }

    private static String unpack(String p,int a,int c,String k[]){
        while(c-->0){
                p=p.replaceAll(e(a,c),k[c]);
        }
        return p;
    }


    public static String unpackJavaScript(String js){
        Pattern pat=Pattern.compile("return p\\}\\((\\'.*\\')\\.split\\(\\'\\|\\'\\),0,\\{\\}\\)\\)");
        Matcher m=pat.matcher(js);
        if(m.find()){
            String sdec=m.group(1);
            Pattern pat2=Pattern.compile("^'(.*)',([0-9]+),([0-9]+),'(.*)'$");
            m=pat2.matcher(sdec);
            if(m.find()){
                String p=m.group(1);
                p=p.replaceAll("\\\\'", "'");
                p=p.replaceAll("\\\\\"", "\"");
                int a=Integer.parseInt(m.group(2));
                int c=Integer.parseInt(m.group(3));
                String k[]=m.group(4).split("\\|");
                return unpack(p,a,c,k);
            }

        }
        return "";
    }
}
