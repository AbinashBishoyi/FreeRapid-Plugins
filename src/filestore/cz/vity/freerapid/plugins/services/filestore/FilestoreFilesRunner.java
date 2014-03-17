package cz.vity.freerapid.plugins.services.filestore;

import cz.vity.freerapid.plugins.exceptions.*;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.util.HashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author RickCL
 */
class FilestoreFilesRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(FilestoreFilesRunner.class.getName());
    private static final String URI_BASE = "http://filestore.to";


    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());
        } else
            throw new PluginImplementationException();
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        try {
            PlugUtils.checkFileSize(httpFile, content, "<td width=\"220\" style=\"font-weight:bold;\">", "</td>");
            PlugUtils.checkName(httpFile, content, "<td colspan=\"2\" style=\"color:#DD0000; font-weight:bold;\">", "</td>");
        } catch(PluginImplementationException e) {
            checkProblems("checkNameAndSize");
        }
    }


    @Override
    public void run() throws Exception {
        super.run();

        GetMethod getMethod = getGetMethod(fileURL);
        if (makeRedirectedRequest(getMethod)) {
            checkNameAndSize(getContentAsString());

            downloadTask.sleep(10 + 1);

            //URL=http://filestore.to/ajax/download.php?a=1&f=89762E7911&s=a0fe3c1466f541cf1e504c23872c357f
            HashMap<String, String> param = new HashMap<String, String>(2);
            Matcher matcher = getMatcherAgainstContent("<input type=\"hidden\" name=\"([\\w]+)\" value=\"([\\w]+)\">");
            while( matcher.find() ) {
                param.put( matcher.group(1), matcher.group(2) );
            }
            logger.fine( param.toString() );

            getMethod = getGetMethod(URI_BASE + "/ajax/download.php?a=1&f=" + param.get("fileid") + "&s=" + param.get("sid") );
            if( !makeRedirectedRequest(getMethod) ) {
                checkProblems("ajax get 1");
            }

            getMethod = getGetMethod(URI_BASE + "/ajax/download.php?f=" + param.get("fileid") + "&s=" + param.get("sid") );
            if( !makeRedirectedRequest(getMethod) ) {
                checkProblems("ajax get 2");
            }

            getMethod = getGetMethod( getContentAsString() );
            if (!tryDownloadAndSaveFile(getMethod)) {
                checkProblems("Download Fail !!");
            }

        } else
            throw new PluginImplementationException();
    }

    private void checkProblems(String step) throws ServiceConnectionProblemException, YouHaveToWaitException, URLNotAvailableAnymoreException, PluginImplementationException {
        String content = getContentAsString();
        if (content.contains("\u0421 \u0432\u0430\u0448\u0435\u0433\u043e IP \u0430\u0434\u0440\u0435\u0441\u0430")) {
            throw new ServiceConnectionProblemException(String.format("<b>Your IP is already downloading a file from our system.</b><br>You cannot download more than one file in parallel."));
        }
        if (content.contains("Download-Datei wurde nicht gefunden")) {
            throw new URLNotAvailableAnymoreException(String.format("Downloadfile was not found. Either the file was removed for the downloadleft from our servers or was wrong."));
        }
        /** For debug
        try {
            FileOutputStream f = new FileOutputStream("error-content.html");
            f.write( content.getBytes() );
            f.close();
        } catch(Exception e) {}
        /**/
        throw new PluginImplementationException("Step " + step);
    }

}
