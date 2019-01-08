package com.TrakEngineering.FluidSecureHub.retrofit;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ServerResponse implements Serializable {

    @SerializedName("returned_username")
    private String username;
    @SerializedName("returned_password")
    private String password;
    @SerializedName("message")
    private String message;
    @SerializedName("response_code")
    private int responseCode;
    @SerializedName("ResponceMessage")
    private String ResponceMessage;
    @SerializedName("ResponceText")
    private String ResponceText;
    @SerializedName("VehicleId")
    private String VehicleId;


    //--------------------------
    @SerializedName("MinLimit")
    private String MinLimit;
    @SerializedName("SiteId")
    private String SiteId;
    @SerializedName("PulseRatio")
    private String PulseRatio;
    @SerializedName("PersonId")
    private String PersonId;
    @SerializedName("FuelTypeId")
    private String FuelTypeId;
    @SerializedName("PhoneNumber")
    private String PhoneNumber;
    @SerializedName("ServerDate")
    private String ServerDate;
    @SerializedName("PumpOnTime")
    private String PumpOnTime;
    @SerializedName("PumpOffTime")
    private String PumpOffTime;
    @SerializedName("PulserStopTime")
    private String PulserStopTime;
    @SerializedName("TransactionId")
    private String TransactionId;
    @SerializedName("FirmwareVersion")
    private String FirmwareVersion;
    @SerializedName("FilePath")
    private String FilePath;
    @SerializedName("FOBNumber")
    private String FOBNumber;
    @SerializedName("Company")
    private String Company;
    @SerializedName("Location")
    private String Location;
    @SerializedName("PersonName")
    private String PersonName;
    @SerializedName("PrinterName")
    private String PrinterName;
    @SerializedName("PrinterMacAddress")
    private String PrinterMacAddress;
    @SerializedName("VehicleSum")
    private String VehicleSum;
    @SerializedName("DeptSum")
    private String DeptSum;
    @SerializedName("VehPercentage")
    private String VehPercentage;
    @SerializedName("DeptPercentage")
    private String DeptPercentage;
    @SerializedName("SurchargeType")
    private String SurchargeType;
    @SerializedName("ProductPrice")
    private String ProductPrice;
    @SerializedName("parameter")
    private String parameter;

    @SerializedName("RequireManualOdo")
    private String RequireManualOdo;

    @SerializedName("VehicleNumber")
    private String VehicleNumber;

    @SerializedName("PreviousOdo")
    private String PreviousOdo;

    @SerializedName("OdoLimit")
    private String OdoLimit;

    @SerializedName("OdometerReasonabilityConditions")
    private String OdometerReasonabilityConditions;

    @SerializedName("CheckOdometerReasonable")
    private String CheckOdometerReasonable;


    @SerializedName("IsFSNPUpgradable")
    private String IsFSNPUpgradable;



    @SerializedName("IsTLDCall")
    private String IsTLDCall;



    //--------------------------



    public ServerResponse(String username, String password, String message, int responseCode, String ResponceMessage, String ResponceText,
                          String MinLimit, String SiteId, String PulseRatio, String PersonId, String FuelTypeId, String PhoneNumber, String ServerDate,
                          String PumpOnTime, String PumpOffTime, String PulserStopTime, String TransactionId, String FirmwareVersion, String FilePath,
                          String FOBNumber, String Company, String Location, String PersonName, String PrinterName, String PrinterMacAddress, String VehicleSum,
                          String DeptSum, String VehPercentage, String DeptPercentage, String SurchargeType, String ProductPrice, String parameter, String VehicleNumber, String RequireManualOdo,String PreviousOdo,String OdoLimit,String OdometerReasonabilityConditions,String CheckOdometerReasonable,String IsFSNPUpgradable,String IsTLDCall) {

        this.username = username;
        this.password = password;
        this.message = message;
        this.responseCode = responseCode;
        this.ResponceMessage = ResponceMessage;
        this.ResponceText = ResponceText;
        this.VehicleId = VehicleId;
        this.MinLimit = MinLimit;
        this.SiteId = SiteId;
        this.PulseRatio = PulseRatio;
        this.PersonId = PersonId;
        this.FuelTypeId = FuelTypeId;
        this.PhoneNumber = PhoneNumber;
        this.ServerDate = ServerDate;
        this.PumpOnTime = PumpOnTime;
        this.PumpOffTime = PumpOffTime;
        this.PulserStopTime = PulserStopTime;
        this.TransactionId = TransactionId;
        this.FirmwareVersion = FirmwareVersion;
        this.FilePath = FilePath;
        this.FOBNumber = FOBNumber;
        this.Company = Company;
        this.Location = Location;
        this.PersonName = PersonName;
        this.PrinterName = PrinterName;
        this.PrinterMacAddress = PrinterMacAddress;
        this.VehicleSum = VehicleSum;
        this.DeptSum = DeptSum;
        this.VehPercentage = VehPercentage;
        this.DeptPercentage = DeptPercentage;
        this.SurchargeType = SurchargeType;
        this.ProductPrice = ProductPrice;
        this.parameter = parameter;
        this.RequireManualOdo = RequireManualOdo;
        this.VehicleNumber = VehicleNumber;
        this.PreviousOdo = PreviousOdo;
        this.OdoLimit = OdoLimit;
        this.OdometerReasonabilityConditions = OdometerReasonabilityConditions;
        this.CheckOdometerReasonable = CheckOdometerReasonable;
        this.IsFSNPUpgradable = IsFSNPUpgradable;
        this.IsTLDCall = IsTLDCall;


    }


    public String getVehicleId() {
        return VehicleId;
    }

    public void setVehicleId(String vehicleId) {
        VehicleId = vehicleId;
    }

    public String getResponceMessage() {
        return ResponceMessage;
    }

    public void setResponceMessage(String responceMessage) {
        ResponceMessage = responceMessage;
    }

    public String getResponceText() {
        return ResponceText;
    }

    public void setResponceText(String responceText) {
        ResponceText = responceText;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getMinLimit() {
        return MinLimit;
    }

    public void setMinLimit(String minLimit) {
        MinLimit = minLimit;
    }

    public String getSiteId() {
        return SiteId;
    }

    public void setSiteId(String siteId) {
        SiteId = siteId;
    }

    public String getPulseRatio() {
        return PulseRatio;
    }

    public void setPulseRatio(String pulseRatio) {
        PulseRatio = pulseRatio;
    }

    public String getPersonId() {
        return PersonId;
    }

    public void setPersonId(String personId) {
        PersonId = personId;
    }

    public String getFuelTypeId() {
        return FuelTypeId;
    }

    public void setFuelTypeId(String fuelTypeId) {
        FuelTypeId = fuelTypeId;
    }

    public String getPhoneNumber() {
        return PhoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        PhoneNumber = phoneNumber;
    }

    public String getServerDate() {
        return ServerDate;
    }

    public void setServerDate(String serverDate) {
        ServerDate = serverDate;
    }

    public String getPumpOnTime() {
        return PumpOnTime;
    }

    public void setPumpOnTime(String pumpOnTime) {
        PumpOnTime = pumpOnTime;
    }

    public String getPumpOffTime() {
        return PumpOffTime;
    }

    public void setPumpOffTime(String pumpOffTime) {
        PumpOffTime = pumpOffTime;
    }

    public String getPulserStopTime() {
        return PulserStopTime;
    }

    public void setPulserStopTime(String pulserStopTime) {
        PulserStopTime = pulserStopTime;
    }

    public String getTransactionId() {
        return TransactionId;
    }

    public void setTransactionId(String transactionId) {
        TransactionId = transactionId;
    }

    public String getFirmwareVersion() {
        return FirmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        FirmwareVersion = firmwareVersion;
    }

    public String getFilePath() {
        return FilePath;
    }

    public void setFilePath(String filePath) {
        FilePath = filePath;
    }

    public String getFOBNumber() {
        return FOBNumber;
    }

    public void setFOBNumber(String FOBNumber) {
        this.FOBNumber = FOBNumber;
    }

    public String getCompany() {
        return Company;
    }

    public void setCompany(String company) {
        Company = company;
    }

    public String getLocation() {
        return Location;
    }

    public void setLocation(String location) {
        Location = location;
    }

    public String getPersonName() {
        return PersonName;
    }

    public void setPersonName(String personName) {
        PersonName = personName;
    }

    public String getPrinterName() {
        return PrinterName;
    }

    public void setPrinterName(String printerName) {
        PrinterName = printerName;
    }

    public String getPrinterMacAddress() {
        return PrinterMacAddress;
    }

    public void setPrinterMacAddress(String printerMacAddress) {
        PrinterMacAddress = printerMacAddress;
    }

    public String getVehicleSum() {
        return VehicleSum;
    }

    public void setVehicleSum(String vehicleSum) {
        VehicleSum = vehicleSum;
    }

    public String getDeptSum() {
        return DeptSum;
    }

    public void setDeptSum(String deptSum) {
        DeptSum = deptSum;
    }

    public String getVehPercentage() {
        return VehPercentage;
    }

    public void setVehPercentage(String vehPercentage) {
        VehPercentage = vehPercentage;
    }

    public String getDeptPercentage() {
        return DeptPercentage;
    }

    public void setDeptPercentage(String deptPercentage) {
        DeptPercentage = deptPercentage;
    }

    public String getSurchargeType() {
        return SurchargeType;
    }

    public void setSurchargeType(String surchargeType) {
        SurchargeType = surchargeType;
    }

    public String getProductPrice() {
        return ProductPrice;
    }

    public void setProductPrice(String productPrice) {
        ProductPrice = productPrice;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getVehicleNumber() {
        return VehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        VehicleNumber = vehicleNumber;
    }

    public String getRequireManualOdo() {
        return RequireManualOdo;
    }

    public void setRequireManualOdo(String requireManualOdo) {
        RequireManualOdo = requireManualOdo;
    }


    public String getPreviousOdo() {
        return PreviousOdo;
    }

    public void setPreviousOdo(String previousOdo) {
        PreviousOdo = previousOdo;
    }

    public String getOdoLimit() {
        return OdoLimit;
    }

    public void setOdoLimit(String odoLimit) {
        OdoLimit = odoLimit;
    }

    public String getOdometerReasonabilityConditions() {
        return OdometerReasonabilityConditions;
    }

    public void setOdometerReasonabilityConditions(String odometerReasonabilityConditions) {
        OdometerReasonabilityConditions = odometerReasonabilityConditions;
    }

    public String getCheckOdometerReasonable() {
        return CheckOdometerReasonable;
    }

    public void setCheckOdometerReasonable(String checkOdometerReasonable) {
        CheckOdometerReasonable = checkOdometerReasonable;
    }

    public String getIsFSNPUpgradable() {
        return IsFSNPUpgradable;
    }

    public void setIsFSNPUpgradable(String isFSNPUpgradable) {
        IsFSNPUpgradable = isFSNPUpgradable;
    }

    public String getIsTLDCall() {
        return IsTLDCall;
    }

    public void setIsTLDCall(String isTLDCall) {
        IsTLDCall = isTLDCall;
    }


}