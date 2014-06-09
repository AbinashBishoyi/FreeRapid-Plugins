package cz.vity.freerapid.plugins.services.mp4parser.test;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.ItunesBuilder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.FlvAacTrackImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author tong2shot
 */
public class testDemux {
    public static void main(String[] args) throws Exception {
        String dir = "/media/LNXDEV/frd/frd-test/audioExtractor test";


        /*
        //Conversion, not extraction
        //FAILED
        String fname = "Cássia Eller - Bufo & Spallanzani - Dentro De Ti-aac.flv";
        File inputFile = new File(dir,fname);
        File outFile = new File(dir,fname.replaceFirst("\\..{3}$",".mp3"));
        FileOutputStream fos = new FileOutputStream(outFile);
        InputStream is = new FlvToMp3InputStream(new FileInputStream(inputFile));
        byte[] buffer = new byte[1024 * 1024];
        int len;
        int size = 0;
        try {
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                size += len;
            }
        } finally {
            is.close();
            fos.close();
        }
        */

        /*
        //-------------------------------------------------------
        String fname = "Ek Thi Daayan - Kaali Kaali-mp3.flv";
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            File inputFile = new File(dir, fname);
            File outFile = new File(dir, fname.replaceFirst("\\..{3}$", ".mp3"));
            fos = new FileOutputStream(outFile);
            is = new FlvToMp3InputStream(new FileInputStream(inputFile));
            byte[] buffer = new byte[64 * 1024];
            int len;
            int size = 0;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                size += len;
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    //
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    //
                }
            }
        }
        */

        //-------------------------------------------------------
        String fname = "Cássia Eller - Bufo & Spallanzani - Dentro De Ti-aac.flv";
        FileOutputStream fos = null;
        try {
            FlvAacTrackImpl flvAACTrack = new FlvAacTrackImpl(new FileDataSourceImpl(new File(dir, fname)));
            Movie m = new Movie();
            m.addTrack(flvAACTrack);
            Container out = new ItunesBuilder().build(m);
            File outFile = new File(dir, fname.replaceFirst("\\..{3}$", ".m4a"));
            fos = new FileOutputStream(outFile);
            out.writeContainer(fos.getChannel());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    //
                }
            }
        }

        //-------------------------------------------------------
        fname = "The Who - Baba O'Riley-aac.mp4";
        fos = null;
        try {
            Movie m = MovieCreator.build(new FileDataSourceImpl(new File(dir, fname)));
            Container out = new ItunesBuilder().build(m);
            File outFile = new File(dir, fname.replaceFirst("\\..{3}$", ".m4a"));
            fos = new FileOutputStream(outFile);
            out.writeContainer(fos.getChannel());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    //
                }
            }
        }
    }
}
