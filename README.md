# SmartImage Processor: Resampling, Quantization, and Filtering

This project is my personal implementation of image processing techniques including resampling, quantization, and filtering, designed to efficiently handle image transformations with high quality outputs.

---

## Overview
SmartImage Processor is a C++ project that resizes images, performs quantization with both uniform and logarithmic methods, and applies a 3x3 averaging filter for image smoothing. This project showcases my technical expertise in multimedia systems and image processing.

---

## Features
- **Image Resizing:** Scales images with precision while preserving details.
- **Quantization:**
  - **Uniform Quantization:** Divides pixel values into equal ranges.
  - **Logarithmic Quantization:** Non-uniform quantization based on a dynamic pivot.
- **Filtering:** Smoothens images with an averaging filter.
- **Command Line Interface:** User-friendly interface for processing images.
- **Optimal Pivot Detection:** Automatically determines the best pivot for logarithmic quantization.

---

## Usage
Compile and run from the command line as:
```sh
SmartImageProcessor.exe <image_file.rgb> <scale_factor> <quantization_bits> <mode>
