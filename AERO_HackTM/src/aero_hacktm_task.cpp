/*! \file ****************************************************************************************************************************

  COMPANY:           Continental Automotive: Advanced Driver Assistance Systems - Advanced Engineering (for HackTM participants)

  PROJECT:           ...

  MODULNAME:         aero_hacktm_task.cpp

  DESCRIPTION:       HackTM module reading inputs from from Stereo Camera, Long Range Radar (both front center), Vehicle Dynamics
					 and GPS data.

  CREATION DATE:     28.03.2016

  VERSION:           $Revision: 1.1 $

  -------------------------------------------------
  About ADAS Advanced Engineering:
  -------------------------------------------------
	http://tinyurl.com/ContiAdasAeInterview
	http://tinyurl.com/ContiAutomatedDriving

	And some projects we're working on in Timisoara:
	http://continental-automated-driving.com/Navigation/Enablers/Comprehensive-Environment-Model
	http://continental-automated-driving.com/Navigation/Driving-Functions/Cruising-Chauffeur


*/

/*********************************************************************************************************************************/
// The example below reads specific input data provided by the sensors. Users will insert their code in the "wxEVT_STEP" function.
// "wxEVT_STEP" is called cyclically.
//
// Information provided:
//---------------------
//      - Traffic participants data from Stereo Camera (SCAM) Front Center (FC)    => SCAM_FC
//      - Traffic participants data from Long Range Radar (LRR) Front Center (FC)  => LLR_FC
//      - Vehicle dynamics data from a separate ECU                                => VEH_DYN_Data
//      - GPS data                                                                 => GPS_Data
/*********************************************************************************************************************************/

//////////////////////////////////////////////////////////
//           headers
//////////////////////////////////////////////////////////
#include "aero_hacktm_task.h"
#include "VehicleConfig.h"
#include "LogWriterModPlugin.h"
#include <iostream>
#include <ctime>
#include <fstream>
#include <string>
#include <string.h>


using namespace Tasks::AERO_HACK_TM_Task; // Tasks part mandatory for runtask

//////////////////////////////////////////////////////////
//           plugin header
//////////////////////////////////////////////////////////
//! Specific name of the plugin
std::string PLUGIN_NAME = "AERO_HackTM";    // mandatory/standard for runtask
//! Additional plugin description
std::string PLUGIN_DESC = "A task which integrates the HackTM user app inside a Runtask framework";    // mandatory/standard for runtask


//path to where to dump the data
std::string dumpDir = "generated_jsons";
std::ofstream outFile;
std::string dump_filename = "";
std::string lastTime = "";
int callCounter = 0;
const int MAX_ENTRIES = 5 * 60;
int entryCounter = 0;
std::string recordingId = "";

//////////////////////////////////////////////////////////
//           class CModPlugIn definition
//////////////////////////////////////////////////////////

/**
   * @brief  Default constructor.
	*
	 * @param inst_name  Name of the plugin instance.
	  * @param callback   Pointer to the central callback hook (for internal use).
	  **/
CModPlugIn::CModPlugIn(const char* inst_name, wxPlg_IntFunT callback)
	: CEvtPlugIn(inst_name, callback)
{

	//! Use the 'DEC_EVT' macro to register the callbacks (please comment out unused events)
	DEC_EVT(OBJECTEVT_OPEN, wxEVT_OPEN);
	DEC_EVT(OBJECTEVT_CLOSE, wxEVT_CLOSE);
	DEC_EVT(OBJECTEVT_CONNECT, wxEVT_CONNECT);
	DEC_EVT(OBJECTEVT_DISCONNECT, wxEVT_DISCONNECT);
	DEC_EVT(OBJECTEVT_STEP, wxEVT_STEP);
	DEC_EVT(OBJECTEVT_DRAW2D, wxEVT_DRAW2D);
	DEC_EVT(OBJECTEVT_DRAW3D, wxEVT_DRAW3D);
	DEC_EVT(OBJECTEVT_SIZE, wxEVT_SIZE);

	eCAL::Initialize(0, NULL, "HackTM Task");
	memset(&LRR_FC_TrafParticList, 0, sizeof(LRR_FC_TrafParticList));
	memset(&SCAM_FC_TrafParticList, 0, sizeof(SCAM_FC_TrafParticList));
}


