package cz.vity.freerapid.plugins.services.shareflare.captcha;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * @author RickCL
 */
public class CaptchaReader {

    private final static Logger logger = Logger.getLogger(CaptchaReader.class.getName());

    private static TreeMap<String, Matrix> chars;

    private String word = "";

    @SuppressWarnings("unchecked")
    public CaptchaReader(BufferedImage ocr) throws Exception {

        if (chars == null) {
            synchronized (CaptchaReader.class) {
                if (chars == null) {
                    InputStream in = this.getClass().getResourceAsStream("/resources/shareflare-captcha.bin");
                    //InputStream in = new ByteArrayInputStream(letters);
                    GZIPInputStream gzis = new GZIPInputStream(in);
                    ObjectInputStream ois = new ObjectInputStream(gzis);
                    chars = (TreeMap<String, Matrix>) ois.readObject();
                }
            }
        }

        ArrayList<int[]> resul = new ArrayList<int[]>();
        Matrix mocr = bufferedImage2Matrix(ocr);
        for (String key : chars.keySet()) {
            Matrix m = chars.get(key);
            for (int[] i : mocr.contains(m, 5)) {
                if (i[2] >= 0) {
                    resul.add(new int[] { key.charAt(0), i[0], i[1], i[2] });
                }
            }
        }

        Comparator<int[]> c = new Comparator<int[]>() {
            public int compare(int[] o1, int[] o2) {
                if (o1[2] > o2[2])
                    return 10;
                if (o1[2] < o2[2])
                    return -10;
                if (o1[2] == o2[2]) {
                    if (o1[3] > o2[3])
                        return 1;
                    if (o1[3] < o2[3])
                        return -1;
                }
                return 0;
            }
        };

        Collections.sort(resul, c);
        while (resul.size() > 6) {
            int[] remove = null;
            for (int[] i : resul) {
                if (remove == null || i[3] > remove[3])
                    remove = i;
            }
            resul.remove(remove);
        }

        for (int[] i : resul) {
            word += (char) i[0];
            logger.fine("Captcha character factor : " + (char) i[0] + "\t" + i[1] + "," + i[2] + "\t" + i[3]);
        }
        logger.fine("Captcha recognized :" + word);
    }

    public Matrix bufferedImage2Matrix(BufferedImage image) {
        Matrix m = new Matrix(image.getHeight(), image.getWidth());
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                m.set(y, x, (new Color(image.getRGB(x, y))).getBlue() < 128 ? 1 : 0);
            }
        }
        return m;
    }

    public String getWord() {
        return word;
    }

}
