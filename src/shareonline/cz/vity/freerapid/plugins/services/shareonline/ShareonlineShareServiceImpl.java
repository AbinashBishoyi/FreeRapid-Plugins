package cz.vity.freerapid.plugins.services.shareonline;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;


import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek
 */
public class ShareonlineShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "share-online.biz";
    private final static Pattern pattern = Pattern.compile("http://(www\\.)?share-online\\.biz/.*", Pattern.CASE_INSENSITIVE);
    private ServicePluginContext context = new ServicePluginContext();

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 1;
    }

    public boolean supportsURL(String url) {
        return pattern.matcher(url).matches();
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new ShareonlineRunner().run(downloader, context);
    }

}