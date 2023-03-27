package org.sagebionetworks.template.datawarehouse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EtlJobConfigValidatorTest {
    @InjectMocks
    private EtlJobConfigValidator etlJobConfigValidator;
    @Mock
    private EtlJobConfig etlJobConfig;


    @Test
    public void testAllValidation() {
        List<EtlJobDescriptor> etlJobDescriptors = new ArrayList<>();
        EtlJobDescriptor etlJobDescriptor = new EtlJobDescriptor();
        etlJobDescriptors.add(etlJobDescriptor);
        etlJobDescriptor.setDescription("test");
        etlJobDescriptor.setName("test");
        etlJobDescriptor.setScriptLocation("S3://fakeBucket/");
        etlJobDescriptor.setSourcePath("S3://source/");
        etlJobConfig.setEtlJobDescriptors(etlJobDescriptors);

        //call under test
        etlJobConfigValidator.validate();
    }

    @Test
    public void testEtlConfigWithOutName() {
        List<EtlJobDescriptor> etlJobDescriptors = new ArrayList<>();
        EtlJobDescriptor etlJobDescriptor = new EtlJobDescriptor();
        etlJobDescriptor.setName(" ");
        etlJobDescriptors.add(etlJobDescriptor);

        when(etlJobConfig.getEtlJobDescriptors()).thenReturn(etlJobDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlJobConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl job name cannot be empty", errorMessage);
    }

    @Test
    public void testEtlConfigWithOutScript() {
        List<EtlJobDescriptor> etlJobDescriptors = new ArrayList<>();
        EtlJobDescriptor etlJobDescriptor = new EtlJobDescriptor();
        etlJobDescriptor.setName("abc");
        etlJobDescriptor.setDescription("test");
        etlJobDescriptor.setScriptLocation("");
        etlJobDescriptors.add(etlJobDescriptor);

        when(etlJobConfig.getEtlJobDescriptors()).thenReturn(etlJobDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlJobConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl job script location cannot be empty", errorMessage);
    }

    @Test
    public void testEtlConfigWithOutSourcePath() {
        List<EtlJobDescriptor> etlJobDescriptors = new ArrayList<>();
        EtlJobDescriptor etlJobDescriptor = new EtlJobDescriptor();
        etlJobDescriptor.setName("abc");
        etlJobDescriptor.setDescription("test");
        etlJobDescriptor.setScriptLocation("fakeScriptPath");
        etlJobDescriptor.setScriptName("abc.py");
        etlJobDescriptor.setSourcePath("");
        etlJobDescriptors.add(etlJobDescriptor);

        when(etlJobConfig.getEtlJobDescriptors()).thenReturn(etlJobDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlJobConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl s3 source path cannot be empty", errorMessage);
    }

    @Test
    public void testEtlConfigWithOutDescription() {
        List<EtlJobDescriptor> etlJobDescriptors = new ArrayList<>();
        EtlJobDescriptor etlJobDescriptor = new EtlJobDescriptor();
        etlJobDescriptor.setName("abc");
        etlJobDescriptor.setScriptLocation("fakeScriptPath");
        etlJobDescriptor.setSourcePath("sourcePath");
        etlJobDescriptor.setDescription(null);
        etlJobDescriptors.add(etlJobDescriptor);

        when(etlJobConfig.getEtlJobDescriptors()).thenReturn(etlJobDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlJobConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl job description cannot be empty", errorMessage);
    }

    @Test
    public void testEtlConfigWithOutScriptName() {
        List<EtlJobDescriptor> etlJobDescriptors = new ArrayList<>();
        EtlJobDescriptor etlJobDescriptor = new EtlJobDescriptor();
        etlJobDescriptor.setName("abc");
        etlJobDescriptor.setDescription("test");
        etlJobDescriptor.setScriptLocation("fakeScriptPath");
        etlJobDescriptor.setScriptName("");

        etlJobDescriptors.add(etlJobDescriptor);

        when(etlJobConfig.getEtlJobDescriptors()).thenReturn(etlJobDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlJobConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl job script name cannot be empty", errorMessage);
    }
}
