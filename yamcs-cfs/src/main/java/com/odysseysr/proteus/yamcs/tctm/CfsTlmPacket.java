package com.odysseysr.proteus.yamcs.tctm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.yamcs.utils.CcsdsPacket;
import org.yamcs.utils.TimeEncoding;



public class CfsTlmPacket implements Comparable<CfsTlmPacket>{
    static public final int DATA_OFFSET=14;
    
	static public final int MAX_CCSDS_SIZE=1500;
	protected ByteBuffer bb;
	
	public CfsTlmPacket( byte[] packet) {
		bb = ByteBuffer.wrap(packet);
	}

	public CfsTlmPacket(ByteBuffer bb) {
		this.bb=bb;
	}

	public int getSecondaryHeaderFlag() {
		return (bb.getShort(0)>>11)&1;
	}

	public int getSequenceCount() {
		return bb.getShort(2)&0x3FFF;
	}
	
	public void setSequenceCount(short seqCount) {
	   short oldSeqField = bb.getShort(2);
	   short seqInd = (short)(oldSeqField&(~0x3FFF));
	   bb.putShort(2, (short)((seqCount&0x3FFF) + seqInd));
	}

	public static short getSequenceCount(ByteBuffer bb) {
		return (short) (bb.getShort(2)&0x3FFF);
	}

	public int getAPID() {
		return bb.getShort(0)& 0x07FF;
	}
	
	public void setAPID(int apid) {
		int tmp=bb.getShort(0) & (~0x07FF);
		tmp=tmp|apid;
		bb.putShort(0,(short)tmp);
	}	
	public static short getAPID(ByteBuffer bb) {
		return (short)(bb.getShort(0)& 0x07FF);
	}
	
	/*returns the length written in the ccsds header*/
    public static int getCcsdsPacketLength(ByteBuffer bb) {
        return bb.getShort(4)&0xFFFF;
    }
    
	
	/*returns the length written in the ccsds header*/
	public int getCcsdsPacketLength() {
		return getCcsdsPacketLength(bb);
	}
	
    public void setCcsdsPacketLength(short length) {
        //return bb.getShort(4)&0xFFFF;
        bb.putShort(4, length);
    }
	
	/**returns the length of the packet, normally equals ccsdslength+7*/
	public int getLength() {
		return bb.capacity();
	}
	
	/**
	 * 
	 * @return instant
	 */
	public long getInstant() {
		return TimeEncoding.fromGpsCcsdsTime(bb.getInt(6), bb.get(10)) ;
	}

	public static long getInstant(ByteBuffer bb) {
		return TimeEncoding.fromGpsCcsdsTime(bb.getInt(6), bb.get(10)) ;
	}

	/**
	 * 
	 * @param data.bb
	 * @return time in seconds since 6 Jan 1980
	 */
	public long getCoarseTime() {
		return bb.getInt(6)&0xFFFFFFFFL;
	}
	public int getTimeId() {
		return (bb.get(11) &0xFF)>>6;
	}
	
	public void setCoarseTime(int time) {
	    bb.putInt(6, time);
	}
	
	public static long getCoarseTime(ByteBuffer bb) {
		return bb.getInt(6)&0xFFFFFFFFL;
	}
	
	public int getFineTime() {
		long lngFineTime = bb.getInt(10) & 0xFFFFFFFFL;
		float fltFineTime = lngFineTime / 4294967296L;
		byte bFineTime = (byte)(fltFineTime * 256);
		return bFineTime;
	}
	
	public void setFineTime(short fineTime) {
		long lngFineTime = bb.getInt(10) & 0xFFFFFFFFL;
		float fltFineTime = lngFineTime / 4294967296L;
		byte bFineTime = (byte)(fltFineTime * 256);
	    bb.putInt(10, (byte)(fineTime&0xFF));
	}
	
	public static int getFineTime(ByteBuffer bb) {
		return bb.get(10)&0xFF;
	}

	public byte[] getBytes() {
		return bb.array();
	}
	
	public ByteBuffer getByteBuffer() {
		return bb;
	}

	public static CcsdsPacket getPacketFromStream(InputStream input) throws IOException {
		byte[] b=new byte[6];
		ByteBuffer bb=ByteBuffer.wrap(b);
		if ( input.read(b) < 6 ) {
			throw new IOException("cannot read CCSDS primary header\n");
		}
		int ccsdslen = bb.getShort(4)&0xFFFF;
		if ( ccsdslen  > MAX_CCSDS_SIZE ) {
			throw new IOException("illegal CCSDS length "+ ccsdslen);
		}
		bb=ByteBuffer.allocate(ccsdslen+7);
		bb.put(b);

		if ( input.read(bb.array(), 6, ccsdslen+1) < ccsdslen+1 ) {
			throw new IOException("cannot read full packet");
		}
		return new CcsdsPacket(bb);
	}
	
	
	public static long getInstant(byte[] pkt) {
        return getInstant(ByteBuffer.wrap(pkt));
    }
	
	
	
	public static short getAPID(byte[] packet) {
	    return getAPID(ByteBuffer.wrap(packet));
	}
	
	public static int getCccsdsPacketLength(byte[] buf) {       
        return getCcsdsPacketLength(ByteBuffer.wrap(buf));
    }

    /*comparison based on time*/
    public int compareTo(CfsTlmPacket p) {
        return Long.signum(this.getInstant()-p.getInstant());
    }
	
	@Override
    public String toString() {
		StringBuffer sb=new StringBuffer();
		StringBuffer text = new StringBuffer();
		byte c;
		int len = bb.limit();
		sb.append("apid: "+getAPID()+"\n");
		sb.append("time: "+ TimeEncoding.toCombinedFormat(getInstant()));
		sb.append("\n");
		for(int i=0;i<len;++i) {
			if(i%16==0) {
				sb.append(String.format("%04x:",i));
				text.setLength(0);
			}
			c = bb.get(i);
			if ( (i & 1) == 0 ) {
				sb.append(String.format(" %02x",0xFF&c));
			} else {
				sb.append(String.format("%02x",0xFF&c));
			}
			text.append(((c >= ' ') && (c <= 127)) ? String.format("%c",c) : ".");
			if((i+1)%16==0) {
				sb.append(" ");
				sb.append(text);
				sb.append("\n");
			}
		}
		sb.append("\n\n");
		return sb.toString();
	}

}