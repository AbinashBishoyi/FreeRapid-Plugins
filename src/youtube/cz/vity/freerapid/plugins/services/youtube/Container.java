package cz.vity.freerapid.plugins.services.youtube;

/**
 * @author tong2shot
 */
public enum Container {
    Any(0),
    mp4(1),
    flv(2),
    webm(3),
    _3gp(4);

    private final String name;
    private final String fileExt;

    private Container(int index) {
        switch (index) {
            case 0:
                name = "Any container";
                fileExt = "Any";
                break;
            case 4:
                name = "3GP";
                fileExt = ".3gp";
                break;
            default:
                name = super.toString().toUpperCase();
                fileExt = "." + super.toString().toLowerCase();
                break;
        }
    }

    public String getName() {
        return name;
    }

    public String getFileExt() {
        return fileExt;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Container[] getItems() {
        return values();
    }
}