/**
   * @brief Destructor.
   **/
CModPlugIn::~CModPlugIn()
{
	eCAL::Finalize();
}


//////////////////////////////////////////////////////////
//           class CModPlugIn events definition
//////////////////////////////////////////////////////////
//! Event OPEN will be called after host application has load the dll
long CModPlugIn::wxEVT_OPEN(const wxPlg_Obj* arg_obj, wxPlg_Obj* ret_obj)
{
	helpers::IniFile iniFile; arg_obj; ret_obj;

	/* Set up the log writer*/
	LogWriterModPlugin *logWriter = new LogWriterModPlugin(this);
	CLogDevice::get()->addLogWriter(logWriter);
	LOG_INFO("AERO_HackTM build %s %s", __DATE__, __TIME__);



	return(0);
};

void setup_hacktm_dump_file()
{
	/* Set up the dump file*/

	try {

		time_t rawtime;
		time(&rawtime);
		struct tm * timeinfo = localtime(&rawtime);
		char bufferTime[150];
		strftime(bufferTime, 80, "%Y%m%d%H%M%S", timeinfo);
		std::string time(bufferTime);
		std::string fileName = dumpDir + "\\drive_dump_tmp_" + time + ".json";
		dump_filename = fileName;
		LOG_INFO("trying to open file %s", dump_filename.c_str());

		outFile.open(fileName.c_str());

		if (!outFile)
		{
			LOG_ERROR("Error opening writing to ...");
		}
		else
		{
			LOG_INFO("Filedump open");
			outFile << "[";
		}
	}
	catch (const std::exception&)
	{
		LOG_ERROR("exception while opening file");
	}
}

void close_hacktm_dump_file()
{
	if (!outFile)
	{
		LOG_ERROR("exception while closing file");
	}
	else
	{
		outFile << "]";
		outFile.close();
	}
}

//! Event CLOSE will be called before host application will unload the dll
long CModPlugIn::wxEVT_CLOSE(const wxPlg_Obj* arg_obj, wxPlg_Obj* ret_obj)
{
	TextPrint2Con("wxEVT_CLOSE was called"); arg_obj; ret_obj;

	return(0);
};

//! Event CONNECT will be called after host application has gone in connect state
// e.g. wxWavE's "Network Connect" or RunTask's "Start"
long CModPlugIn::wxEVT_CONNECT(const wxPlg_Obj* arg_obj, wxPlg_Obj* ret_obj)
{
	TextPrint2Con("wxEVT_CONNECT was called"); arg_obj; ret_obj;

	mSubTP_LRR_FC = new eCAL::CProtoSubscriber<class aero_trafpartic_protobuf::AERO_t_TrafParticList_Proto>(AERO_Proto::AERO_LRR_FC_TPLChannel);
	mSubTP_SCAM_FC = new eCAL::CProtoSubscriber<class aero_trafpartic_protobuf::AERO_t_TrafParticList_Proto>(AERO_Proto::AERO_SCAM_FC_TPLChannel);
	mSub_VEH_DYN_DATA = new eCAL::CProtoSubscriber<class aero_vehicle_data_protobuf::AERO_t_VehicleDynamics_Proto>(AERO_Proto::AERO_Vehicle_Dynamics_Channel);
	mSub_GPS_DATA = new eCAL::CProtoSubscriber<class aero_gps_rmc_protobuf::AERO_t_GpsRmc_Proto>(AERO_Proto::AERO_GPS_RMC_Data_Channel);

	mSubTP_LRR_FC->AddReceiveCallback(std::bind(&CModPlugIn::AEROTP_TrafParticReceive_LRR_FC, this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3));
	mSubTP_SCAM_FC->AddReceiveCallback(std::bind(&CModPlugIn::AEROTP_TrafParticReceive_SCAM_FC, this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3));
	mSub_VEH_DYN_DATA->AddReceiveCallback(std::bind(&CModPlugIn::AEROTP_DataReceive_VEH_DYN, this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3));
	mSub_GPS_DATA->AddReceiveCallback(std::bind(&CModPlugIn::AEROTP_DataReceive_GPS, this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3));

	setup_hacktm_dump_file();

	time_t rawtime;
	time(&rawtime);
	struct tm * timeinfo = localtime(&rawtime);
	char bufferTime[150];
	strftime(bufferTime, 80, "%Y%m%d%H%M%S", timeinfo);
	std::string time(bufferTime);
	recordingId = time;

	return(0);
};


