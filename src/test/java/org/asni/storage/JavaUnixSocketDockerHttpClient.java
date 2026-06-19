package org.asni.storage;

import com.github.dockerjava.transport.DockerHttpClient;

import java.io.*;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JavaUnixSocketDockerHttpClient implements DockerHttpClient {

    private final String socketPath;

    public JavaUnixSocketDockerHttpClient(String socketPath) {
        this.socketPath = socketPath;
    }

    @Override
    public Response execute(Request request) {
        try {
            SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
            channel.connect(UnixDomainSocketAddress.of(socketPath));

            OutputStream out = Channels.newOutputStream(channel);

            StringBuilder req = new StringBuilder();
            req.append(request.method()).append(' ').append(request.path()).append(" HTTP/1.1\r\n");
            req.append("Host: localhost\r\n");

            Map<String, String> headers = request.headers();
            if (headers != null) {
                for (Map.Entry<String, String> e : headers.entrySet()) {
                    req.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
                }
            }

            byte[] body = null;
            if (request.body() != null) {
                body = request.body().readAllBytes();
                if (body.length > 0) {
                    req.append("Content-Length: ").append(body.length).append("\r\n");
                }
            }

            req.append("Connection: close\r\n");
            req.append("\r\n");

            out.write(req.toString().getBytes(StandardCharsets.UTF_8));
            if (body != null && body.length > 0) {
                out.write(body);
            }
            out.flush();

            return readResponse(Channels.newInputStream(channel), channel);
        } catch (IOException e) {
            throw new RuntimeException("Docker unix socket request failed: " + e.getMessage(), e);
        }
    }

    private Response readResponse(InputStream rawIn, SocketChannel channel) throws IOException {
        String statusLine = readHeaderLine(rawIn);
        if (statusLine == null || !statusLine.startsWith("HTTP/")) {
            throw new IOException("Invalid HTTP response: " + statusLine);
        }
        String[] parts = statusLine.split(" ", 3);
        int statusCode = Integer.parseInt(parts[1]);

        Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
        String line;
        boolean chunked = false;
        long contentLength = -1;

        while ((line = readHeaderLine(rawIn)) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = line.substring(colon + 1).trim();
                responseHeaders.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
                if (name.equals("transfer-encoding") && value.equalsIgnoreCase("chunked")) {
                    chunked = true;
                }
                if (name.equals("content-length")) {
                    try { contentLength = Long.parseLong(value.trim()); } catch (NumberFormatException ignored) {}
                }
            }
        }

        InputStream bodyStream;
        if (chunked) {
            bodyStream = new ChunkedInputStream(rawIn, channel);
        } else if (contentLength >= 0) {
            bodyStream = new BoundedInputStream(rawIn, channel, contentLength);
        } else {
            bodyStream = new ClosingInputStream(rawIn, channel);
        }

        final int code = statusCode;
        final Map<String, List<String>> hdrs = responseHeaders;
        final InputStream finalBody = bodyStream;

        return new Response() {
            @Override
            public int getStatusCode() { return code; }

            @Override
            public Map<String, List<String>> getHeaders() { return hdrs; }

            @Override
            public String getHeader(String name) {
                List<String> vals = hdrs.get(name.toLowerCase(Locale.ROOT));
                return vals != null && !vals.isEmpty() ? vals.get(0) : null;
            }

            @Override
            public InputStream getBody() { return finalBody; }

            @Override
            public void close() {
                try { channel.close(); } catch (IOException ignored) {}
            }
        };
    }

    private String readHeaderLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                int next = in.read();
                if (next != '\n' && next != -1) buf.write(next);
                break;
            }
            if (b == '\n') break;
            buf.write(b);
        }
        if (buf.size() == 0 && b == -1) return null;
        return buf.toString(StandardCharsets.ISO_8859_1);
    }

    private static class ClosingInputStream extends InputStream {
        private final InputStream delegate;
        private final SocketChannel channel;

        ClosingInputStream(InputStream delegate, SocketChannel channel) {
            this.delegate = delegate;
            this.channel = channel;
        }

        @Override public int read() throws IOException { return delegate.read(); }
        @Override public int read(byte[] b, int off, int len) throws IOException { return delegate.read(b, off, len); }

        @Override
        public void close() throws IOException {
            try { delegate.close(); } finally { channel.close(); }
        }
    }

    private static class BoundedInputStream extends InputStream {
        private final InputStream delegate;
        private final SocketChannel channel;
        private long remaining;

        BoundedInputStream(InputStream delegate, SocketChannel channel, long length) {
            this.delegate = delegate;
            this.channel = channel;
            this.remaining = length;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int b = delegate.read();
            if (b != -1) remaining--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int toRead = (int) Math.min(len, remaining);
            int n = delegate.read(b, off, toRead);
            if (n > 0) remaining -= n;
            return n;
        }

        @Override
        public void close() throws IOException {
            try { delegate.close(); } finally { channel.close(); }
        }
    }

    private class ChunkedInputStream extends InputStream {
        private final InputStream source;
        private final SocketChannel channel;
        private int remaining = 0;
        private boolean done = false;

        ChunkedInputStream(InputStream source, SocketChannel channel) {
            this.source = source;
            this.channel = channel;
        }

        private boolean nextChunk() throws IOException {
            if (done) return false;
            String sizeLine = readHeaderLine(source);
            if (sizeLine == null) { done = true; return false; }
            int semi = sizeLine.indexOf(';');
            String hex = semi >= 0 ? sizeLine.substring(0, semi).trim() : sizeLine.trim();
            if (hex.isEmpty()) { done = true; return false; }
            remaining = Integer.parseInt(hex, 16);
            if (remaining == 0) {
                readHeaderLine(source);
                done = true;
                return false;
            }
            return true;
        }

        @Override
        public int read() throws IOException {
            while (remaining == 0) {
                if (!nextChunk()) return -1;
            }
            int b = source.read();
            if (b != -1) {
                remaining--;
                if (remaining == 0) readHeaderLine(source);
            }
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            while (remaining == 0) {
                if (!nextChunk()) return -1;
            }
            int toRead = Math.min(len, remaining);
            int n = source.read(b, off, toRead);
            if (n > 0) {
                remaining -= n;
                if (remaining == 0) readHeaderLine(source);
            }
            return n;
        }

        @Override
        public void close() throws IOException {
            try { source.close(); } finally { channel.close(); }
        }
    }

    @Override
    public void close() {}
}
