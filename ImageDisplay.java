// CSCI-576: Assg-1 
// Date: Feb 14, 2025
// Author: Ameya Deshmukh

// imports 
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;

public class ImageDisplay {

    // original image dimensions
    static final int ORIGINAL_WIDTH = 512;
    static final int ORIGINAL_HEIGHT = 512;
    
    // read the image of given width and height at the given imgPath into the provided BufferedImage
    private static boolean readImageRGB(String fileName, int width, int height, BufferedImage img)
    {
        try 
        {
            int numPixels = width * height;
            int frameLength = numPixels * 3;  // in this case there are 3 channels
            
            FileInputStream fis = new FileInputStream(fileName);
            byte[] bytes = new byte[frameLength];
            
            int bytesRead = fis.read(bytes);
            
            fis.close();
            
            if (bytesRead != frameLength) 
            {
                System.out.println("File size does not match expected " + frameLength + " bytes.");
                return false;
            }
            // for each pixel, read RGB values from the 3 blocks respectively
            for (int y = 0; y < height; y++)
            {
                for (int x = 0; x < width; x++)
                {
                    int index = y * width + x;
                    
                    int r = bytes[index] & 0xFF;
                    int g = bytes[index + numPixels] & 0xFF;
                    int b = bytes[index + 2 * numPixels] & 0xFF;
                    
                    int rgb = (0xFF << 24) | (r << 16) | (g << 8) | b;
                    
                    img.setRGB(x, y, rgb);
                }
            }
            
            return true;

        } 
            catch(IOException e) 
        {
            e.printStackTrace();
            return false;
        }
    }
    
