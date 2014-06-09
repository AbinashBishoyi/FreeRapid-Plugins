package cz.vity.freerapid.plugins.services.mp4parser.test;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.ItunesBuilder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author tong2shot
 */
public class testItunes {
    public static void main(String[] args) throws Exception {
        String dir = "/media/LNXDEV/frd/frd-test/dash test2";
        String dirOut = "/media/LNXDEV/frd/frd-test/dash test2/itunes-out";
        String fname = "The Who - Baba O'Riley.m4a";

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(dirOut, fname));
            Movie movie = MovieCreator.build(new FileDataSourceImpl(new File(dir, fname)));
            Container out = new ItunesBuilder().build(movie);
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
