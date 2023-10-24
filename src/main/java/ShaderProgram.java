import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    public final int programId;
    public int vertexShaderId;
    public int fragmentShaderId;

    public ShaderProgram() throws Exception {
        this.programId = glCreateProgram();
        if (this.programId == 0) {
            throw new Exception("Failed to create shader program");
        }
    }

    public void createVertexShader(String shaderCode) throws Exception {
        this.vertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
    }

    public void createFragmentShader(String shaderCode) throws Exception {
        this.fragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
    }

    protected int createShader(String shaderCode, int shaderType) throws Exception {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Type: " + shaderType);
        }

        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);

        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            throw new Exception("Error compiling shader code: " + glGetShaderInfoLog(shaderId));
        }

        glAttachShader(this.programId, shaderId);

        return shaderId;
    }

    public void link() {
        glLinkProgram(this.programId);
        if (glGetProgrami(this.programId, GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Error linking shader code: " + glGetProgramInfoLog(this.programId));
        }

        if (this.vertexShaderId != 0) {
            glDetachShader(this.programId, this.vertexShaderId);
        }

        if (this.fragmentShaderId != 0) {
            glDetachShader(this.programId, this.fragmentShaderId);
        }

        glValidateProgram(this.programId);
        if (glGetProgrami(this.programId, GL_VALIDATE_STATUS) == 0) {
            System.err.println("Warning validating shader code: " + glGetProgramInfoLog(this.programId));
        }
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }
}
