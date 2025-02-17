# CSCI-576 Assignment 1: Image Scaling and Quantization

### Author:
Ameya Deshmukh  
Date: February 14, 2025  

---

### Overview:
This project performs image scaling with anti-aliasing (3×3 averaging filter) and quantization (uniform, logarithmic, and piecewise). It also implements extra credit by computing an optimal pivot for logarithmic quantization when no mode is provided.

---

### Files:
- ImageDisplay.java: Main source code for reading, processing (scaling, filtering, quantization), and displaying images.

---

### Compilation Instructions:
To compile the code, use:
javac ImageDisplay.java

---

### Execution Instructions:
To run the program, use:
java ImageDisplay <image.rgb> <scale> <Q> [<mode>]

<image.rgb>: Path to the raw image file (e.g., Lenna_512_512.rgb).  
<scale>: Scaling factor (e.g., 1.0 for original size, 0.5 for half size, etc.).  
<Q>: Bit depth for quantization (e.g., 8, 4, 2, etc.).  
[<mode>] (Optional):  
- -1 for uniform quantization.  
- 0 for logarithmic quantization with automatic pivot (extra credit).  
- Any value between 1 and 255 for piecewise quantization using the given pivot.

---

### Execution Examples:
java ImageDisplay Lenna_512_512.rgb 1.0 8 0  
This reads the image Lenna_512_512.rgb, scales it by 1.0, applies 8-bit quantization using logarithmic quantization with pivot 0.

java ImageDisplay Lenna_512_512.rgb 0.5 4  
This scales the image by 0.5, applies 4-bit quantization with the extra credit feature, computing an optimal pivot automatically.

---

### Extra Credit:
- Completed: The extra credit feature is implemented.  
  When the <mode> argument is omitted, the program computes an optimal pivot automatically for logarithmic quantization after scaling the image, as specified in the assignment.

---

### Note:
- Ensure that the input image is a raw .rgb file with dimensions 512×512.
- Display output is shown in a new window using JFrame.
