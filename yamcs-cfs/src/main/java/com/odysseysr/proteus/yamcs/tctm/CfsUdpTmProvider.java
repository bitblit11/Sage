package com.odysseysr.proteus.yamcs.tctm;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.ConfigurationException;
import org.yamcs.YConfiguration;
import org.yamcs.YamcsServer;
import org.yamcs.api.EventProducer;
import org.yamcs.api.EventProducerFactory;
import org.yamcs.archive.PacketWithTime;
import org.yamcs.parameter.SystemParametersCollector;
import org.yamcs.parameter.SystemParametersProducer;
import org.yamcs.protobuf.Pvalue.ParameterValue;
import org.yamcs.protobuf.Yamcs.NamedObjectId;
import org.yamcs.tctm.TmPacketSource;
import org.yamcs.tctm.TmSink;
import org.yamcs.time.TimeService;

import com.google.common.util.concurrent.AbstractExecutionThreadService;


public class CfsUdpTmProvider extends AbstractExecutionThreadService implements TmPacketSource,  SystemParametersProducer {
	protected volatile long packetcount = 0;
	protected DatagramSocket tmSocket;
	protected String host="localhost";
	protected int port=10031;
	protected volatile boolean disabled=false;
	protected EventProducer eventProducer;
	protected int eventMsgID = 0x0808;
	protected Logger log=LoggerFactory.getLogger(this.getClass().getName());
	private TmSink tmSink;
	private int CFE_SB_TLM_HDR_SIZE = 6;
	private int OS_MAX_API_NAME = 20;
	private int CFE_EVS_MAX_MESSAGE_LENGTH = 122;
	
	private int CFE_EVS_DEBUG_BIT 		= 0x0001;
	private int CFE_EVS_INFORMATION_BIT = 0x0002;
	private int CFE_EVS_ERROR_BIT 		= 0x0004;
	private int CFE_EVS_CRITICAL_BIT 	= 0x0008;
	
	private SystemParametersCollector sysParamCollector;
	ParameterValue svConnectionStatus;
	List<ParameterValue> sysVariables= new ArrayList<ParameterValue>();
	private NamedObjectId sv_linkStatus_id, sp_dataCount_id;
	final String yamcsInstance;
	final String name;
	final TimeService timeService;

	protected CfsUdpTmProvider(String instance, String name) {// dummy constructor needed by subclass constructors
		this.yamcsInstance = instance;
		this.name = name;
		this.timeService = YamcsServer.getTimeService(instance);
		
        eventProducer=EventProducerFactory.getEventProducer(this.yamcsInstance);
        eventProducer.setSource("CFS");
	}

	public CfsUdpTmProvider(String instance, String name, String spec) throws ConfigurationException  {
		this.yamcsInstance = instance;
		this.name = name;

		YConfiguration c=YConfiguration.getConfiguration("cfs");
		host=c.getString(spec, "tmHost");
		port=c.getInt(spec, "tmPort");
		this.timeService = YamcsServer.getTimeService(instance);
		
        eventProducer=EventProducerFactory.getEventProducer(this.yamcsInstance);
        eventProducer.setSource("CFS");
	}
	
	public boolean isEventMsg(byte rawPacket[]) {
		ByteBuffer bb = ByteBuffer.wrap(rawPacket);
		int msgID = bb.getShort();
		if(msgID == eventMsgID) {
			return true;
		}
		
		return false;
	}

	/*
	 * 
	 * 
	typedef struct 
	{
		uint8   StreamId[2];  /* packet identifier word (stream ID) 
      		/*  bits  shift   ------------ description ---------------- 
      		/* 0x07FF    0  : application ID                            
      		/* 0x0800   11  : secondary header: 0 = absent, 1 = present 
      		/* 0x1000   12  : packet type:      0 = TLM, 1 = CMD        
      		/* 0xE000   13  : CCSDS version, always set to 0            
	
   		uint8   Sequence[2];  /* packet sequence word */
      		/*  bits  shift   ------------ description ---------------- 
      		/* 0x3FFF    0  : sequence count                            
      		/* 0xC000   14  : segmentation flags:  3 = complete packet  

		uint8  Length[2];     /* packet length word */
			/*  bits  shift   ------------ description ---------------- 
			/* 0xFFFF    0  : (total packet length) - 7                 

		
   		uint8   Time[CCSDS_TIME_SIZE];
   
		uint8   TlmHeader[CFE_SB_TLM_HDR_SIZE];
   		char    AppName[OS_MAX_API_NAME];
   		uint16  EventID;                
   		uint16  EventType;
   		uint32  SpacecraftID;
   		uint32  ProcessorID;                          
		char    Message[CFE_EVS_MAX_MESSAGE_LENGTH]; 
		uint8   Spare1;                               
		uint8   Spare2;		                      
	} CFE_EVS_Packet_t;
	*/
	
