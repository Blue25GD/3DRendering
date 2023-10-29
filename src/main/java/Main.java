import de.matthiasmann.twl.utils.PNGDecoder;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.io.IOException;
import java.nio.*;
import java.util.ArrayList;
import java.util.Arrays;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    // The window handle
    private long window;
    private ShaderProgram shaderProgram;
    private int VAO;

    public static int WIDTH = 1280;
    public static int HEIGHT = 720;
    int resolutionUniform;

    public int frames = 0;

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");
        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
//        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);

        glfwWindowHint(GLFW_DEPTH_BITS, GL_TRUE);

        // Create the window
        window = glfwCreateWindow(WIDTH, HEIGHT, "Hello World!", NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        glfwSetWindowSizeCallback(window, (window, width, height) -> {
            WIDTH = width;
            HEIGHT = height;

            glUniform2f(resolutionUniform, WIDTH, HEIGHT);
        });

        // Make the window visible
        glfwShowWindow(window);

        GL.createCapabilities();

        glEnable(GL_TEXTURE_2D);

        try {
            VAO = glGenVertexArrays();
            glBindVertexArray(VAO);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.createVertexShader(Util.readFile(Util.getResourceAsStream("src/main/java/vertexShader.glsl")));
            shaderProgram.createFragmentShader(Util.readFile(Util.getResourceAsStream("src/main/java/fragmentShader.glsl")));
            shaderProgram.link();

            this.shaderProgram = shaderProgram;
            resolutionUniform = glGetUniformLocation(this.shaderProgram.programId, "resolution");
        } catch (Exception e) {
            e.printStackTrace();
        }

        PNGDecoder decoder = null;
        ByteBuffer image = null;
        try {
            decoder = new PNGDecoder(
                    Util.getResourceAsStream("src/main/java/textures.png"));
            image = ByteBuffer.allocateDirect(
                    4 * decoder.getWidth() * decoder.getHeight());
            decoder.decode(image, decoder.getWidth() * 4, PNGDecoder.Format.RGBA);
            image.flip();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, decoder.getWidth(), decoder.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, image);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);


        // Bind to textureData uniform
        int textureUniform = glGetUniformLocation(this.shaderProgram.programId, "textureData");
        glUniform1i(textureUniform, 0);
    }

    private void loop() {
        GL.createCapabilities();
        glDepthFunc(GL_LESS);
        glEnable(GL_DEPTH_TEST);

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);


        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            render();

            glfwSwapBuffers(window);

            glfwPollEvents();

            frames++;
        }
    }

    private void render() {
        try {
            float cubeSize = 0.3f;

            float[] front = new float[]{
                    -cubeSize, -cubeSize, cubeSize,  // 0
                    cubeSize, -cubeSize, cubeSize,   // 1
                    cubeSize, cubeSize, cubeSize,    // 2
                    -cubeSize, cubeSize, cubeSize,   // 3
            };

            float[] back = new float[]{
                    -cubeSize, -cubeSize, -cubeSize,  // 0
                    cubeSize, -cubeSize, -cubeSize,   // 1
                    cubeSize, cubeSize, -cubeSize,    // 2
                    -cubeSize, cubeSize, -cubeSize,   // 3
            };

            float[] left = new float[]{
                    -cubeSize, -cubeSize, -cubeSize,  // 0
                    -cubeSize, -cubeSize, cubeSize,   // 1
                    -cubeSize, cubeSize, cubeSize,    // 2
                    -cubeSize, cubeSize, -cubeSize,   // 3
            };

            float[] right = new float[]{
                    cubeSize, -cubeSize, -cubeSize,  // 0
                    cubeSize, -cubeSize, cubeSize,   // 1
                    cubeSize, cubeSize, cubeSize,    // 2
                    cubeSize, cubeSize, -cubeSize,   // 3
            };

            float[] top = new float[]{
                    -cubeSize, cubeSize, -cubeSize,  // 0
                    cubeSize, cubeSize, -cubeSize,   // 1
                    cubeSize, cubeSize, cubeSize,    // 2
                    -cubeSize, cubeSize, cubeSize,   // 3
            };

            float[] bottom = new float[]{
                    -cubeSize, -cubeSize, -cubeSize,  // 0
                    cubeSize, -cubeSize, -cubeSize,   // 1
                    cubeSize, -cubeSize, cubeSize,    // 2
                    -cubeSize, -cubeSize, cubeSize,   // 3
            };

            int[] indices = new int[]{  // note that we start from 0!
//                    0, 1, 2,   // first triangle
                    0, 3, 2,   // first
                    0, 2, 1,   // second
            };

            float[] color1 = new float[]{
                    1f, 0f, 0f, 1f,
                    1f, 1f, 0f, 1f,
                    1f, 0f, 1f, 1f,
                    1f, 0f, 0f, 1f,
            };

            float[] color2 = new float[]{
                    1f, 0f, 0f, 1f,
                    0f, 1f, 1f, 1f,
                    1f, 0f, 1f, 1f,
                    1f, 1f, 0f, 1f,
            };

            int speed = 4;

            double[] rotationAngles = new double[]{
                    Math.toRadians(30), 0f, 0f,
                    Math.toRadians(30), 0f, 0f,
                    Math.toRadians(30), 0f, 0f,
                    Math.toRadians(30), 0f, 0f,
            };

            float[] rotationCenter = new float[]{
                    0f, 0f, 0f,
                    0f, 0f, 0f,
                    0f, 0f, 0f,
                    0f, 0f, 0f,
            };

            glUniform2f(resolutionUniform, WIDTH, HEIGHT);

            float[] textureCoords1 = getTextureCoordsOnImage(0, 0, 16, 16, 256, 256);

            float[] textureCoords2 = getTextureCoordsOnImage(16, 0, 16, 16, 256, 256);
            textureCoords2 = flip2DCoordinateArray(textureCoords2);

            float[] textureCoords3 = getTextureCoordsOnImage(32, 0, 16, 16, 256, 256);

            System.out.println("========================================================");
            System.out.println(Arrays.toString(textureCoords1));
            System.out.println(Arrays.toString(textureCoords2));
            System.out.println(Arrays.toString(textureCoords3));
            System.out.println("========================================================");

            drawShape(front, indices, color1, rotationAngles, rotationCenter, textureCoords2);
            drawShape(back, indices, color2, rotationAngles, rotationCenter, textureCoords2);
            drawShape(left, indices, color2, rotationAngles, rotationCenter, textureCoords2);
            drawShape(right, indices, color2, rotationAngles, rotationCenter, textureCoords2);
            drawShape(top, indices, color2, rotationAngles, rotationCenter, textureCoords1);
            drawShape(bottom, indices, color2, rotationAngles, rotationCenter, textureCoords3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void drawShape(float[] vertices, int[] indices, float[] color, double[] rotationAngles, float[] rotationCenter, float[] textureCoords) {
        int VBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, VBO);

        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glBindVertexArray(VAO);

        int EBO = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO);

        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        int colorVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, colorVBO);

        glBufferData(GL_ARRAY_BUFFER, color, GL_STATIC_DRAW);

        glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);

        int rotationAnglesVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, rotationAnglesVBO);

        glBufferData(GL_ARRAY_BUFFER, rotationAngles, GL_STATIC_DRAW);

        glVertexAttribPointer(2, 3, GL_DOUBLE, false, 0, 0);
        glEnableVertexAttribArray(2);

        int rotationCenterVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, rotationCenterVBO);

        glBufferData(GL_ARRAY_BUFFER, rotationCenter, GL_STATIC_DRAW);

        glVertexAttribPointer(3, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(3);

        int textureCoordsVBO = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, textureCoordsVBO);

        glBufferData(GL_ARRAY_BUFFER, textureCoords, GL_STATIC_DRAW);

        glVertexAttribPointer(4, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(4);

        this.shaderProgram.bind();

//        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        glDeleteBuffers(VBO);
        glDeleteBuffers(EBO);
        glDeleteBuffers(colorVBO);
        glDeleteBuffers(rotationAnglesVBO);
        glDeleteBuffers(rotationCenterVBO);
        glDeleteBuffers(textureCoordsVBO);

    }

    public static void main(String[] args) {
        new Main().run();
    }

    public float transformToOpenGLImageCoordinates(int pixelCoords, int imageWidth) {
        // p * 1 / w
        return (float) pixelCoords / imageWidth;
    }

    public float[] getTextureCoordsOnImage(int x, int y, int textureW, int textureH, int imgW, int imgH) {
        float[] coords = new float[]{
                transformToOpenGLImageCoordinates(x, imgW), transformToOpenGLImageCoordinates(y, imgH),
                transformToOpenGLImageCoordinates(x + textureW, imgW), transformToOpenGLImageCoordinates(y, imgH),
                transformToOpenGLImageCoordinates(x + textureW, imgW), transformToOpenGLImageCoordinates(y + textureH, imgH),
                transformToOpenGLImageCoordinates(x, imgW), transformToOpenGLImageCoordinates(y + textureH, imgH),
        };
        return coords;
    }

    public float[] flip2DCoordinateArray(float[] inputArray) {
        if (inputArray == null) {
            throw new IllegalArgumentException("Input array cannot be null.");
        }

        int length = inputArray.length;

        if (length % 2 != 0) {
            throw new IllegalArgumentException("Input array length must be even.");
        }

        float[] reversedArray = new float[length];

        for (int i = 0; i < length / 2; i++) {
            int j = length - 2 - i * 2;
            reversedArray[i * 2] = inputArray[j];
            reversedArray[i * 2 + 1] = inputArray[j + 1];
        }

        return reversedArray;
    }

}