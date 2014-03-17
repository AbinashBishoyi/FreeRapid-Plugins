package cz.vity.freerapid.plugins.services.netloadin;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek
 */
public class NetloadInShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "netload.in";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 2;
    }


    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new NetloadInRunner().run(downloader);
    }

}