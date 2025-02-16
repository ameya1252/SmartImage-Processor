import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageProcessor {

    // The original image dimensions as specified.
    static final int ORIGINAL_WIDTH = 512;
    static final int ORIGINAL_HEIGHT = 512;

    public static void main(String[] args) {
        // Expect either 3 or 4 arguments.
        //  - 1st: image file name (planar 512x512, 24-bit RGB)
        //  - 2nd: scale (0.0 < scale <= 1.0)
        //  - 3rd: Q (number of bits per channel, 1 <= Q <= 8)
        //  - 4th (optional): mode for quantization.
        //       If mode == -1, uniform quantization is used.
        //       Otherwise, mode (an integer between 0 and 255) is used as the pivot for logarithmic quantization.
        // If the 4th parameter is omitted, we assume extra credit: logarithmic quantization using an automatically computed pivot.
        if (args.length < 3 || args.length > 4) {
            System.out.println("Usage: java ImageProcessor <image.rgb> <scale> <Q> [<mode>]");
            System.out.println("If mode is omitted, extra credit is assumed (automatic pivot for logarithmic quantization).");
            System.exit(0);
        }
        
        String fileName = args[0];
        double scale = Double.parseDouble(args[1]);
        int Q = Integer.parseInt(args[2]);
        
        boolean extraCredit = false;
        int mode = -1; // Default: -1 indicates uniform quantization.
        if (args.length == 3) {
            extraCredit = true;  // No mode provided: extra credit is enabled.
        } else {
            mode = Integer.parseInt(args[3]);
        }
        
        // Read the original image using the provided planar order.
        BufferedImage originalImg = new BufferedImage(ORIGINAL_WIDTH, ORIGINAL_HEIGHT, BufferedImage.TYPE_INT_RGB);
        if (!readPlanarRGB(fileName, ORIGINAL_WIDTH, ORIGINAL_HEIGHT, originalImg)) {
            System.out.println("Error reading image file.");
            System.exit(1);
        }
        
        // Process the image: scaling and filtering.
        // If extraCredit is true, we delay quantization until after scaling.
        boolean applyQuantization = !extraCredit;
        BufferedImage processedImg = processImage(originalImg, scale, Q, mode, applyQuantization);
        
        // Extra Credit: If no mode parameter was provided and Q < 8,
        // compute an optimal pivot from the processed image and then apply logarithmic quantization.
        if (extraCredit && Q < 8) {
            int computedPivot = computePivot(processedImg);
            System.out.println("Computed optimal pivot: " + computedPivot);
            quantizeImage(processedImg, Q, computedPivot);
        }
        
        // Display the final processed image in a single window.
        showImage(processedImg);
    }
    
    /**
     * Reads a planar .rgb file.
     * The file format: first all red channel values in scan-line order,
     * then green, then blue.
     */
    private static boolean readPlanarRGB(String fileName, int width, int height, BufferedImage img) {
        try {
            int totalPixels = width * height;
            int frameLength = totalPixels * 3;  // 3 channels.
            FileInputStream fis = new FileInputStream(fileName);
            byte[] bytes = new byte[frameLength];
            int bytesRead = fis.read(bytes);
            fis.close();
            if (bytesRead != frameLength) {
                System.out.println("File size does not match expected " + frameLength + " bytes.");
                return false;
            }
            // For each pixel, read the red value from the first block,
            // green from the second, and blue from the third.
            for (int y = 0; y < height; y++){
                for (int x = 0; x < width; x++){
                    int index = y * width + x;
                    int r = bytes[index] & 0xFF;
                    int g = bytes[index + totalPixels] & 0xFF;
                    int b = bytes[index + 2 * totalPixels] & 0xFF;
                    int rgb = (0xFF << 24) | (r << 16) | (g << 8) | b;
                    img.setRGB(x, y, rgb);
                }
            }
            return true;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Scales the image using a 3×3 averaging filter (anti-aliasing).
     * For each output pixel, the corresponding position in the original image is computed,
     * a 3×3 neighborhood is averaged (with Math.round() for nearest integer), and
     * if applyQuantization is true and Q < 8, uniform or logarithmic quantization is applied.
     */
    private static BufferedImage processImage(BufferedImage in, double scale, int Q, int mode, boolean applyQuantization) {
        int newWidth = (int)(ORIGINAL_WIDTH * scale);
        int newHeight = (int)(ORIGINAL_HEIGHT * scale);
        BufferedImage out = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        
        for (int outY = 0; outY < newHeight; outY++) {
            for (int outX = 0; outX < newWidth; outX++) {
                // Map the output pixel back to original coordinates.
                double origX = outX / scale;
                double origY = outY / scale;
                int centerX = (int)Math.round(origX);
                int centerY = (int)Math.round(origY);
                centerX = Math.min(Math.max(centerX, 0), ORIGINAL_WIDTH - 1);
                centerY = Math.min(Math.max(centerY, 0), ORIGINAL_HEIGHT - 1);
                
                int sumR = 0, sumG = 0, sumB = 0;
                int count = 0;
                // Apply a 3×3 averaging (box filter).
                for (int y = centerY - 1; y <= centerY + 1; y++) {
                    for (int x = centerX - 1; x <= centerX + 1; x++) {
                        if (x >= 0 && x < ORIGINAL_WIDTH && y >= 0 && y < ORIGINAL_HEIGHT) {
                            int rgb = in.getRGB(x, y);
                            int r = (rgb >> 16) & 0xFF;
                            int g = (rgb >> 8) & 0xFF;
                            int b = rgb & 0xFF;
                            sumR += r;
                            sumG += g;
                            sumB += b;
                            count++;
                        }
                    }
                }
                int avgR = (int)Math.round(sumR / (double)count);
                int avgG = (int)Math.round(sumG / (double)count);
                int avgB = (int)Math.round(sumB / (double)count);
                
                // Apply quantization here only if requested.
                if (applyQuantization && Q < 8) {
                    avgR = quantize(avgR, Q, mode);
                    avgG = quantize(avgG, Q, mode);
                    avgB = quantize(avgB, Q, mode);
                }
                
                int newRGB = (0xFF << 24) | (avgR << 16) | (avgG << 8) | avgB;
                out.setRGB(outX, outY, newRGB);
            }
        }
        return out;
    }
    
    /**
     * Quantizes a single channel value (0–255) to Q bits.
     * If mode == -1, uniform quantization is used.
     * Otherwise, mode is taken as the pivot for logarithmic quantization.
     */
    private static int quantize(int value, int Q, int mode) {
        if (mode == -1) { // Uniform quantization.
            int levels = 1 << Q; // 2^Q levels.
            double interval = 256.0 / levels;
            int idx = (int)(value / interval);
            if (idx >= levels) idx = levels - 1;
            double lower = idx * interval;
            double upper = (idx + 1) * interval - 1;
            return (int)Math.round((lower + upper) / 2.0);
        } else { // Logarithmic quantization using provided pivot.
            int pivot = mode;
            int levels = 1 << Q;
            if (pivot == 0) {
                // Pure logarithmic mapping.
                double logMax = Math.log(256);
                int idx = 0;
                for (int i = 1; i <= levels; i++) {
                    double boundary = Math.exp(logMax * i / levels) - 1;
                    if (value <= boundary) {
                        idx = i - 1;
                        break;
                    }
                    if (i == levels) idx = levels - 1;
                }
                double lowerBound = (idx == 0) ? 0 : Math.exp(logMax * idx / levels) - 1;
                double upperBound = Math.exp(logMax * (idx + 1) / levels) - 1;
                return (int)Math.round((lowerBound + upperBound) / 2.0);
            } else {
                // Piecewise mapping: allocate some intervals to [0, pivot] and the rest to [pivot+1, 255].
                int lowerLevels = (int)Math.round(levels * ((pivot + 1) / 256.0));
                if (lowerLevels < 1) lowerLevels = 1;
                if (lowerLevels > levels - 1) lowerLevels = levels - 1;
                int upperLevels = levels - lowerLevels;
                if (value <= pivot) {
                    double interval = (pivot + 1) / (double)lowerLevels;
                    int idx = (int)(value / interval);
                    if (idx >= lowerLevels) idx = lowerLevels - 1;
                    double lowerBound = idx * interval;
                    double upperBound = (idx + 1) * interval - 1;
                    return (int)Math.round((lowerBound + upperBound) / 2.0);
                } else {
                    double interval = (255 - pivot) / (double)upperLevels;
                    int idx = (int)((value - pivot - 1) / interval);
                    if (idx >= upperLevels) idx = upperLevels - 1;
                    double lowerBound = pivot + 1 + idx * interval;
                    double upperBound = pivot + 1 + (idx + 1) * interval - 1;
                    return (int)Math.round((lowerBound + upperBound) / 2.0);
                }
            }
        }
    }
    
    /**
     * Computes an optimal pivot value from the processed image.
     * (This simple implementation computes the average intensity over all pixels.)
     */
    private static int computePivot(BufferedImage img) {
        long sum = 0;
        int count = 0;
        for (int y = 0; y < img.getHeight(); y++){
            for (int x = 0; x < img.getWidth(); x++){
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int avg = (r + g + b) / 3;
                sum += avg;
                count++;
            }
        }
        return (int)Math.round(sum / (double)count);
    }
    
    /**
     * Re-quantizes the image using logarithmic quantization with the specified pivot.
     */
    private static void quantizeImage(BufferedImage img, int Q, int pivot) {
        for (int y = 0; y < img.getHeight(); y++){
            for (int x = 0; x < img.getWidth(); x++){
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int newR = quantize(r, Q, pivot);
                int newG = quantize(g, Q, pivot);
                int newB = quantize(b, Q, pivot);
                int newRGB = (0xFF << 24) | (newR << 16) | (newG << 8) | newB;
                img.setRGB(x, y, newRGB);
            }
        }
    }
    
    /**
     * Displays the provided BufferedImage in a JFrame with a single JLabel.
     */
    private static void showImage(BufferedImage img) {
        JFrame frame = new JFrame("Processed Image");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel label = new JLabel(new ImageIcon(img));
        frame.getContentPane().add(label, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }
}
