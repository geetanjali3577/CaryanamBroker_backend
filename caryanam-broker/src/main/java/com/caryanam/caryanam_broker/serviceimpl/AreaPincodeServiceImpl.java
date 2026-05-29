
package com.caryanam.caryanam_broker.serviceimpl;

import com.caryanam.caryanam_broker.entity.AreaPincode;
import com.caryanam.caryanam_broker.repository.AreaPincodeRepository;
import com.caryanam.caryanam_broker.service.AreaPincodeService;

import org.apache.poi.ss.usermodel.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class AreaPincodeServiceImpl
        implements AreaPincodeService {

    @Autowired
    private AreaPincodeRepository
            areaPincodeRepository;

    @Override
    public String uploadExcel(
            MultipartFile file) {

        try {

            InputStream inputStream =
                    file.getInputStream();

            Workbook workbook =
                    WorkbookFactory.create(inputStream);

            Sheet sheet =
                    workbook.getSheetAt(0);

            for (Row row : sheet) {

                // SKIP HEADER
                if (row.getRowNum() == 0) {
                    continue;
                }

                Cell cityCell =
                        row.getCell(0);

                Cell areaCell =
                        row.getCell(1);

                Cell pincodeCell =
                        row.getCell(2);

                Cell nearbyCell =
                        row.getCell(3);

                if (cityCell == null ||
                        areaCell == null ||
                        pincodeCell == null) {

                    continue;
                }

                String city =
                        cityCell
                                .toString()
                                .trim();

                String area =
                        areaCell
                                .toString()
                                .trim();

                String pincode =
                        pincodeCell
                                .toString()
                                .replace(".0", "")
                                .trim();

                String nearby = "";

                if (nearbyCell != null) {

                    nearby =
                            nearbyCell
                                    .toString()
                                    .trim();
                }

                AreaPincode existing =
                        areaPincodeRepository
                                .findByCityIgnoreCaseAndAreaIgnoreCase(
                                        city,
                                        area
                                );

                if (existing != null) {
                    continue;
                }

                AreaPincode areaPincode =
                        new AreaPincode();

                areaPincode.setCity(city);

                areaPincode.setArea(area);

                areaPincode.setPincode(pincode);

                areaPincode.setNearBy(nearby);

                // SAME PINCODE SAVE
                areaPincode.setNearbyPincode(
                        pincode
                );

                areaPincodeRepository
                        .save(areaPincode);
            }

            workbook.close();

            return "Excel uploaded successfully";

        } catch (Exception e) {

            e.printStackTrace();

            return "Failed to upload excel : "
                    + e.getMessage();
        }
    }

    @Override
    public List<String> getNearbyData(
            String nearbyPincode) {

        List<AreaPincode> list =
                areaPincodeRepository
                        .findByNearbyPincode(
                                nearbyPincode
                        );

        List<String> response =
                new ArrayList<>();

        for (AreaPincode areaPincode : list) {

            if (areaPincode.getNearBy() != null &&
                    !areaPincode.getNearBy().isEmpty()) {

                String[] nearbyArray =
                        areaPincode
                                .getNearBy()
                                .split(",");

                for (String nearby : nearbyArray) {

                    response.add(
                            nearby.trim()
                    );
                }
            }
        }

        return response;
    }
}