//! Event DISCONNECT will be called before host application is going in disconnect state
// e.g. wxWavE's "Network Disconnect" or RunTask's "Stop"
long CModPlugIn::wxEVT_DISCONNECT(const wxPlg_Obj* arg_obj, wxPlg_Obj* ret_obj)
{
	arg_obj; ret_obj;

	if (mSubTP_LRR_FC != NULL)     delete mSubTP_LRR_FC;
	if (mSubTP_SCAM_FC != NULL)    delete mSubTP_SCAM_FC;
	if (mSub_VEH_DYN_DATA != NULL) delete mSub_VEH_DYN_DATA;
	if (mSub_GPS_DATA != NULL)     delete mSub_GPS_DATA;

	return(0);
};



//! Event STEP will be called every n-th ms depending from host application settings
long CModPlugIn::wxEVT_STEP(const wxPlg_Obj* arg_obj, wxPlg_Obj* ret_obj)
{
	arg_obj; ret_obj;

	LOG_INFO("AERO: HackTM Step called:");

	std::string dataJson = "{";

	/*********************************************************************************************************************************/
	// PLEASE INSERT THE BEST HACK_TM CODE HERE!
	/*********************************************************************************************************************************/
	if (VEH_DYN_Data.SpeedDisplayed.e_SignalStatus == AERO_SIGNAL_STATUS_OK)
	{
		LOG_INFO("VehDyn.Speed: %.1f", VEH_DYN_Data.SpeedDisplayed.f_Value);
		char buffer[50];
		sprintf(buffer, "%.1f", VEH_DYN_Data.SpeedDisplayed.f_Value);
		std::string speed(buffer);
		dataJson += "\"speed\":" + speed + ",";
	}

	if (VEH_DYN_Data.AccPedalPos.e_SignalStatus == AERO_SIGNAL_STATUS_OK)
	{
		LOG_INFO("VehDyn.AccPedalPos: %.1f", VEH_DYN_Data.AccPedalPos.f_Value);
		char buffer[50];
		sprintf(buffer, "%.1f", VEH_DYN_Data.AccPedalPos.f_Value);
		std::string pedal(buffer);
		dataJson += "\"pedal\":" + pedal + ",";
	}

	if (VEH_DYN_Data.EngRPM.e_SignalStatus == AERO_SIGNAL_STATUS_OK)
	{
		LOG_INFO("VehDyn.EngRPM: %.1f", VEH_DYN_Data.EngRPM.f_Value);
		char buffer[50];
		sprintf(buffer, "%.1f", VEH_DYN_Data.EngRPM.f_Value);
		std::string rpm(buffer);
		dataJson += "\"rpm\":" + rpm + ",";
	}

	if (VEH_DYN_Data.Yawrate.e_SignalStatus == AERO_SIGNAL_STATUS_OK)
	{
		LOG_INFO("VehDyn.Yawrate: %.1f", VEH_DYN_Data.Yawrate.f_Value);
		char buffer[50];
		sprintf(buffer, "%.1f", VEH_DYN_Data.Yawrate.f_Value);
		std::string yawrate(buffer);
		dataJson += "\"yawrate\":" + yawrate + ",";
	}
	else
	{
		LOG_INFO("VEH_DYN_Data not available");
	}

	if (RAD2DEG(GPS_Data.f_LatitudeRad) < 1000 && RAD2DEG(GPS_Data.f_LatitudeRad) > -1000)
	{
		LOG_INFO("GPS_Data.f_Latitude: %f", RAD2DEG(GPS_Data.f_LatitudeRad));
		char bufferLat[150];
		sprintf(bufferLat, "%10.6f", RAD2DEG(GPS_Data.f_LatitudeRad));
		std::string latitude(bufferLat);
		dataJson += "\"latitude\":" + latitude + ",";
	}
	else
	{
		std::string latitude("0");
		dataJson += "\"latitude\":" + latitude + ",";
	}

	if (RAD2DEG(GPS_Data.f_LongitudeRad) < 1000 && RAD2DEG(GPS_Data.f_LongitudeRad) > -1000)
	{
		LOG_INFO("GPS_Data.f_Longitude: %f", RAD2DEG(GPS_Data.f_LongitudeRad));
		char bufferLong[150];
		sprintf(bufferLong, "%10.6f", RAD2DEG(GPS_Data.f_LongitudeRad));
		std::string longitude(bufferLong);
		dataJson += "\"longitude\":" + longitude + ",";
	}
	else
	{
		std::string longitude("0");
		dataJson += "\"longitude\":" + longitude + ",";
	}

	if (LRR_FC_TrafParticList.u_NumTrafficParticipants > 0)
	{
		LOG_INFO("LRR_FC_TrafParticList.TrafPart[0].DynProp.Velocity.f_X: %f", LRR_FC_TrafParticList.TrafPart[0].DynProp.Velocity.f_X);
		char bufferLong[150];
		sprintf(bufferLong, "%f", LRR_FC_TrafParticList.TrafPart[0].DynProp.Velocity.f_X);
		std::string longitude(bufferLong);
		dataJson += "\"trafficx\":" + longitude + ",";

		LOG_INFO("LRR_FC_TrafParticList.TrafPart[0].DynProp.Velocity.f_Y: %f", LRR_FC_TrafParticList.TrafPart[0].DynProp.Velocity.f_Y);
		char bufferLat[150];
		sprintf(bufferLat, "%f", LRR_FC_TrafParticList.TrafPart[0].DynProp.Velocity.f_Y);
		std::string latitude(bufferLat);
		dataJson += "\"trafficy\":" + latitude + ",";
	}
	else
	{
		LOG_INFO("LRR_FC_TrafParticList.u_NumTrafficParticipants: %d", LRR_FC_TrafParticList.u_NumTrafficParticipants);
	}


	if (SCAM_FC_TrafParticList.u_NumTrafficParticipants > 0)
	{
		std::string latitudeStr = "[";

		for (int i = 0; i < SCAM_FC_TrafParticList.u_NumTrafficParticipants ; i++) {
			LOG_INFO("SCAM_FC_TrafParticList.TrafPart[%d].GeomProp.Length: %.1f", i, SCAM_FC_TrafParticList.TrafPart[i].GeomProp.Length.f_Value);
			char bufferLong[150];
			sprintf(bufferLong, "%.1f", SCAM_FC_TrafParticList.TrafPart[i].GeomProp.Length.f_Value);
			latitudeStr += std::string(bufferLong);
			if (i != (SCAM_FC_TrafParticList.u_NumTrafficParticipants - 1)) {
				latitudeStr += ",";
			}
		}
		latitudeStr += "]";

		std::string heightStr = "[";
		dataJson += "\"trafficlength\":" + latitudeStr + ",";
		for (int i = 0; i < SCAM_FC_TrafParticList.u_NumTrafficParticipants; i++) {
			LOG_INFO("SCAM_FC_TrafParticList.TrafPart[%d].GeomProp.Height: %.1f", i, SCAM_FC_TrafParticList.TrafPart[i].GeomProp.Length.f_Value);
			char bufferLong[150];
			sprintf(bufferLong, "%.1f", SCAM_FC_TrafParticList.TrafPart[i].GeomProp.Length.f_Value);
			heightStr += std::string(bufferLong);
			if (i != (SCAM_FC_TrafParticList.u_NumTrafficParticipants - 1)) {
				heightStr += ",";
			}
		}
		heightStr += "]";
		dataJson += "\"trafficheight\":" + heightStr + ",";

	}
	else
	{
		LOG_INFO("SCAM_FC_TrafParticList.u_NumTrafficParticipants: %d", SCAM_FC_TrafParticList.u_NumTrafficParticipants);
	}


	time_t rawtime;
	time(&rawtime);
	struct tm * timeinfo = localtime(&rawtime);

	char bufferTime[150];
	strftime(bufferTime, 80, "%Y%m%d%H%M%S", timeinfo);

	std::string time(bufferTime);
	if (time.compare(lastTime) != 0)
	{
		lastTime = time;
		callCounter = 0;
	}
	else
	{
		callCounter++;
	}

	sprintf(bufferTime, "%s%d", time.c_str(), callCounter);
	std::string timefinal(bufferTime);

	dataJson += "\"drivetime\":" + timefinal + ",";
	dataJson += "\"recordingid\":" + recordingId + ",";

	char bufferId[50];
	sprintf(bufferId, "%d", 1234567890);
	std::string bufid(bufferId);
	dataJson += "\"driver\":" + bufid + "";

	dataJson += "}";

	LOG_INFO("%s", dataJson.c_str());

	if (!outFile) { std::cerr << "Error writing to ..." << std::endl; }
	else
	{
		LOG_INFO("writing entry %d to %s", entryCounter, dump_filename.c_str());
		outFile << dataJson;

		entryCounter++;
		if (entryCounter % MAX_ENTRIES != 0) {
			// still need to write another line
			outFile << ",\n";
		}
		else {
			outFile << "\n";
		}
	}

	if (entryCounter % MAX_ENTRIES == 0)
	{
		LOG_INFO("dump file reseted");
		close_hacktm_dump_file();
		setup_hacktm_dump_file();
		entryCounter = 0;
	}

	LOG_INFO("----------------------------------------------\n");

	/*********************************************************************************************************************************/
	// End of DEMO code
	/*********************************************************************************************************************************/


	/* Return true to show output to hdc on the runtask, return false to ignore output*/
	return true;
};


