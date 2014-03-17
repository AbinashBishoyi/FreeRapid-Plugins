package jd.plugins;

import java.io.Serializable;

/**
 * Stub required for serialization
 *
 * @see cz.vity.freerapid.plugins.container.impl.Jdc
 */
public class DownloadLink implements Serializable {

    private static final long serialVersionUID = 1981079856214268373L;

    public String urlDownload;
    public String forcedFileName;
    public String finalFileName;
    public String name;
    public long downloadMax;

}
