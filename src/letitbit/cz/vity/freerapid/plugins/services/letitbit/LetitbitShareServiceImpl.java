package cz.vity.freerapid.plugins.services.letitbit;

import cz.vity.freerapid.plugins.webclient.AbstractFileShareService;
import cz.vity.freerapid.plugins.webclient.HttpFileDownloader;
import cz.vity.freerapid.plugins.services.letitbit.LetitbitRunner;

import java.util.regex.Pattern;

/**
 * @author Ladislav Vitasek
 */
public class LetitbitShareServiceImpl extends AbstractFileShareService {
    private static final String SERVICE_NAME = "letitbit.net";
    private final static Pattern pattern = Pattern.compile("http://(www\\.)?letitbit\\.net/.*", Pattern.CASE_INSENSITIVE);

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
        new LetitbitRunner().run(downloader);
    }

}