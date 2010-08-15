package com.cleverua.bb;

import java.io.EOFException;

import net.rim.device.api.servicebook.ServiceBook;
import net.rim.device.api.servicebook.ServiceRecord;
import net.rim.device.api.synchronization.ConverterUtilities;
import net.rim.device.api.system.CoverageInfo;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.system.RadioInfo;
import net.rim.device.api.system.WLANInfo;
import net.rim.device.api.util.DataBuffer;

public class Transports {
    private static final String INTERFACE_WIFI = ";interface=wifi";
    private static final String CONNECTION_UID = ";ConnectionUID=";
    private static final String CONNECTION_TYPE_BIS = ";ConnectionType=mds-public";
    
    public static final String MDS         = "MDS";
    private static final String BIS         = "BIS";
    private static final String WAP        = "WAP 1.x";    // WAP 1.x is not supported yet
    public static final String WAP2        = "WAP2";
    public static final String WIFI        = "WiFi";
    public static final String DIRECT_TCP  = "Direct TCP";
    public static final String UNITE       = "Unite";
    public static final String AUTOMATIC   = "Automatic";
    
    /** 
     * CONFIG_TYPE_ constants which are used to find appropriate service books.
     * TODO Currently only Unite is detected this way. 
     */ 
    private static final int CONFIG_TYPE_WAP  = 0;
    private static final int CONFIG_TYPE_BES  = 1;
    
    private static final String UNITE_STR   = "unite";
    private static final String MMS_STR     = "mms";
    private static final String GPMDS_STR   = "gpmds";
    private static final String IPPP_STR    = "ippp";
    private static final String WAP_STR     = "wap";
    private static final String WPTCP_STR   = "wptcp";
    private static final String WIFI_STR    = "wifi";
    
    private static final String DEVICESIDE_TRUE  = ";deviceside=true";
    private static final String DEVICESIDE_FALSE = ";deviceside=false";

    private static Transports instance;

    private ServiceRecord srMDS;
    private ServiceRecord srBIS;
    private ServiceRecord srWAP;
    private ServiceRecord srWAP2;
//    private ServiceRecord srWiFi;
    private ServiceRecord srUnite;

    private boolean coverageTCP   = false;
    private boolean coverageMDS   = false;
    private boolean coverageBIS   = false;
    private boolean coverageWAP   = false;
    private boolean coverageWAP2  = false;
    private boolean coverageWiFi  = false;
    private boolean coverageUnite = false;
    
    private String lastSuccessfulTransport;

    public static Transports getInstance() {
        if (instance == null) {
            instance = new Transports();
        }
        return instance;
    }

    public boolean isAcceptable(String transportType) {
        if (MDS.equals(transportType)) {
            return coverageMDS && (srMDS != null);
        } else if (BIS.equals(transportType)) {
            return coverageBIS && (srBIS != null);
        } else if (WAP.equals(transportType)) {
            return coverageWAP && (srWAP != null);
        } else if (WAP2.equals(transportType)) {
            return coverageWAP2 && (srWAP2 != null);
        } else if (WIFI.equals(transportType)) {
            return coverageWiFi;
        } else if (DIRECT_TCP.equals(transportType)) {
            return coverageTCP;   
        } else if (UNITE.equals(transportType)) {
            return coverageUnite && (srUnite != null);
        } else {
            return false;
        }
    }
    
    public String getUrlForTransport(String baseUrl, String transportType) {
        if (MDS.equals(transportType)) {
            Logger.debug("Preparing url for MDS...");
            return getMDSUrl(baseUrl);
        } else if (BIS.equals(transportType)) {            
            Logger.debug("Preparing url for BIS...");
            return getBISUrl(baseUrl);
        } else if (WAP.equals(transportType)) {
            Logger.debug("Preparing url for WAP 1.x...");
            return getWAPUrl(baseUrl);
        } else if (WAP2.equals(transportType)) {
            Logger.debug("Preparing url for WAP2...");
            return getWAP2Url(baseUrl);
        } else if (WIFI.equals(transportType)) {
            Logger.debug("Preparing url for WiFi...");
            return getWiFiUrl(baseUrl);
        } else if (DIRECT_TCP.equals(transportType)) {
            Logger.debug("Preparing url for Direct TCP...");
            return getTCPUrl(baseUrl);
        } else if (UNITE.equals(transportType)) {
            Logger.debug("Preparing url for Unite...");
            return getUniteUrl(baseUrl);
        } else {
            return null;
        }
    }
    
    public String getLastSuccessfulTransport() {
        return lastSuccessfulTransport;
    }

    public void setLastSuccessfulTransport(String lastSuccessfulTransport) {
        this.lastSuccessfulTransport = lastSuccessfulTransport;
    }

    private String getMDSUrl(String url) {
        return url + DEVICESIDE_FALSE;
    }

    private String getBISUrl(String baseUrl) {
        return baseUrl + DEVICESIDE_FALSE + CONNECTION_TYPE_BIS;
    }
    
    // TODO: Manage WAP's attributes (for WAP 1.x)
    private String getWAPUrl(String baseUrl) {
        return baseUrl + DEVICESIDE_TRUE;
    }
    
    private String getWAP2Url(String baseUrl) {
        return baseUrl + DEVICESIDE_TRUE + CONNECTION_UID + srWAP2.getUid();
    }
    
    private String getWiFiUrl(String baseUrl) {
        return baseUrl + INTERFACE_WIFI;
    }
    
