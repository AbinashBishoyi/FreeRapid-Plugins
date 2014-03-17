package cz.vity.freerapid.plugins.services.sharebee;

import cz.vity.freerapid.plugins.exceptions.BuildMethodException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.DownloadState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;

import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 */
class SharebeeFileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(SharebeeFileRunner.class.getName());

    private static final String[] SERVER_LIST = {"Megaupload","RapidShare","zSHARE","DepositFiles","Badongo"};

    @Override
    public void runCheck() throws Exception {
        super.runCheck();

        final HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    @Override
    public void run() throws Exception {
        super.run();

        HttpMethod getMethod = getMethodBuilder().setAction(fileURL).toGetMethod();
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());

            String nURL = null;
            for(String type : SERVER_LIST) {
                try {
                    nURL = getMethodBuilder().setActionFromAHrefWhereATagContains( type ).getAction();
                    logger.info(type + " Found: " + nURL);
                    break;
                }catch(BuildMethodException e) {
                    logger.info(type + " Not Found");
                    continue;
                }
            }
            if( nURL != null ) {
                logger.info("New URL :" + nURL);
                this.httpFile.setNewURL(new URL(nURL));
                this.httpFile.setPluginID("");
                this.httpFile.setState(DownloadState.QUEUED);
            } else
                throw new PluginImplementationException("No Server Found !");

        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        Matcher matcher = getMatcherAgainstContent("Filename[^>]+>([^<]+)[^\\(]+\\(([\\w\\.\\s]*)\\)");
        if( !matcher.find() ) {
            throw new PluginImplementationException("File name not found");
        }
        httpFile.setFileName( matcher.group(1).trim() );
        final long size = PlugUtils.getFileSizeFromString(matcher.group(2));
        httpFile.setFileSize(size);
    }

}
