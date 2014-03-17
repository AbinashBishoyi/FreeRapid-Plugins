package jd.plugins;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Stub required for serialization
 *
 * @see cz.vity.freerapid.plugins.container.impl.Jdc
 */
public class FilePackage implements Serializable {

    private static final long serialVersionUID = -8859842964299890820L;

    public String comment;
    public ArrayList<DownloadLink> downloadLinkList;

}
