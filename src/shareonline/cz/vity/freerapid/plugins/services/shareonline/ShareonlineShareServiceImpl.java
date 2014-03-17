package cz.vity.freerapid.plugins.services.shareonline;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek
 */
public class ShareonlineShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "share-online.biz";
    private ServicePluginContext context = new ServicePluginContext();

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }


    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new ShareonlineRunner().run(downloader, context);
    }

}