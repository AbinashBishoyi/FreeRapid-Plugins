package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek
 */
public class LetitbitShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "letitbit.net";

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new LetitbitRunner().run(downloader);
    }

}