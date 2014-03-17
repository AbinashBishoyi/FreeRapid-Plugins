package cz.vity.freerapid.plugins.services.circlecaptcha;

/**
 * @author ntoskrnl
 */
public class Circle {

    private final int x, y, r;

    public Circle(final int x, final int y, final int r) {
        this.x = x;
        this.y = y;
        this.r = r;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int r() {
        return r;
    }

}
