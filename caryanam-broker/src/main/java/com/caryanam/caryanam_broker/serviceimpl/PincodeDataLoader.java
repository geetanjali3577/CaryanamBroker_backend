package com.caryanam.caryanam_broker.serviceimpl;

import com.caryanam.caryanam_broker.service.AreaPincodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.mock.web.MockMultipartFile;
@Component
@RequiredArgsConstructor
public class PincodeDataLoader implements CommandLineRunner {

    private final AreaPincodeService pincodeService;

    @Override
    public void run(String... args) throws Exception {

        if (pincodeService.count() == 0) {

            ClassPathResource resource =
                    new ClassPathResource("pincode/Pune_PCMC_Area_Pincode.xlsx");

            MultipartFile file =
                    new MockMultipartFile(
                            "file",
                            resource.getFilename(),
                            MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            resource.getInputStream());

            pincodeService.uploadExcel(file);

            System.out.println("Pincode Data Imported Successfully");
        }
    }
}