	public void ProcessEventMsg(byte rawPacket[]) {
		ByteBuffer bb = ByteBuffer.wrap(rawPacket);
		
		bb.position(6);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		long coarseTime = bb.getInt();
		long fineTime = bb.getInt();

		bb.position(bb.position()+2);
		
		byte bAppName[] = new byte[OS_MAX_API_NAME];
	    bb.get(bAppName);
		try {
			String appName = new String(bAppName, "ASCII");

			int eventID = bb.getShort();
			int eventType = bb.getShort();
			long spacecraftID = bb.getInt();
			long processorID = bb.getInt();
			String message = "";
			if(bb.remaining() >= CFE_EVS_MAX_MESSAGE_LENGTH) {
				byte bMessage[] = new byte[CFE_EVS_MAX_MESSAGE_LENGTH];
			    bb.get(bMessage);
				message = "\n" + new String(bMessage, "ASCII");
			}
			
			if((eventType & CFE_EVS_DEBUG_BIT) != 0) {
	            eventProducer.sendInfo("" + spacecraftID + "/" + processorID + "/" + appName, "DEBUG " + eventID + message);
			}
			if((eventType & CFE_EVS_INFORMATION_BIT) != 0) {
	            eventProducer.sendInfo("" + spacecraftID + "/" + processorID + "/" + appName, "INFO " + eventID + message);
			}
			if((eventType & CFE_EVS_ERROR_BIT) != 0) {
	            eventProducer.sendWarning("" + spacecraftID + "/" + processorID + "/" + appName, "ERROR " + eventID + message);
			}
			if((eventType & CFE_EVS_CRITICAL_BIT) != 0) {
	            eventProducer.sendError("" + spacecraftID + "/" + processorID + "/" + appName, "CRITICAL " + eventID + message);
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void openSocket() throws IOException {
        tmSocket = new DatagramSocket(port);
	}

	public void setTmSink(TmSink tmSink) {
		this.tmSink=tmSink;
	}

	public void run() {
		setupSysVariables();
		while(isRunning()) {
			PacketWithTime pwrt=getNextPacket();
			if(pwrt==null) break;
			tmSink.processPacket(pwrt);
		}
	}

	public PacketWithTime getNextPacket() {
		ByteBuffer bb=null;
		while (isRunning()) {
			while(disabled) {
				if(!isRunning()) return null;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return null;
				}
			}
			try {
				if (tmSocket==null) {
					openSocket();
					log.info("TM connection established to "+host+" port "+port);
				} 
				byte rawPacket[] = new byte[65535];
				int bytesReceived = readWithBlocking(rawPacket,0,65535);
				if(bytesReceived <= 0)
					continue;
				
				rawPacket = Arrays.copyOf(rawPacket, bytesReceived);
				
				if(isEventMsg(rawPacket)) {
					ProcessEventMsg(rawPacket);
				}
				
				bb=ByteBuffer.allocate(bytesReceived + 4);
				bb.putInt(0);
				bb.put(rawPacket);
				bb.rewind();
				short mask = 0x07ff;
				short apid = (short) (bb.getShort(4) & mask);
				int packetID = (int)apid;
				bb.putInt(packetID);
				bb.rewind();
				packetcount++;
				break;
			} catch (IOException e) {
				log.info("Cannot open or read from TM socket at "+host+":"+port+": "+e+"; retrying in 10 seconds.");
				try {tmSocket.close();} catch (Exception e2) {}
				tmSocket=null;
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					log.warn("exception "+ e1.toString()+" thrown when sleeping 10 sec");
					return null;
				}
			}
		}
		if(bb!=null) {
			return new PacketWithTime(timeService.getMissionTime(), CfsTlmPacket.getInstant(bb), bb.array());
		} 
		return null;
	}

	public boolean isArchiveReplay() {
		return false;
	}
	/**
	 * Read n bytes from the tmSocket, blocking if necessary till all bytes are available.
	 * Returns true if all the bytes have been read and false if the stream has closed before all the bytes have been read.
	 * @param b
	 * @param n
	 * @return
	 * @throws IOException 
	 */
	protected int readWithBlocking(byte[] b, int pos, int n) throws IOException {
		DatagramPacket packet = new DatagramPacket(b, pos, n); //, address);
		tmSocket.receive(packet);
		return packet.getLength();
	}

	public String getLinkStatus() {
		if (disabled) return "DISABLED";
		if (tmSocket==null) {
			return "UNAVAIL";
		} else {
			return "OK";
		}
	}

	@Override
	public void triggerShutdown() {
		if(tmSocket!=null) {
			tmSocket.close();
			tmSocket=null;
		}
	}

	public void disable() {
		disabled=true;
		if(tmSocket!=null) {
			tmSocket.close();
			tmSocket=null;
		}
	}

	public void enable() {
		disabled=false;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public String getDetailedStatus() {
		if(disabled) {
			return String.format("DISABLED (should connect to %s:%d)", host, port);
		}
		if (tmSocket==null) {
			return String.format("Not connected to %s:%d", host, port);
		} else {
			return String.format("OK, connected to %s:%d, received %d packets", host, port, packetcount);
		}
	}


	public long getDataCount() {
		return packetcount;
	}

	protected void setupSysVariables() {
		this.sysParamCollector = SystemParametersCollector.getInstance(yamcsInstance);
		if(sysParamCollector!=null) {
			sysParamCollector.registerProvider(this, null);
			sv_linkStatus_id = NamedObjectId.newBuilder().setName(sysParamCollector.getNamespace()+"/"+name+"/linkStatus").build();
			sp_dataCount_id = NamedObjectId.newBuilder().setName(sysParamCollector.getNamespace()+"/"+name+"/dataCount").build();


		} else {
			log.info("System variables collector not defined for instance {} ", yamcsInstance);
		}

	}

	public Collection<ParameterValue> getSystemParameters() {
		long time = timeService.getMissionTime();
		ParameterValue linkStatus = SystemParametersCollector.getPV(sv_linkStatus_id, time, getLinkStatus());
		ParameterValue dataCount = SystemParametersCollector.getPV(sp_dataCount_id, time, getDataCount());
		return Arrays.asList(linkStatus, dataCount);
	}
}

