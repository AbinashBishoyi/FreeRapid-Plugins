package cz.vity.freerapid.plugins.services.ulozto;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class UlozToServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "uloz.to";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new UlozToRunner().run(downloader);
    }

}