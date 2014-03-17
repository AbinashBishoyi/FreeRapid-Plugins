package cz.vity.freerapid.plugins.services.iskladka;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek
 */
public class IskladkaServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "iskladka.cz";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 2;
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new IskladkaRunner().run(downloader);
    }

}