    // scale the image using a 3×3 averaging filter also called anti-aliasing.
    private static BufferedImage processImage(BufferedImage in, double scale, int Q, int mode, boolean applyQuantization) 
    {
        int newWidth = (int)(ORIGINAL_WIDTH * scale);
        int newHeight = (int)(ORIGINAL_HEIGHT * scale);
        
        BufferedImage out = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        
        for (int outY = 0; outY < newHeight; outY++) 
        {
            for (int outX = 0; outX < newWidth; outX++) 
            {
                // mapping the output pixel to the original coordinates
                double origX = outX / scale;
                double origY = outY / scale;
                
                int cntX = (int)Math.round(origX);
                int cntY = (int)Math.round(origY);
                
                cntX = Math.min(Math.max(cntX, 0), ORIGINAL_WIDTH - 1);
                cntY = Math.min(Math.max(cntY, 0), ORIGINAL_HEIGHT - 1);
                
                int totalR = 0;
                int totalG = 0;
                int totalB = 0;
                int count = 0;
                
                // applying 3×3 averaging box filter
                for (int y = cntY - 1; y <= cntY + 1; y++) 
                {
                    for (int x = cntX - 1; x <= cntX + 1; x++) 
                    {
                        if (x >= 0 && x < ORIGINAL_WIDTH && y >= 0 && y < ORIGINAL_HEIGHT) 
                        {
                            int rgb = in.getRGB(x, y);
                            int r = (rgb >> 16) & 0xFF;
                            int g = (rgb >> 8) & 0xFF;
                            int b = rgb & 0xFF;
                            totalR += r;
                            totalG += g;
                            totalB += b;
                            count++;
                        }
                    }
                }

                int avgR = (int)Math.round(totalR / (double)count);
                int avgG = (int)Math.round(totalG / (double)count);
                int avgB = (int)Math.round(totalB / (double)count);
                
                // applying quantization if requested
                if (applyQuantization && Q < 8) 
                {
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
    
    // quantization
    private static int quantize(int value, int Q, int mode) 
    {
        // uniform quantization
        if (mode == -1) 
        {
            int levels = 1 << Q;
            double interval = 256.0 / levels;
            int idx = (int)(value / interval);
            if (idx >= levels) 
            {
                idx = levels - 1;
            }
            double lower = idx * interval;
            double upper = (idx + 1) * interval - 1;
            
            return (int)Math.round((lower + upper) / 2.0);
        }
        // logarithmic quantization when a pivot is given
        else 
        {
            int pivot = mode;
            int levels = 1 << Q;
            if (pivot == 0) 
            {
                // logarithmic mapping
                double logMax = Math.log(256);
                int idx = 0;

                for (int i = 1; i <= levels; i++) 
                {
                    double boundary = Math.exp(logMax * i / levels) - 1;
                    
                    if (value <= boundary) 
                    {
                        idx = i - 1;
                        break;
                    }

                    if (i == levels) idx = levels - 1;
                }
                
                double lwrBound = (idx == 0) ? 0 : Math.exp(logMax * idx / levels) - 1;
                double upprBound = Math.exp(logMax * (idx + 1) / levels) - 1;
                return (int)Math.round((lwrBound + upprBound) / 2.0);
            }
            // piecewise mapping
            else 
            {
                int lwrLvls = (int)Math.round(levels * ((pivot + 1) / 256.0));
                if (lwrLvls < 1)
                {
                    lwrLvls = 1;
                }
                if (lwrLvls > levels - 1) 
                {
                    lwrLvls = levels - 1;
                }

                int upprLvls = levels - lwrLvls;
                
                if (value <= pivot) 
                {
                    double interval = (pivot + 1) / (double)lwrLvls;
                    int idx = (int)(value / interval);
                    if (idx >= lwrLvls) idx = lwrLvls - 1;
                    double lwrBound = idx * interval;
                    double upprBound = (idx + 1) * interval - 1;
                    
                    return (int)Math.round((lwrBound + upprBound) / 2.0);
                } 
                else 
                {
                    double interval = (255 - pivot) / (double)upprLvls;
                    int idx = (int)((value - pivot - 1) / interval);
                    if (idx >= upprLvls) idx = upprLvls - 1;
                    double lwrBound = pivot + 1 + idx * interval;
                    double upprBound = pivot + 1 + (idx + 1) * interval - 1;
                    
                    return (int)Math.round((lwrBound + upprBound) / 2.0);
                }
            }
        }
    }
    
    // computing optimal pivot value 
    private static int computePivot(BufferedImage img) 
    {
        long sum = 0;
        int count = 0;

        for (int y = 0; y < img.getHeight(); y++)
        {
            for (int x = 0; x < img.getWidth(); x++)
            {
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
    
    // requantizing image with pivot
    private static void quantizeImage(BufferedImage img, int Q, int pivot) 
    {
        for (int y = 0; y < img.getHeight(); y++)
        {
            for (int x = 0; x < img.getWidth(); x++)
            {
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
    
    // displaying buffer image in jframe with single jlabel
    private static void showImage(BufferedImage img) 
    {
        JFrame frame = new JFrame("Processed Image");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel label = new JLabel(new ImageIcon(img));
        frame.getContentPane().add(label, BorderLayout.CENTER);

        frame.pack();
        frame.setVisible(true);
    }

    // main function
    public static void main(String[] args) 
    {
        if (args.length < 3 || args.length > 4) 
        {
            System.out.println("Usage: java ImageDisplay <image.rgb> <scale> <Q> [<mode>]");
            System.out.println("If mode is omitted, extra credit is assumed (automatic pivot for logarithmic quantization).");
            System.exit(0);
        }
        
        String fileName = args[0];
        double scale = Double.parseDouble(args[1]);
        int Q = Integer.parseInt(args[2]);
        
        boolean extraCredit = false;
        // default for uniform quantization
        int mode = -1;
        // if mode is not provided --> extra credit is processed
        if (args.length == 3) 
        {
            extraCredit = true; 
        } 
        else 
        {
            mode = Integer.parseInt(args[3]);
        }
        
        // reading the original image
        BufferedImage originalImg = new BufferedImage(ORIGINAL_WIDTH, ORIGINAL_HEIGHT, BufferedImage.TYPE_INT_RGB);

        if (!readImageRGB(fileName, ORIGINAL_WIDTH, ORIGINAL_HEIGHT, originalImg)) 
        {
            System.out.println("Error reading image file.");
            System.exit(1);
        }
        
        // scaling and filtering
        boolean applyQuantization = !extraCredit; // if extracredit, then do quantization after doing scaling
        BufferedImage processedImg = processImage(originalImg, scale, Q, mode, applyQuantization);
        
        // extra credit
        if (extraCredit && Q < 8) 
        {
            int computedPivot = computePivot(processedImg);
            System.out.println("Computed optimal pivot: " + computedPivot);

            quantizeImage(processedImg, Q, computedPivot);
        }
        
        // displaying final image
        showImage(processedImg);
    }
}
