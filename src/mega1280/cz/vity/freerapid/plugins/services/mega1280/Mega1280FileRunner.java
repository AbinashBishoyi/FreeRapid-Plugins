package cz.vity.freerapid.plugins.services.mega1280;

import cz.vity.freerapid.plugins.exceptions.CaptchaEntryInputMismatchException;
import cz.vity.freerapid.plugins.exceptions.ErrorDuringDownloadingException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.AbstractRunner;
import cz.vity.freerapid.plugins.webclient.FileState;
import cz.vity.freerapid.plugins.webclient.hoster.CaptchaSupport;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class which contains main code
 *
 * @author Vity, ntoskrnl
 */
class Mega1280FileRunner extends AbstractRunner {
    private final static Logger logger = Logger.getLogger(Mega1280FileRunner.class.getName());
    private final static int COLOR_LIMIT = 200;
    private final int captchaMax = 6;
    private int captchaCounter = 0;

    @Override
    public void runCheck() throws Exception { //this method validates file
        super.runCheck();
        final GetMethod getMethod = getGetMethod(fileURL);//make first request
        if (makeRedirectedRequest(getMethod)) {
            checkProblems();
            checkNameAndSize(getContentAsString());//ok let's extract file name and size from the page
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkNameAndSize(String content) throws ErrorDuringDownloadingException {
        PlugUtils.checkName(httpFile, content, "<span class=\"clr05\"><b>", "</b></span><br />");
        PlugUtils.checkFileSize(httpFile, content, "<strong>", "</strong>\n</td>");
        httpFile.setFileState(FileState.CHECKED_AND_EXISTING);
    }

    @Override
    public void run() throws Exception {
        super.run();
        logger.info("Starting download in TASK " + fileURL);
        final GetMethod method = getGetMethod(fileURL); //create GET request
        if (makeRedirectedRequest(method)) { //we make the main request
            final String contentAsString = getContentAsString();//check for response
            checkProblems();//check problems
            checkNameAndSize(contentAsString);//extract file name and size from the page
            final String firstPage = "name=\"code_security\" id=\"code_security\"";
            while (getContentAsString().contains(firstPage)) {
                final HttpMethod httpMethod = stepCaptcha();
                if (!makeRedirectedRequest(httpMethod)) //reload captcha page or move to another page
                    throw new ServiceConnectionProblemException();
            }
            if (!getContentAsString().contains("hddomainname"))
                throw new ServiceConnectionProblemException("Invalid page content");
            final String hddomainname = PlugUtils.getStringBetween(getContentAsString(), "hddomainname\" style=\"display:none\">", "</div>");
            final String hdfolder = PlugUtils.getStringBetween(getContentAsString(), "hdfolder\" style=\"display:none\">", "</div>");
            final String hdcode = PlugUtils.getStringBetween(getContentAsString(), "hdcode\" style=\"display:none\">", "</div>");
            final String hdfilename = PlugUtils.getStringBetween(getContentAsString(), "hdfilename\" style=\"display:none\">", "</div>");
            downloadTask.sleep(2);
            final HttpMethod httpMethod = getMethodBuilder().setAction(hddomainname + hdfolder + hdcode + "/" + hdfilename).toGetMethod();

            if (!tryDownloadAndSaveFile(httpMethod)) {
                checkProblems();//if downloading failed
                logger.warning(getContentAsString());//log the info
                throw new ServiceConnectionProblemException("Error starting download");//some unknown problem
            }
        } else {
            checkProblems();
            throw new ServiceConnectionProblemException();
        }
    }

    private void checkProblems() throws ErrorDuringDownloadingException {
        final String contentAsString = getContentAsString();
        if (contentAsString.contains("File Not Found")
                || contentAsString.contains("File not found")
                || contentAsString.contains("Li\u00EAn k\u1EBFt b\u1EA1n v\u1EEBa ch\u1ECDn kh\u00F4ng t\u1ED3n t\u1EA1i tr\u00EAn h\u1EC7 th\u1ED1ng")
                || contentAsString.contains("Y\u00EAu c\u1EA7u kh\u00F4ng \u0111\u01B0\u1EE3c t\u00ECm th\u1EA5y")) {
            throw new URLNotAvailableAnymoreException("File not found"); //let user know in FRD
        }
        if (contentAsString.contains("Vui l\u00F2ng ch\u1EDD cho l\u01B0\u1EE3t download k\u1EBF ti\u1EBFp"))
            throw new ServiceConnectionProblemException("Please wait for your previous download to finish");
        if (contentAsString.contains("Limit download xx !")) {
            throw new ServiceConnectionProblemException("Limit download xx ! - unknown error message from server");
        }
    }

    private HttpMethod stepCaptcha() throws Exception {
        final CaptchaSupport captchaSupport = getCaptchaSupport();
        final String captchaURL = "http://mega.1280.com/security_code.php";
        final String captcha;
        if (captchaCounter < captchaMax) {
            ++captchaCounter;
            final BufferedImage captchaImage = prepareCaptchaImage(captchaSupport.getCaptchaImage(captchaURL));
            //captcha = PlugUtils.recognize(captchaImage, "-d -1 -C a-z-0-9");
            captcha = new CaptchaRecognizer().recognize(captchaImage);
            logger.info("Attempt " + captchaCounter + " of " + captchaMax + ", OCR recognized " + captcha);
        } else {
            captcha = captchaSupport.getCaptcha(captchaURL);
            if (captcha == null) throw new CaptchaEntryInputMismatchException();
            logger.info("Manual captcha " + captcha);
        }

        return getMethodBuilder().setReferer(fileURL).setActionFromFormByName("frm_download", true).setAction(fileURL).setParameter("code_security", captcha).toPostMethod();
    }

    private BufferedImage prepareCaptchaImage(final BufferedImage input) {
        final int w = input.getWidth();
        final int h = input.getHeight();

        //convert input image to greyscale
        final BufferedImage greyScale = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        greyScale.getGraphics().drawImage(input, 0, 0, Color.WHITE, null);

        //convert greyscale image to black and white according to COLOR_LIMIT
        final BufferedImage blackAndWhite = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int red = new Color(greyScale.getRGB(x, y)).getRed();
                final int color = red > COLOR_LIMIT ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                blackAndWhite.setRGB(x, y, color);
            }
        }

        //remove the small distraction dots
        final BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        output.getGraphics().drawImage(blackAndWhite, 0, 0, Color.WHITE, null);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (blackAndWhite.getRGB(x, y) == Color.BLACK.getRGB()) {
                    final int color = getBlock(blackAndWhite, x, y).size() > 5 ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
                    output.setRGB(x, y, color);
                }
            }
        }

        //JOptionPane.showConfirmDialog(null, new ImageIcon(output));

        return output;
    }

    //from megaupload.captcha.CaptchaReader with minor modifications

    private ArrayList<Point> getBlock(final BufferedImage image, final int bx, final int by) {
        ArrayList<Point> block = new ArrayList<Point>();
        int colour = image.getRGB(bx, by);
        List<Point> edge = new ArrayList<Point>();
        edge.add(new Point(bx, by));
        block.add(new Point(bx, by));

        while (edge.size() > 0) {
            List<Point> newedge = new ArrayList<Point>();
            for (final Point p : edge) {
                int x = p.x;
                int y = p.y;
                List<Point> adjacent = new ArrayList<Point>();
                adjacent.add(new Point(x + 1, y));
                adjacent.add(new Point(x - 1, y));
                adjacent.add(new Point(x, y + 1));
                adjacent.add(new Point(x, y - 1));
                for (final Point q : adjacent) {
                    int s = q.x;
                    int t = q.y;

                    if (isWithin(image, s, t) && !block.contains(new Point(s, t)) && image.getRGB(s, t) == colour) {
                        block.add(new Point(s, t));
                        newedge.add(new Point(s, t));
                    }
                }
            }
            edge = newedge;
        }
        return block;
    }

    private boolean isWithin(final BufferedImage image, final int x, final int y) {
        return 0 <= x && x < image.getWidth() && 0 <= y && y < image.getHeight();
    }

}