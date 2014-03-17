package cz.vity.freerapid.plugins.services.zippyshare.js;

import sun.org.mozilla.javascript.internal.ScriptableObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ntoskrnl
 */
@SuppressWarnings("unused")
public class JsDocument extends ScriptableObject {

    private final Map<String, Object> elements = new HashMap<String, Object>();
    private String pageContent;

    public JsDocument() {
    }

    public JsDocument(final String pageContent) {
        this.pageContent = pageContent;
    }

    {
        defineFunctionProperties(new String[]{"getElementById"}, JsDocument.class, 0);
    }

    @Override
    public String getClassName() {
        return "JsDocument";
    }

    @Override
    public Object getDefaultValue(final Class<?> aClass) {
        return toString();
    }

    public Object getElementById(final String id) {
        Object element = elements.get(id);
        if (element == null) {
            element = new JsElement(id, pageContent);
            elements.put(id, element);
        }
        return element;
    }

}
