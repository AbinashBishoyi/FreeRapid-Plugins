package cz.vity.freerapid.plugins.services.mediafire.js;

import sun.org.mozilla.javascript.internal.ScriptableObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ntoskrnl
 */
@SuppressWarnings("unused")
public class JsDocument extends ScriptableObject {

    private final Map<String, JsElement> elements = new HashMap<String, JsElement>();

    public JsDocument() {
    }

    @Override
    public String getClassName() {
        return "JsDocument";
    }

    public Object jsFunction_getElementById(final String id) {
        JsElement element = elements.get(id);
        if (element == null) {
            element = JsElement.newInstance(id);
            elements.put(id, element);
        }
        return element;
    }

    public Map<String, JsElement> getElements() {
        return elements;
    }

}
