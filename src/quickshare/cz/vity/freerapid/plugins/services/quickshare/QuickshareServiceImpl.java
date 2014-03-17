package cz.vity.freerapid.plugins.services.quickshare;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek, Ludek Zika
 */
public class QuickshareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "quickshare.cz";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new QuickshareRunner().run(downloader);
    }

}