//! Event DRAW2D will be called every n-th ms depending from host application settings
long CModPlugIn::wxEVT_DRAW2D(const wxPlg_Obj* arg_obj, wxPlg_Obj* ret_obj)
{
	arg_obj; ret_obj;
	return(0);
}


//! Event DRAW3D will be called every n-th ms depending from host application settings
long CModPlugIn::wxEVT_DRAW3D(const wxPlg_Obj* arg_obj, wxPlg_Obj* ret_obj)
{
	LOG_INFO("Got triggered in DRAW3D ..."); arg_obj; ret_obj;

	// host app wxWavE
	if (GetHostAppName() == "wxWavE") {}

	// host app RunTask
	if (GetHostAppName() == "RunTask") {}

	return(0);
}


//! This will be called when a "change size" event of a drawing tab from the RunTask application
long CModPlugIn::wxEVT_SIZE(const wxPlg_Obj* arg_obj, wxPlg_Obj* ret_obj)
{
	arg_obj; ret_obj;
	return(0);
}


void CModPlugIn::AEROTP_TrafParticReceive_LRR_FC(const char* topic_name_, const class aero_trafpartic_protobuf::AERO_t_TrafParticList_Proto& msg_, const long long time_)
{
	AEROTrafparticProto_DeSerialize(&msg_, &LRR_FC_TrafParticList); topic_name_; time_;
}


