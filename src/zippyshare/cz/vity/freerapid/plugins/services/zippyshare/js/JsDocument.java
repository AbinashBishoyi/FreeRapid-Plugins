package cz.vity.freerapid.plugins.services.zippyshare.js;

import sun.org.mozilla.javascript.internal.ScriptableObject;

/**
 * @author ntoskrnl
 */
@SuppressWarnings("unused")
public class JsDocument extends ScriptableObject {

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
        return new JsElement(id, pageContent);
    }

}