    /**
     * This method intends that user has filled the APN settings in device options. 
     * So the result url WILL NOT include the 
     * <b>apn</b>, <b>tunnelauthusername</b> and <b>tunnelauthpassword</b> parameters. 
     */
    private String getTCPUrl(String baseUrl) {
        return baseUrl + DEVICESIDE_TRUE;
    }
    
    private String getUniteUrl(String baseUrl) {
        if (srUnite == null) {
            throw new IllegalStateException("Unite is not supported!");
        }
        return baseUrl + DEVICESIDE_FALSE + CONNECTION_UID + srUnite.getUid();
    }
    
    private Transports() {
        init();
    }

    private void init() {
        ServiceBook sb = ServiceBook.getSB();
        ServiceRecord[] records = sb.getRecords();

        for (int i = 0; i < records.length; i++) {
            ServiceRecord myRecord = records[i];
            String cid, uid;

            if (myRecord.isValid() && !myRecord.isDisabled()) {
                cid = myRecord.getCid().toLowerCase();
                uid = myRecord.getUid().toLowerCase();
                // BIS
                if (cid.indexOf(IPPP_STR) != -1 && uid.indexOf(GPMDS_STR) != -1) {
                    Logger.debug("BIS record detected!");
                    Logger.debug("cid = " + cid + ", uid = " + uid);
                    srBIS = myRecord;
                }           

                // BES
                if (cid.indexOf(IPPP_STR) != -1 && uid.indexOf(GPMDS_STR) == -1) {
                    Logger.debug("BES record detected!");
                    Logger.debug("cid = " + cid + ", uid = " + uid);
                    srMDS = myRecord;
                }
                /* WiFi - we do not need the WiFi record. 
                 * If the mobile network is disabled the records in the Service Book is disabled too. 
                 * But in does not mean that WiFi is unavailable. 
                 * So we'll check the WiFi with the other methods later. */
//                if (cid.indexOf(WPTCP_STR) != -1 && uid.indexOf(WIFI_STR) != -1) {
//                    Logger.debug("WiFi record detected!");
//                    Logger.debug("cid = " + cid + ", uid = " + uid);
//                    srWiFi = myRecord;
//                }       
                // Wap1.0
                if (getConfigType(myRecord)==CONFIG_TYPE_WAP && cid.equalsIgnoreCase(WAP_STR)) {
                    Logger.debug("WAP 1.x record detected!");
                    Logger.debug("cid = " + cid + ", uid = " + uid);
                    srWAP = myRecord;
                }
                // Wap2.0
                if (cid.indexOf(WPTCP_STR) != -1 && uid.indexOf(WIFI_STR) == -1 && uid.indexOf(MMS_STR) == -1) {
                    Logger.debug("WAP 2 record detected!");
                    Logger.debug("cid = " + cid + ", uid = " + uid);
                    srWAP2 = myRecord;
                }
                // Unite
                if(getConfigType(myRecord) == CONFIG_TYPE_BES && myRecord.getName().equalsIgnoreCase(UNITE_STR)) {
                    Logger.debug("Unite record detected!");
                    Logger.debug("cid = " + cid + ", uid = " + uid);
                    srUnite = myRecord;
                }
            }   
        }
        
        if (!CoverageInfo.isOutOfCoverage()) { /* data service turned ON in options */
            if (RadioInfo.isDataServiceOperational()) { /* radio enabled */
                if(CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_BIS_B)){
                    Logger.debug("BIS coverage detected!");
                    coverageBIS=true;   
                }  
                
                if(CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_DIRECT)){
                    Logger.debug("TCP, WAP 1.x, WAP 2 coverages detected!");
                    coverageTCP=true;
                    coverageWAP=true;
                    coverageWAP2=true;
                }
                
                // CoverageInfo.isCoverageSufficient always returns 'false' for simulator
                if(CoverageInfo.isCoverageSufficient(CoverageInfo.COVERAGE_MDS) || 
                        DeviceInfo.isSimulator()){
                    Logger.debug("BES-MDS, Unite coverage detected!");
                    coverageMDS=true;
                    coverageUnite=true;
                }   
            }
            if(RadioInfo.areWAFsSupported(RadioInfo.WAF_WLAN) /* does the device support WiFi? */ && 
                    WLANInfo.getWLANState()==WLANInfo.WLAN_STATE_CONNECTED /* is the WiFi connected? */){
                Logger.debug("WiFi coverage detected!");
                coverageWiFi = true;
            }
        }
    }

    /**
     * Gets the config type of a ServiceRecord using getDataInt below
     * @param record    A ServiceRecord
     * @return  configType of the ServiceRecord
     */
    private int getConfigType(ServiceRecord record) {
        return getDataInt(record, 12);
    }

    /**
     * Gets the config type of a ServiceRecord. Passing 12 as type returns the configType.    
     * @param record    A ServiceRecord
     * @param type  dataType
     * @return  configType
     */
    private int getDataInt(ServiceRecord record, int type)
    {
        DataBuffer buffer = null;
        buffer = getDataBuffer(record, type);

        if (buffer != null){
            try {
                return ConverterUtilities.readInt(buffer);
            } catch (EOFException e) {
                return -1;
            }
        }
        return -1;
    }

    /** 
     * Utility Method for getDataInt()
     */
    private DataBuffer getDataBuffer(ServiceRecord record, int type) {
        byte[] data = record.getApplicationData();
        if (data != null) {
            DataBuffer buffer = new DataBuffer(data, 0, data.length, true);
            try {
                buffer.readByte();
            } catch (EOFException e1) {
                return null;
            }
            if (ConverterUtilities.findType(buffer, type)) {
                return buffer;
            }
        }
        return null;
    }
}
