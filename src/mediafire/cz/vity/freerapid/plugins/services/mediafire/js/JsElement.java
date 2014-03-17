package cz.vity.freerapid.plugins.services.mediafire.js;

import sun.org.mozilla.javascript.internal.ScriptableObject;

import java.lang.reflect.Method;

/**
 * @author ntoskrnl
 */
@SuppressWarnings("unused")
public class JsElement extends ScriptableObject {

    private final static Method GETTER1, SETTER1, GETTER2, GETTER3, SETTER3;

    static {
        try {
            GETTER1 = JsElement.class.getDeclaredMethod("jsGet_innerHTML");
            SETTER1 = JsElement.class.getDeclaredMethod("jsSet_innerHTML", String.class);
            GETTER2 = JsElement.class.getDeclaredMethod("jsGet_style");
            GETTER3 = JsElement.class.getDeclaredMethod("jsGet_display");
            SETTER3 = JsElement.class.getDeclaredMethod("jsSet_display", String.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static JsElement newInstance(final String id) {
        final JsElement element = new JsElement(id);
        element.defineProperty("innerHTML", null, GETTER1, SETTER1, PERMANENT | DONTENUM);
        element.defineProperty("style", null, GETTER2, null, PERMANENT | DONTENUM | READONLY);
        element.defineProperty("display", null, GETTER3, SETTER3, PERMANENT | DONTENUM);
        return element;
    }

    private String id;
    private String text;
    private String display;

    public JsElement() {
    }

    private JsElement(final String id) {
        this.id = id;
    }

    @Override
    public String getClassName() {
        return "JsElement";
    }

    public Object jsGet_innerHTML() {
        return text;
    }

    public void jsSet_innerHTML(final String text) {
        //System.out.println(id + " innerHTML = " + text);
        this.text = text;
    }

    public Object jsGet_style() {
        return this;
    }

    public Object jsGet_display() {
        return display;
    }

    public void jsSet_display(final String display) {
        //System.out.println(id + " style.display = " + display);
        this.display = display;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public boolean isVisible() {
        return "block".equals(display);
    }

}
