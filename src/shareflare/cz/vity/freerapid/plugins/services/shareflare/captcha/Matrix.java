package cz.vity.freerapid.plugins.services.shareflare.captcha;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Matrix implements Comparable<Matrix>, Serializable {

    private static final long serialVersionUID = -3921208452563560481L;

    int width;
    int height;
    int[][] value;

    public Matrix(int h, int w) {
        width = w;
        height = h;
        value = new int[w][h];
    }

    public void set(int y, int x, int value) {
        this.value[x][y] = value;
    }

    public int get(int h, int w) {
        return value[w][h];
    }

    public String toString() {
        String ret = "";
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                ret += get(j, i) + (j + 1 == height ? "" : "\t,");
            }
            ret += "\n";
        }
        return ret;
    }

    public String toJavaArraySource(String key) {
        String ret = "{ ";
        for (int i = 0; i < width; i++) {
            ret += "{";
            for (int j = 0; j < height; j++) {
                ret += get(j, i) + (j + 1 == height ? "" : ",");
            }
            ret += "}" + (i + 1 == width ? " " : ", ");
        }
        return "private final static int[][] " + key + " = new int[][]" + ret + " };";
    }

    @Override
    public int compareTo(Matrix o) {
        if (height != o.height || width != o.width)
            throw new RuntimeException("Matrix don't have same size.");

        double ret = 0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ret += get(i, j) == o.get(i, j) ? 0 : 1;
            }
        }
        return (int) ((ret / (double) (width * height)) * 100.0);
    }

    public Matrix getMatrix(int y, int x, int dy, int dx) {
        Matrix ret = new Matrix(dy, dx);
        for (int i = y; i < y + dy; i++) {
            for (int j = x; j < x + dx; j++) {
                ret.set(i - y, j - x, get(i, j));
            }
        }
        return ret;
    }

    public List<int[]> contains(Matrix o, int factor) {
        Matrix aux;
        List<int[]> ret = new ArrayList<int[]>();
        //int[] ret = new int[] { 0, 0, -1 };
        for (int x = 0; x < width - o.width; x++) {
            for (int y = 0; y < height - o.height; y++) {

                aux = getMatrix(y, x, o.height, o.width);
                int i = aux.compareTo(o);
                if ( i < factor) {
                    ret.add( new int[]{y,x,i} );
                }
            }
        }
        return ret;
    }

}
