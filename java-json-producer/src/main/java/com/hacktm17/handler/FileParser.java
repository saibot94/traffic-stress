package com.hacktm17.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hacktm17.domain.CarJsonDO;
import com.hacktm17.dto.CarJsonDTO;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by darkg on 27-May-17.
 */
public class FileParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static long FILE_SLEEP = 2 * 1000;
    private static long BATCH_SLEEP = 2 * 1000;
    private static long DIR_SLEEP = 5 * 1000;

    private static int BATCH_SIZE = 1500;

    private HttpDataPost httpDataPost;

    public FileParser() {
        httpDataPost = new HttpDataPost();
    }

    private static long recordingId = System.currentTimeMillis();

    public static void main(String[] args) throws InterruptedException {
        PropertyConfigurator.configure(App.class.getClassLoader().getResourceAsStream("log4j.xml"));

//        String ip = args[0];
//        String port = args[1];
        if (args.length < 1) {
            LOGGER.warn(String.format("missing input: dump directory"));
            return;
        }

        boolean streaming = false;
        if (args.length == 2) {
            streaming = true;
        }

        if (!streaming) {
            BATCH_SLEEP = 0;
            FILE_SLEEP = 0;
        }


        String dumpDirectory = args[0];

        FileParser fileParser = new FileParser();

        LOGGER.info(String.format("Started with dir: %s", dumpDirectory));
        while (true) {
            LOGGER.info("crawling ... ");
            fileParser.crawlDirectory(dumpDirectory, streaming);
            LOGGER.info("sleeping ... ");
            Thread.sleep(DIR_SLEEP);
        }
    }


    private void crawlDirectory(String dumpDirectoryPath, boolean streaming) throws InterruptedException {

        File dumpDirectory = new File(dumpDirectoryPath);
        if (!dumpDirectory.isDirectory()) {
            LOGGER.error("dump directory path is not a directory");
            return;
        }

        int lastDirectory = 0;
        if (streaming) {
            lastDirectory = 1;
        }

        File[] listOfFiles = dumpDirectory.listFiles();
        Arrays.sort(listOfFiles);


        for (int i = 0; i < listOfFiles.length - lastDirectory; i++) {
            File file = listOfFiles[i];

            if (file.isDirectory())
                continue;

            String fileName = file.getName();
            if (!fileName.startsWith("drive_dump") && !fileName.startsWith("MOCK_DATA"))
                continue;

            LOGGER.info(String.format("dumping file: %s", file.getName()));

            String parentPath = file.getParent();
            String archivedFile = parentPath + File.separator + "archived" + File.separator + file.getName();

            if (FILE_SLEEP > 1000)
                Thread.sleep(FILE_SLEEP);

            processFile(file);

            try {
                FileUtils.copyFile(file, new File(archivedFile));
                file.delete();
                System.out.println("Deleted file...");

                LOGGER.info(String.format("archived to: %s", archivedFile));
            } catch (IOException e) {
                LOGGER.warn("Could not copy file to destination");
            }


        }
    }


    private void processFile(File file) throws InterruptedException {
        LOGGER.info(String.format("PROCESSING FILE %s", file.getName()));

        try {
            ObjectMapper mapper = new ObjectMapper();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            List<CarJsonDTO> carPostData = new ArrayList<>();
            String line = bufferedReader.readLine();
            while (line != null) {
                CarJsonDO carJsonData = mapper.readValue(line, CarJsonDO.class);
                CarJsonDTO dbObject = processLine(carJsonData);
                if (dbObject == null)
                    continue;

                carPostData.add(dbObject);

                if (carPostData.size() >= BATCH_SIZE) {
                    httpDataPost.insertMany(carPostData);
                    carPostData.clear();
                    if (BATCH_SLEEP > 450)
                        Thread.sleep(BATCH_SLEEP);
                }
            }

            if (carPostData.size() > 2)
                httpDataPost.insertMany(carPostData);

            LOGGER.info(String.format("carPostData written: %d", carPostData.size()));
        } catch (FileNotFoundException ex) {
            LOGGER.error(String.format("Unable to open file %s", file.getName()), ex);
        } catch (IOException ex) {
            LOGGER.error(String.format("error reading file %s", file.getName()), ex);
        }
    }

    private CarJsonDTO processLine(CarJsonDO carData) {
//        LOGGER.info(String.format("line read: %s", line));
        try {
            return processJsonNode(carData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private CarJsonDTO processJsonNode(CarJsonDO jsonNode) throws Exception {

        CarJsonDTO dto = new CarJsonDTO();
        dto.setDriver(jsonNode.getDriver());
        dto.setRpm(jsonNode.getRpm());
        if (jsonNode.getTrafficheadingcam() != null) {
            dto.setTrafficHeading(jsonNode.getTrafficheadingcam());
        }
        if (jsonNode.getTrafficlengthcam() != null) {
            dto.setTrafficLength(jsonNode.getTrafficlengthcam());
        }
        if (jsonNode.getTrafficspeedxcam() != null) {
            dto.setTrafficXSpeed(jsonNode.getTrafficspeedxcam());
        }
        if (jsonNode.getTrafficspeedycam() != null) {
            dto.setTrafficYSpeed(jsonNode.getTrafficspeedycam());
        }
        if (jsonNode.getTrafficxcam() != null) {
            dto.setTrafficXPos(jsonNode.getTrafficxcam());
        }
        if (jsonNode.getTrafficycam() != null) {
            dto.setTrafficYPos(jsonNode.getTrafficycam());
        }

        dto.setYawRate(jsonNode.getYawrate());
        dto.setPedal(jsonNode.getPedal());
        //process speed
        Double speedValue = jsonNode.getSpeed();
        if (speedValue != null) {
            double speedDoubleKmh = 3.6 * speedValue;
            dto.setSpeed(speedDoubleKmh);
        }

        if (jsonNode.getLongitude() != null) {
            //                                              longitude & latitude
            //   "geo" : { "type" : "Point", "coordinates" : [ -121.88355687, 37.44609999 ] }
            if (jsonNode.getLongitude() > 1000 || jsonNode.getLongitude() < -1000)
                return null;
            if (jsonNode.getLongitude() > 1000 || jsonNode.getLongitude() < -1000)
                return null;
            dto.setLongitude(jsonNode.getLongitude());
            dto.setLatitude(jsonNode.getLatitude());
        }

        if (jsonNode.getDrivetime() != null) {

            //unix time
            Long driveTimeValue = jsonNode.getDrivetime();
            String driveTime = driveTimeValue.toString();
            if (driveTime.length() != 15) {
                Calendar cal = new GregorianCalendar();
                cal.setTimeInMillis(System.currentTimeMillis());
                driveTime = "" + cal.get(Calendar.YEAR)
                        + (cal.get(Calendar.MONTH) > 10 ? cal.get(Calendar.MONTH) : ("0" + cal.get(Calendar.MONTH)))
                        + (cal.get(Calendar.DAY_OF_MONTH) > 10 ? cal.get(Calendar.DAY_OF_MONTH) : ("0" + cal.get(Calendar.DAY_OF_MONTH)))
                        + (cal.get(Calendar.HOUR_OF_DAY) > 10 ? cal.get(Calendar.HOUR_OF_DAY) : ("0" + cal.get(Calendar.HOUR_OF_DAY)))
                        + (cal.get(Calendar.MINUTE) > 10 ? cal.get(Calendar.MINUTE) : ("0" + cal.get(Calendar.MINUTE)))
                        + (cal.get(Calendar.SECOND) > 10 ? cal.get(Calendar.SECOND) : ("0" + cal.get(Calendar.SECOND)))
                        + cal.get(Calendar.MILLISECOND);
            }

            String year = driveTime.substring(0, 4);
            String month = driveTime.substring(4, 6);
            String day = driveTime.substring(6, 8);
            String hour = driveTime.substring(8, 10);
            String minute = driveTime.substring(10, 12);
            String second = driveTime.substring(12, 14);
            String ms = driveTime.substring(14, 15);

            int y = Integer.valueOf(year);
            int m = Integer.valueOf(month);
            int d = Integer.valueOf(day);
            int hh = Integer.valueOf(hour);
            int mm = Integer.valueOf(minute);
            int ss = Integer.valueOf(second);
            int mss = Integer.valueOf(ms);

            Calendar calendar = new GregorianCalendar(y, m, d, hh, mm, ss);
            calendar.set(Calendar.MILLISECOND, mss);
            long utc = calendar.getTimeInMillis();

            dto.setTimestamp(utc);
        }

        //("ingestiontime", System.currentTimeMillis());

        if (jsonNode.getRecordingid() == null) {
            dto.setRecordingId(recordingId);
        } else {
            dto.setRecordingId(recordingId);
        }

        return dto;
    }

}
