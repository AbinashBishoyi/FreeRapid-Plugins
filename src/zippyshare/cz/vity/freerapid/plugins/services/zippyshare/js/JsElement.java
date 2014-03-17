package cz.vity.freerapid.plugins.services.zippyshare.js;

import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import sun.org.mozilla.javascript.internal.ScriptableObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ntoskrnl
 */
@SuppressWarnings("unused")
public class JsElement extends ScriptableObject {

    private String id;
    private String pageContent;

    public JsElement() {
    }

    public JsElement(final String id, final String pageContent) {
        this.id = id;
        this.pageContent = pageContent;
    }

    {
        defineFunctionProperties(new String[]{"getAttribute"}, JsElement.class, 0);
    }

    @Override
    public String getClassName() {
        return "JsElement";
    }

    @Override
    public Object getDefaultValue(final Class<?> aClass) {
        return toString();
    }

    public Object getAttribute(final String attribute) {
        Matcher matcher = PlugUtils.matcher("(<[^<>]+?\\bid=\"" + Pattern.quote(id) + "\"[^<>]+?>)", pageContent);
        if (matcher.find()) {
            matcher = PlugUtils.matcher(Pattern.quote(attribute) + "=\"(.+?)\"", matcher.group(1));
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        throw new RuntimeException("Attribute " + attribute + " not found for element " + id);
    }

}
