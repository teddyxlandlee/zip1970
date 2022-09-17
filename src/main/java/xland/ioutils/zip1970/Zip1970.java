package xland.ioutils.zip1970;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.chrono.ChronoLocalDateTime;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Zip1970 implements Closeable {
    private final ZipFile zipFile;
    /*@Nullable*/ private final Pattern included;
    private final Pattern excluded;
    /*@Nullable*/private final LocalDateTime createTime, modifyTime, accessTime;
    private final ZipOutputStream output;

    public Zip1970(ZipFile zipFile, Pattern included, Pattern excluded,
                   LocalDateTime createTime, LocalDateTime modifyTime, LocalDateTime accessTime, ZipOutputStream output) {
        this.zipFile = zipFile;
        this.included = included;
        this.excluded = excluded;
        this.createTime = createTime;
        this.modifyTime = modifyTime;
        this.accessTime = accessTime;
        this.output = output;
    }

    public void process() throws IOException {
        final FileTime cTime = ofFileTime(createTime), aTime = ofFileTime(accessTime), mTime = ofFileTime(modifyTime);
        try {
            zipFile.stream().forEach(zipEntry -> {
                try {
                    final String name = zipEntry.getName();
                    output.putNextEntry(zipEntry);
                    if ((included == null || included.matcher(name).matches()) &&
                            (excluded == null || !excluded.matcher(name).matches())) {
                        if (cTime != null) zipEntry.setCreationTime(cTime);
                        if (aTime != null) zipEntry.setLastAccessTime(aTime);
                        if (mTime != null) zipEntry.setLastModifiedTime(mTime);
                    }
                    transferTo(zipFile.getInputStream(zipEntry), output);
                    output.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeExceptionWrapper(e);
                }
            });
        } catch (RuntimeExceptionWrapper w) {
            throw w.getCause();
        }
    }

    private static /*@Nullable*/ FileTime ofFileTime(ChronoLocalDateTime<?> time) {
        if (time == null) return null;
        return FileTime.from(time.toInstant(ZoneOffset.UTC));
    }

    private static final class RuntimeExceptionWrapper extends RuntimeException {
        RuntimeExceptionWrapper(IOException e) {
            super(e);
        }

        @Override
        public IOException getCause() {
            return (IOException) super.getCause();
        }
    }

    private static void transferTo(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer, 0, 8192)) >= 0)
            out.write(buffer, 0, read);
    }

    @Override
    public void close() throws IOException {
        zipFile.close();
        output.close();
    }
}
