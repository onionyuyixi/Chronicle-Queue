package net.openhft.chronicle.queue.impl.single;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.ValueOut;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public final class QueueInspectorTest {
    private static final String PROPERTY_KEY = "wire.encodePidInHeader";
    private static String previousValue = null;

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    @BeforeClass
    public static void enableFeature() {
        previousValue = System.getProperty(PROPERTY_KEY);
        System.setProperty(PROPERTY_KEY, Boolean.TRUE.toString());
    }

    @AfterClass
    public static void resetFeature() {
        if (previousValue != null) {
            System.setProperty(PROPERTY_KEY, previousValue);
        }
    }

    @Test
    public void shouldDetermineWritingProcessIdWhenDocumentIsNotComplete() throws IOException {
        try (final SingleChronicleQueue queue = SingleChronicleQueueBuilder.binary(tmpDir.newFolder()).
                testBlockSize().
                build()) {
            final QueueInspector inspector = new QueueInspector(queue);
            final ExcerptAppender appender = queue.acquireAppender();
            appender.writeDocument(37L, ValueOut::int64);
            try (final DocumentContext ctx = appender.writingDocument()) {
                ctx.wire().write("foo").int32(17L);
                assertThat(inspector.getWritingProcessId(), is(OS.getProcessId()));
            }
        }
    }

    @Test
    public void shouldIndicateNoProcessIdWhenDocumentIsComplete() throws IOException {
        try (final SingleChronicleQueue queue = SingleChronicleQueueBuilder.binary(tmpDir.newFolder()).
                testBlockSize().
                build()) {
            final QueueInspector inspector = new QueueInspector(queue);
            final ExcerptAppender appender = queue.acquireAppender();
            appender.writeDocument(37L, ValueOut::int64);
            try (final DocumentContext ctx = appender.writingDocument()) {
                ctx.wire().write("foo").int32(17L);
            }
            final int writingProcessId = inspector.getWritingProcessId();
            assertThat(writingProcessId, is(not(OS.getProcessId())));
            assertThat(QueueInspector.isValidProcessId(writingProcessId), is(false));
        }
    }
}