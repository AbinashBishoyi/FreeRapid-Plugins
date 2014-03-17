package cz.vity.freerapid.plugins.services.yourfilehost;

import cz.vity.freerapid.plugins.exceptions.InvalidURLOrServiceProblemException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author Alex
 */
class YourFileHostRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(YourFileHostRunner.class.getName());

    @Override
    public void runCheck() throws Exception {
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);
        if (makeRequest(getMethod)) {
            checkNameandSize(getContentAsString());
        } else
            throw new PluginImplementationException("Problem with a connection to service.\nCannot find requested page content");
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        GetMethod getMethod = getGetMethod(fileURL);

        if (makeRedirectedRequest(getMethod)) {
            String contentAsString = getContentAsString();
            checkNameandSize(contentAsString);
            ///<param name="movie" value="http://www.yourfilehost.com/flash/flvplayer7.swf?autoStart=1&no_skin_menu=1&video=http%3A%2F%2Fwww.yourfilehost.com%2Fvideo-embed.php%3Fvidlink%3D%26cid%3D26330e1832d639cd184a61890d5219ff%26adult%3D%26cat%3Dvideo%26file%3Dbush_shoe_attack.flv%26family%3Don%26key%3D202.77.98%26cdn%3D1&videoembed_id=http%3A%2F%2Fwww.yourfilehost.com%2Fmedia.php%3Fcat%3Dvideo%26file%3Dbush_shoe_attack.flv&search_url=http%3A%2F%2Fwww.flurl.com%2Fsearch%3F1%26site_id%3D147&src_ip=&postroll=0" />

            //Matcher matcher = PlugUtils.matcher("<div class=\"download\"><a href=\"([^\"<]*)", contentAsString);
            String vidEmbed = PlugUtils.getParameter("movie", contentAsString);
            vidEmbed = URLDecoder.decode(vidEmbed, "UTF-8");

            if (vidEmbed.contains("video-embed")) {

                Matcher vidURL = PlugUtils.matcher("(www.yourfilehost.com/video-embed.php[^\"]+)&adult", vidEmbed);
                if (vidURL.find()) {


                    getMethod = getGetMethod("http://" + vidURL.group(1));


                    if (makeRequest(getMethod)) {
                        contentAsString = getContentAsString();
                        contentAsString = URLDecoder.decode(contentAsString, "UTF-8");
                        //video_id=http://cdn.yourfilehost.com/unit1/flash8/26/26330e1832d639cd184a61890d5219ff.flv&homeurl=http://www.yourfilehost.com&photo=http://cdnl3.yourfilehost.com/thumbs/26/26330e1832d639cd184a61890d5219ff.jpg&video_url_fs=javascript:fullscreen&flash_skin=100&url_share=http://www.yourfilehost.com/linkshare.php?cat=&file=&shared_same=0&xml_url=http://www.yourfilehost.com/flv-xml.php?cid=26330e1832d639cd184a61890d5219ff&adult=&rand=9553&xml_url2=http://traffic.liveuniversenetwork.com/xml/zones.rss?id=127&ip=202.77.98.130&ses_id=MjAyLjc3Ljk4LjEzMCwxMjI5NjYxODAy&embed=http://www.yourfilehost.com/media.php?cat=&file=&lu_userid_text=C2BA90801FB7450F88EE88DB0255220F&lu_siteid_text=42&ad_liveuniverse_type=3&ad_liveuniverse=1&lu_userid_postroll=3FA8DCD497384EF28089CC4517D88CC9&lu_siteid_postroll=32&family=on


                        vidURL = PlugUtils.matcher("video_id=(http://[^&]+.flv)", contentAsString);
                        if (vidURL.find()) {

                            getMethod = getGetMethod(vidURL.group(1));
                            client.getHTTPClient().getParams().setParameter("considerAsStream", "text/plain");
                            if (!tryDownloadAndSaveFile(getMethod)) {
                                checkProblems();
                                logger.warning(getContentAsString());//something was really wrong, we will explore it from the logs :-)
                                throw new IOException("File input stream is empty.");
                            } 

                        } else throw new InvalidURLOrServiceProblemException("Can't find download link 2");
                    } else throw new InvalidURLOrServiceProblemException("Can't load download link 3");
                } else throw new InvalidURLOrServiceProblemException("Can't find download link 4");
            } else throw new InvalidURLOrServiceProblemException("Can't find download link 5");
        } else throw new InvalidURLOrServiceProblemException("Can't load download link 6");
    }

    private void checkNameandSize(String content) throws Exception {

        if (!content.contains("yourfilehost.com")) {
            logger.warning(getContentAsString());
            throw new InvalidURLOrServiceProblemException("Invalid URL or unindentified service");
        }
        if (content.contains("File doesn")) {
            throw new URLNotAvailableAnymoreException("<b>SaveFile error:</b><br>File doesn't exist");
        }

//http://www.yourfilehost.com/media.php?cat=video&file=bush_shoe_attack.flv
        Matcher xmatcher = PlugUtils.matcher("file=([^<]*)", fileURL);
        if (xmatcher.find()) {
            final String fileName = xmatcher.group(1).trim(); //method trim removes white characters from both sides of string
            logger.info("File name " + fileName);
            httpFile.setFileName(fileName);

        } else logger.warning("File name was not found" + content);


        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);

    }


    private void checkProblems() throws ServiceConnectionProblemException {
        if (getContentAsString().contains("already downloading")) {
            throw new ServiceConnectionProblemException(String.format("<b>SaveFile Error:</b><br>Your IP address is already downloading a file. <br>Please wait until the download is completed."));
        }
        if (getContentAsString().contains("Currently a lot of users")) {
            throw new ServiceConnectionProblemException(String.format("<b>SaveFile Error:</b><br>Currently a lot of users are downloading files."));
        }
    }

}