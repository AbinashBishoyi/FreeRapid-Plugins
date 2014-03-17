package cz.vity.freerapid.plugins.services.filebaseto;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;


import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek
 */
public class FilebaseToServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "filebase.to";
    private final static Pattern pattern = Pattern.compile("http://(www\\.)?filebase\\.to/.*", Pattern.CASE_INSENSITIVE);

    public String getName() {
        return SERVICE_NAME;
    }

    public int getMaxDownloadsFromOneIP() {
        return 3;
    }

    public boolean supportsURL(String url) {
        return pattern.matcher(url).matches();
    }

    public void run(HttpFileDownloader downloader) throws Exception {
        super.run(downloader);
        new FilebaseToRunner().run(downloader);
    }

}