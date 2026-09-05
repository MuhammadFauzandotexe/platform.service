package mdro.platform.service.logging;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BoundedResponseWrapper extends HttpServletResponseWrapper {

    private final int maxBodySize;
    private final LimitedOutputStream capture;
    private ServletOutputStream outputStream;
    private PrintWriter writer;

    public BoundedResponseWrapper(HttpServletResponse response, int maxBodySize) {
        super(response);
        this.maxBodySize = maxBodySize;
        this.capture = new LimitedOutputStream(maxBodySize);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called");
        }
        if (outputStream == null) {
            ServletOutputStream delegate = super.getOutputStream();
            outputStream = new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return delegate.isReady();
                }

                @Override
                public void setWriteListener(WriteListener listener) {
                    delegate.setWriteListener(listener);
                }

                @Override
                public void write(int value) throws IOException {
                    delegate.write(value);
                    capture.write(value);
                }

                @Override
                public void write(byte[] bytes, int offset, int length) throws IOException {
                    delegate.write(bytes, offset, length);
                    capture.write(bytes, offset, length);
                }

                @Override
                public void flush() throws IOException {
                    delegate.flush();
                }

                @Override
                public void close() throws IOException {
                    delegate.close();
                }
            };
        }
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (outputStream != null) {
            throw new IllegalStateException("getOutputStream() has already been called");
        }
        if (writer == null) {
            Charset charset = Charset.forName(getCharacterEncoding() == null
                    ? StandardCharsets.UTF_8.name() : getCharacterEncoding());
            Writer delegate = new java.io.OutputStreamWriter(super.getOutputStream(), charset);
            writer = new PrintWriter(new Writer() {
                @Override
                public void write(char[] chars, int offset, int length) throws IOException {
                    delegate.write(chars, offset, length);
                    capture.write(new String(chars, offset, length).getBytes(charset));
                }

                @Override
                public void flush() throws IOException {
                    delegate.flush();
                }

                @Override
                public void close() throws IOException {
                    delegate.close();
                }
            });
        }
        return writer;
    }

    @Override
    public void resetBuffer() {
        super.resetBuffer();
        capture.reset();
    }

    public byte[] getCapturedBody() {
        return capture.toByteArray();
    }

    public boolean isBodyTruncated() {
        return capture.isTruncated();
    }

    private static final class LimitedOutputStream extends OutputStream {
        private final int maxSize;
        private final java.io.ByteArrayOutputStream delegate = new java.io.ByteArrayOutputStream();
        private boolean truncated;

        private LimitedOutputStream(int maxSize) {
            this.maxSize = Math.max(0, maxSize);
        }

        @Override
        public void write(int value) {
            if (delegate.size() < maxSize) {
                delegate.write(value);
            } else {
                truncated = true;
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            int remaining = maxSize - delegate.size();
            if (remaining > 0) {
                delegate.write(bytes, offset, Math.min(remaining, length));
            }
            if (length > Math.max(remaining, 0)) {
                truncated = true;
            }
        }

        private void reset() {
            delegate.reset();
            truncated = false;
        }

        private byte[] toByteArray() {
            return delegate.toByteArray();
        }

        private boolean isTruncated() {
            return truncated;
        }
    }
}
