package cz.vity.freerapid.plugins.services.filebaseto;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;

/**
 * @author Ladislav Vitasek
 */
public class FilebaseToServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "filebase.to";


    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 3;
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new FilebaseToRunner().run(downloader);
    }

}