void CModPlugIn::AEROTP_TrafParticReceive_SCAM_FC(const char* topic_name_, const class aero_trafpartic_protobuf::AERO_t_TrafParticList_Proto& msg_, const long long time_)
{
	AEROTrafparticProto_DeSerialize(&msg_, &SCAM_FC_TrafParticList); topic_name_; time_;
}


void CModPlugIn::AEROTP_DataReceive_VEH_DYN(const char* topic_name_, const class aero_vehicle_data_protobuf::AERO_t_VehicleDynamics_Proto& msg_, const long long time_)
{
	AEROVehicleDataProto_DeSerializeVehicleDynamics(&msg_, &VEH_DYN_Data); topic_name_; time_;
}


void CModPlugIn::AEROTP_DataReceive_GPS(const char* topic_name_, const class aero_gps_rmc_protobuf::AERO_t_GpsRmc_Proto& msg_, const long long time_)
{
	AEROGpsRmcProto_DeSerialize(&msg_, &GPS_Data); topic_name_; time_;
}


//////////////////////////////////////////////////////////
//           class instance
//////////////////////////////////////////////////////////
//! Creates a CModPlugIn class instance (do not change)
CBasePlugIn* GetPlugIn(const char* inst_name, wxPlg_IntFunT callback)
{
	return(new CModPlugIn(inst_name, callback));
}