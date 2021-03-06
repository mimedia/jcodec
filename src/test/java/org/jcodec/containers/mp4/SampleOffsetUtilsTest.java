package org.jcodec.containers.mp4;

import static org.jcodec.containers.mp4.SampleOffsetUtils.*;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.jcodec.containers.mp4.boxes.ChunkOffsetsBox;
import org.jcodec.containers.mp4.boxes.MediaInfoBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.junit.Test;


public class SampleOffsetUtilsTest {
    File f = new File("src/test/resources/petro_dae/95BCC757-7E23-445B-B7AB-6737208069FA/example1.mp4");
    @Test
    public void testGetFirstSampleAtChunk() throws Exception {
        MovieBox moov = MP4Util.parseMovie(f);
        MediaInfoBox minf = moov.getAudioTracks().get(0).getMdia().getMinf();
        ChunkOffsetsBox stco = NodeBox.findFirst(minf, ChunkOffsetsBox.class, "stbl", "stco");
        SampleToChunkBox stsc = NodeBox.findFirst(minf, SampleToChunkBox.class, "stbl", "stsc");
        assertEquals(0, getFirstSampleAtChunk(1, stsc, stco));
        assertEquals(2, getFirstSampleAtChunk(2, stsc, stco));
        assertEquals(4, getFirstSampleAtChunk(3, stsc, stco));
    }
    @Test
    public void testGetChunkBySample() throws Exception {
        MovieBox moov = MP4Util.parseMovie(f);
        MediaInfoBox minf = moov.getAudioTracks().get(0).getMdia().getMinf();
        ChunkOffsetsBox stco = NodeBox.findFirst(minf, ChunkOffsetsBox.class, "stbl", "stco");
        SampleToChunkBox stsc = NodeBox.findFirst(minf, SampleToChunkBox.class, "stbl", "stsc");
        assertEquals(1, getChunkBySample(0, stco, stsc));
        assertEquals(1, getChunkBySample(1, stco, stsc));
        assertEquals(2, getChunkBySample(2, stco, stsc));
        assertEquals(2, getChunkBySample(3, stco, stsc));
    }
    
    @Test
    public void testGetSamplesInChunk() throws Exception {
        MovieBox moov = MP4Util.parseMovie(f);
        MediaInfoBox minf = moov.getAudioTracks().get(0).getMdia().getMinf();
        SampleToChunkBox stsc = NodeBox.findFirst(minf, SampleToChunkBox.class, "stbl", "stsc");
        assertEquals(2, getSamplesInChunk(1, stsc));
        assertEquals(2, getSamplesInChunk(2, stsc));
        assertEquals(1, getSamplesInChunk(4, stsc));
        assertEquals(4, getSamplesInChunk(12, stsc));
    }
}
