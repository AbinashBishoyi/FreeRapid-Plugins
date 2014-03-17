package cz.vity.freerapid.plugins.services.kewlshare;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 */
class KewlshareRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(KewlshareRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            //checkNameandSize(getContentAsString());
            String checkcontent = getContentAsString();

            checkName(checkcontent);
            checkSize(checkcontent);
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod getMethoda = getGetMethod(fileURL);
        //getMethoda.setFollowRedirects(true);
        //you can now use makeRedirectedRequest() for working with redirects
        if (makeRedirectedRequest(getMethoda)) {
            String contentAsString = getContentAsString();
            //checkNameandSize(contentAsString);
            checkName(contentAsString);
            checkSize(contentAsString);

//            client.setReferer(fileURL);//referer
//            final PostMethod method = getPostMethod(fileURL);//file url
//            String[] parameters = new String[]{"nL", "cL", "selection"}; //array of parameter names for parsing
//            PlugUtils.addParameters(method, contentAsString, parameters);

            //1 Page - Free/Premium
            //      <form action="" method="post">
            //		<input type="hidden" name="nL" value="BDI=" />
            //		<input type="hidden" name="cL" value="AjU=" />
            //		<input type="hidden" name="selection" value="Free" />
            //		<input  id="imageInput" type="image" border="0"  value="Free User" src="http://kewlshare.com/button/free.gif"  />
            //		</form>

            if (contentAsString.contains("name=\"selection")) {  //Confirm 1st Page
                client.setReferer(fileURL);
                final PostMethod page1 = getPostMethod(fileURL);
                String[] parameters = new String[]{"nL", "cL", "selection"};
                PlugUtils.addParameters(page1, contentAsString, parameters);

                if (makeRequest(page1)) { //Request page 2
                    contentAsString = getContentAsString();
                    if (contentAsString.contains("Please wait until the download begins")) { //Confirming Page 2
                        // Find wait timer var intervel=5;
                        //2 Page -

                        //<form action="" method="post">
                        //<input type="hidden" name="nL" value="V2E=" />
                        //<input type="hidden" name="cL" value="BDI=" />
                        //<input type="hidden" name="act" value="captcha" />
                        //<input id="imageInput" type="image" border="0" src="http://kewlshare.com/img/pod.gif"  value="Enter" /></div></div>
                        //</form>

                        //Matcher matcher = getMatcherAgainstContent("var interval=([0-9]+);");
                        //String s = matcher.group(1);
                        //int seconds = new Integer(s);
                        downloadTask.sleep(5);
                        client.setReferer(fileURL);
                        final PostMethod page2 = getPostMethod(fileURL);
                        String[] parameters2 = new String[]{"nL", "cL", "act"};
                        PlugUtils.addParameters(page2, contentAsString, parameters2);
                        if (makeRequest(page2)) { //request page 3
                            contentAsString = getContentAsString();
                            if (contentAsString.contains("Download Now")) {  //Confirm page 3
                                //3 Page - Download now

                                //<form action="http://lucky.kewlshare.com/dl.php/a09e4b6e7a335ddabf241bb91afbf9c9/Harry.Potter.And.The.Goblet.Of.Fire.2005.720p.nHD.x264.AAC.NhaNc3__mininova_.torrent" method="posT" name="post" target="_blank" onsubmit="downloadMe();" >
                                //<input id="imageInput" type="image" border="0" value="Download  Harry.Potter.And.The.Goblet.Of.Fire.2005.720p.nHD.x264.AAC.NhaNc3__mininova_.torrent" src="http://kewlshare.com/img/down.gif"  /></form>

                                Matcher matcher = getMatcherAgainstContent("<form action=\"([^\"]+)");  //Find Action
                                if (matcher.find()) { //find form action
                                    final PostMethod page3 = getPostMethod(matcher.group(1));
                                    if (!tryDownloadAndSaveFile(page3)) {
                                        checkProblems();
                                        logger.warning(getContentAsString());//something was really wrong, we will explore it from the logs :-)
                                        throw new IOException("File input stream is empty.");
                                    }


                                } else throw new InvalidURLOrServiceProblemException("Can't find Page 3 Action");


                            } else throw new InvalidURLOrServiceProblemException("Can't find Page 3");


                        } else throw new InvalidURLOrServiceProblemException("Can't load Page 3");


                    } else throw new InvalidURLOrServiceProblemException("Can't find Page 2"); //Can't find 2nd page
                }


            } else throw new InvalidURLOrServiceProblemException("Failed to requst Page 2"); //Can't request 2nd page


        } else throw new InvalidURLOrServiceProblemException("Can't find 1st Page"); //Can't find first page


    }


    private void checkName(String content) throws Exception {
        Matcher xmatcher = PlugUtils.matcher("<!-- <title>: ([^<]+)", content);
        if (xmatcher.find()) {
            final String fileName = xmatcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

        } else logger.warning("File name was not found" + content);
    }

    private void checkSize(String content) throws Exception {
        Matcher smatcher = PlugUtils.matcher("(([0-9.]* .B))<", content);
        if (smatcher.find()) {
            final String fileSize = smatcher.group(1);
            logger.info("File size " + fileSize);
            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));
            httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
        } else logger.warning("File size was not found" + content);

    }

//    private void checkNameandSize(String content) throws Exception {
//
//        if (!content.contains("kewlshare.com")) {
//            logger.warning(getContentAsString());
//            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
//        }
//        if (content.contains("File doesn")) {
//            throw new URLNotAvailableAnymoreException("<b>Kewlshare error:</b><br>File doesn't exist");
//        }
//
//        Matcher xmatcher = PlugUtils.matcher("File Name : <strong>([^|]+) || ([^<]+)", content);
//        if (xmatcher.find()) {
//            final String fileSize = xmatcher.group(2);
//            logger.info("File size " + fileSize);
//            httpFile.setFileSize(PlugUtils.getFileSizeFromString(fileSize));
//
//        } else logger.warning("File size was not found" + content);
//
//        xmatcher = PlugUtils.matcher("<!-- <title>: ([^<]+)", content);
//        if (xmatcher.find()) {
//            final String fileName = xmatcher.group(1).trim(); //method trim removes white characters from both sides of string
//            logger.info("File name " + fileName);
//            httpFile.setFileName(fileName);
//
//        } else logger.warning("File name was not found" + content);
//
//
//        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
//
//    }
//

    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>Kewlshare Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>Kewlshare Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

}