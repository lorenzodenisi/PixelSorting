import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.util.concurrent.TimeUnit;


public class PixelSorting {

    private BufferedImage img1, img2;
    private JFrame frame;
    private int[][] pixels;
    private int x, y;

    private boolean setupDone = false;

    private JFrame setup(String path) throws IOException {

        File imgFile = new File(path);
        img1 = ImageIO.read(imgFile);
        x = img1.getWidth();
        y = img1.getHeight();
        img2 = new BufferedImage(x, y, BufferedImage.TYPE_INT_RGB);

        frame = new JFrame("PixelSorting");
        int yBorder = 30;
        int xBorder = 30;
        frame.setSize(x * 2 + 2 * xBorder, y + 2 * yBorder);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        ImageIcon icon1 = new ImageIcon(img1);
        ImageIcon icon2 = new ImageIcon(img2);
        JLabel label1 = new JLabel(icon1);
        JLabel label2 = new JLabel(icon2);

        frame.add(label1);
        frame.add(label2);

        frame.setLayout(new FlowLayout());
        //frame.getContentPane().add(new JLabel(new ImageIcon(img1)));
        frame.setVisible(true);

        pixels = this.getPixels();

        //this.getColors(pixels);

        this.setupDone = true;
        return frame;
    }

    public int[][] getPixels() {

        int[][] result = new int[y][x];
        byte[] bytePixels = ((DataBufferByte) img1.getRaster().getDataBuffer()).getData();    //I read the pixels byte by byte
        boolean hasAlpha = (img1.getAlphaRaster() != null);

        if (hasAlpha) {
            final int pixelLenght = 4;

            for (int pixel = 0, row = 0, col = 0; pixel < bytePixels.length; pixel += pixelLenght) {
                int argb = 0;
                argb += (((int) bytePixels[pixel] & 0xff) << 24);       // alpha
                argb += ((int) bytePixels[pixel + 1] & 0xff);           // blue
                argb += (((int) bytePixels[pixel + 2] & 0xff) << 8);    // green
                argb += (((int) bytePixels[pixel + 3] & 0xff) << 16);   // red

                result[row][col] = argb;

                col++;
                if (col >= x) {
                    col = 0;
                    row++;
                }
            }
        } else {
            final int pixelLenght = 3;
            for (int pixel = 0, row = 0, col = 0; pixel < bytePixels.length; pixel += pixelLenght) {
                int argb = 0;
                argb += -16777216;                                      // 255 alpha
                argb += ((int) bytePixels[pixel] & 0xff);               // blue
                argb += (((int) bytePixels[pixel + 1] & 0xff) << 8);    // green
                argb += (((int) bytePixels[pixel + 2] & 0xff) << 16);   // red

                result[row][col] = argb;

                col++;
                if (col >= x) {
                    col = 0;
                    row++;
                }
            }
        }

        return result;
    }

    private Color[] getColors(int[] pixels) {
        int x = pixels.length;
        Color[] result = new Color[x];

        for (int i = 0; i < x; i++) {
            result[i] = new Color(pixels[i]);
        }

        return result;
    }

    public void colorSwap(Color sourceColor, Color newColor, int tolerance) {

        int newC = newColor.getRGB();

        for (int row = 0, col = 0; row < y; ) {

            /*if (pixels[row][col] < source + tolerance && pixels[row][col] > source - tolerance) {
                pixels[row][col] = newC;
            }*/

            Color current = new Color(pixels[row][col]);
            if (current.getBlue() < sourceColor.getBlue() + tolerance && current.getBlue() > sourceColor.getBlue() - tolerance &&
                    current.getRed() < sourceColor.getRed() + tolerance && current.getRed() > sourceColor.getRed() - tolerance &&
                    current.getGreen() < sourceColor.getGreen() + tolerance && current.getGreen() > sourceColor.getGreen() - tolerance)
                pixels[row][col] = newC;

            col++;
            if (col >= x) {
                col = 0;
                row++;
            }
        }

        img1.setRGB(0, 0, x, y, matrixToArray(pixels), 0, x);
        frame.repaint();
    }

    private void pixelSort() {

        int[] pixelArray = this.matrixToArray(pixels);

        Color[] colorArray = getColors(pixelArray);
        Color[] oldColors = new Color[colorArray.length];
        System.arraycopy(colorArray, 0, oldColors, 0, oldColors.length);

        //colorArray = ColorSorting.sort(colorArray);
        int[] prevPos= Hilbert.sort(256, colorArray, 16);

        for (int i = 0; i < colorArray.length; i++) {
            pixelArray[i] = colorArray[i].getRGB();
        }


        for (int i = 0; i < pixelArray.length; i++){
            img1.setRGB(prevPos[i]%x, prevPos[i]/x, Color.WHITE.getRGB());
            img2.setRGB(i%x, i/x, pixelArray[i]);
            frame.repaint();

            long before=System.nanoTime();
            while(before+50000>System.nanoTime());

        }


        /*for (int i = 0; i < pixelArray.length; i++) {

            int toTake=pixelArray[i];
            int j=0;
            while(oldColors[j].getRGB()!=toTake)
                j++;
            img1.setRGB(j%x, j/x, Color.WHITE.getRGB());
            img2.setRGB(i%x, i/x, pixelArray[i]);
            oldColors[j]=Color.WHITE;
            System.out.println(i + " pixels moved");
            frame.repaint();
        }*/

        //img2.setRGB(0, 0, x, y, pixelArray, 0, x);
        frame.repaint();
    }

    private int[] matrixToArray(int[][] pixelMatrix) {
        int x = pixelMatrix[0].length;
        int y = pixelMatrix.length;

        int[] result = new int[x * y];

        for (int row = 0, col = 0, i = 0; row < y; i++) {
            result[i] = pixelMatrix[row][col];

            col++;
            if (col >= x) {
                col = 0;
                row++;
            }
        }
        return result;
    }


    public static void main(String[] args) {

        PixelSorting instance = new PixelSorting();
        try {
            instance.setup("trekking.jpg");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load image \n");
            return;
        }

       /* for (int i = 0; i < 1000; i++) {
            instance.colorSwap(new Color(207, 207, 207, 255), new Color(67, 255, 90, 255), i);
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/
        instance.pixelSort();
    }
}