package org.jcodec.api.specific;

import org.jcodec.api.MediaInfo;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Picture8Bit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface ContainerAdaptor {

    @Deprecated
    Picture decodeFrame(Packet packet, int[][] data);
    Picture8Bit decodeFrame8Bit(Packet packet, byte[][] data);

    boolean canSeek(Packet data);

    @Deprecated
    int[][] allocatePicture();
    
    byte[][] allocatePicture8Bit();

    MediaInfo getMediaInfo();

}
