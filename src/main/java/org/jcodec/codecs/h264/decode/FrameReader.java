package org.jcodec.codecs.h264.decode;

import org.jcodec.codecs.common.biari.MDecoder;
import org.jcodec.codecs.h264.decode.aso.MapManager;
import org.jcodec.codecs.h264.decode.aso.Mapper;
import org.jcodec.codecs.h264.io.CABAC;
import org.jcodec.codecs.h264.io.CAVLC;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.PictureParameterSet;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.io.model.SliceHeader;
import org.jcodec.common.IntObjectMap;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.logging.Logger;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static org.jcodec.codecs.h264.H264Utils.unescapeNAL;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * MPEG 4 AVC ( H.264 ) Frame reader
 *
 * Conforms to H.264 ( ISO/IEC 14496-10 ) specifications
 *
 * @author The JCodec project
 *
 */
public class FrameReader {
    private IntObjectMap<SeqParameterSet> sps = new IntObjectMap<>();
    private IntObjectMap<PictureParameterSet> pps = new IntObjectMap<>();

    public List<SliceReader> readFrame(final List<ByteBuffer> nalUnits) {
        final List<SliceReader> result = new LinkedList<>();
        for (ByteBuffer nalData : nalUnits) {
            NALUnit nalUnit = NALUnit.read(nalData);
            unescapeNAL(nalData);

            if (nalUnit.type != null) {
                Logger.debug(String.format("nalUnit.type = %s", nalUnit.type.getName()));
                switch (nalUnit.type) {
                    case NON_IDR_SLICE:
                    case IDR_SLICE:
                        if (sps.size() == 0 || pps.size() == 0) {
                            Logger.warn("Skipping frame as no SPS/PPS have been seen so far...");
                        } else {
                            result.add(createSliceReader(nalData, nalUnit));
                        }
                        break;
                    case SPS:
                        SeqParameterSet _sps = SeqParameterSet.read(nalData);
                        sps.put(_sps.seq_parameter_set_id, _sps);
                        break;
                    case PPS:
                        PictureParameterSet _pps = PictureParameterSet.read(nalData);
                        pps.put(_pps.pic_parameter_set_id, _pps);
                        break;
                    default:
                }
            } else {
                Logger.warn("NAL unit type is null");
            }
        }

        return result;
    }

    private SliceReader createSliceReader(ByteBuffer segment, NALUnit nalUnit) {
        BitReader in = new BitReader(segment);
        SliceHeaderReader shr = new SliceHeaderReader();
        SliceHeader sh = shr.readPart1(in);
        sh.pps = pps.get(sh.pic_parameter_set_id);
        sh.sps = sps.get(sh.pps.seq_parameter_set_id);
        shr.readPart2(sh, nalUnit, sh.sps, sh.pps, in);

        Mapper mapper = new MapManager(sh.sps, sh.pps).getMapper(sh);

        CAVLC[] cavlc = new CAVLC[] { new CAVLC(sh.sps, sh.pps, 2, 2), new CAVLC(sh.sps, sh.pps, 1, 1),
                new CAVLC(sh.sps, sh.pps, 1, 1) };

        int mbWidth = sh.sps.pic_width_in_mbs_minus1 + 1;
        CABAC cabac = new CABAC(mbWidth);

        MDecoder mDecoder = null;
        if (sh.pps.entropy_coding_mode_flag) {
            in.terminate();
            int[][] cm = new int[2][1024];
            int qp = sh.pps.pic_init_qp_minus26 + 26 + sh.slice_qp_delta;
            cabac.initModels(cm, sh.slice_type, sh.cabac_init_idc, qp);
            mDecoder = new MDecoder(segment, cm);
        }

        return new SliceReader(sh.pps, cabac, cavlc, mDecoder, in, mapper, sh, nalUnit);
    }

    public void addSps(List<ByteBuffer> spsList) {
        for (ByteBuffer byteBuffer : spsList) {
            ByteBuffer dup = byteBuffer.duplicate();
            unescapeNAL(dup);
            SeqParameterSet s = SeqParameterSet.read(dup);
            sps.put(s.seq_parameter_set_id, s);
        }
    }

    public void addPps(List<ByteBuffer> ppsList) {
        for (ByteBuffer byteBuffer : ppsList) {
            ByteBuffer dup = byteBuffer.duplicate();
            unescapeNAL(dup);
            PictureParameterSet p = PictureParameterSet.read(dup);
            pps.put(p.pic_parameter_set_id, p);
        }
    }
}