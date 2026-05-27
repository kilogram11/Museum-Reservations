package com.museum.service.impl;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class RelicOnnxService {

    private OrtEnvironment env;
    private OrtSession session;

    // MobileNetV2 requires 224x224 input
    private static final int INPUT_WIDTH = 224;
    private static final int INPUT_HEIGHT = 224;

    // ImageNet normalization constants
    private static final float[] MEAN = { 0.485f, 0.456f, 0.406f };
    private static final float[] STD = { 0.229f, 0.224f, 0.225f };

    @PostConstruct
    public void init() {
        try {
            // Initialize ONNX Runtime environment
            env = OrtEnvironment.getEnvironment();
            // Load model from resources
            ClassPathResource resource = new ClassPathResource("model/relic_model.onnx");
            try (InputStream is = resource.getInputStream()) {
                byte[] modelBytes = is.readAllBytes();
                session = env.createSession(modelBytes, new OrtSession.SessionOptions());
            }
        } catch (Exception e) {
            System.err.println("Failed to load ONNX model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Perform inference on an image input stream.
     * 
     * @param imageStream Input stream of the uploaded image
     * @return Recognized class ID (0-4), or -1 if failed
     */
    public int predict(InputStream imageStream) {
        if (session == null)
            return -1;

        try {
            // 1. Load and Resize Image
            BufferedImage originalImage = ImageIO.read(imageStream);
            if (originalImage == null)
                return -1;

            BufferedImage resizedImage = new BufferedImage(INPUT_WIDTH, INPUT_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, INPUT_WIDTH, INPUT_HEIGHT, null);
            g.dispose();

            // 2. Preprocess: HWC -> CHW, Normalize
            // Shape: [1, 3, 224, 224]
            int pixelsCount = INPUT_WIDTH * INPUT_HEIGHT;
            FloatBuffer floatBuffer = FloatBuffer.allocate(1 * 3 * pixelsCount);

            for (int c = 0; c < 3; c++) { // Channel loop: R, G, B
                for (int h = 0; h < INPUT_HEIGHT; h++) {
                    for (int w = 0; w < INPUT_WIDTH; w++) {
                        int pixel = resizedImage.getRGB(w, h);

                        // Extract channel value (0-255)
                        float val = 0;
                        if (c == 0)
                            val = (pixel >> 16) & 0xFF; // Red
                        else if (c == 1)
                            val = (pixel >> 8) & 0xFF; // Green
                        else
                            val = pixel & 0xFF; // Blue

                        // Normalize: (value/255 - mean) / std
                        val = ((val / 255.0f) - MEAN[c]) / STD[c];
                        floatBuffer.put(val);
                    }
                }
            }
            floatBuffer.flip();

            // 3. Create Tensor
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, floatBuffer,
                    new long[] { 1, 3, INPUT_WIDTH, INPUT_HEIGHT });

            // 4. Run Inference
            // "input" is the input name defined during export
            OrtSession.Result result = session.run(Collections.singletonMap("input", inputTensor));

            // 5. Post-process
            // Output shape: [1, 5] (probabilities/logits)
            float[][] output = (float[][]) result.get(0).getValue();
            float[] probs = output[0];

            int maxIndex = 0;
            float maxVal = probs[0];
            for (int i = 1; i < probs.length; i++) {
                if (probs[i] > maxVal) {
                    maxVal = probs[i];
                    maxIndex = i;
                }
            }

            // Close resources for this run
            inputTensor.close();
            result.close();

            return maxIndex;

        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
