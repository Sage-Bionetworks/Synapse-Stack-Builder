package org.sagebionetworks.template.etl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EtlConfigValidatorTest {
    @InjectMocks
    private EtlConfigValidator etlConfigValidator;
    @Mock
    private EtlConfig etlConfig;


    @Test
    public void testAllValidation() {
        List<EtlDescriptor> etlDescriptors = new ArrayList<>();
        EtlDescriptor etlDescriptor = new EtlDescriptor();
        etlDescriptors.add(etlDescriptor);
        etlDescriptor.setName("test");
        etlDescriptor.setScriptLocation("S3://fakeBucket/");
        etlDescriptor.setDestinationPath("S3://destination/");
        etlDescriptor.setSourcePath("S3://source/");
        Set<String> buckets = new HashSet<>();
        buckets.add("S3://bucket1");
        buckets.add("S3://bucket2");
        etlDescriptor.setBuckets(buckets);
        etlConfig.setEtlDescriptors(etlDescriptors);

        //call under test
        etlConfigValidator.validate();
    }

    @Test
    public void testEtlConfigWithOutName() {
        List<EtlDescriptor> etlDescriptors = new ArrayList<>();
        EtlDescriptor etlDescriptor = new EtlDescriptor();
        etlDescriptor.setName(" ");
        etlDescriptors.add(etlDescriptor);

        when(etlConfig.getEtlDescriptors()).thenReturn(etlDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl job name cannot be empty", errorMessage);
    }

    @Test
    public void testEtlConfigWithOutScript() {
        List<EtlDescriptor> etlDescriptors = new ArrayList<>();
        EtlDescriptor etlDescriptor = new EtlDescriptor();
        etlDescriptor.setName("abc");
        etlDescriptor.setScriptLocation("");
        etlDescriptors.add(etlDescriptor);

        when(etlConfig.getEtlDescriptors()).thenReturn(etlDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl job script location cannot be empty", errorMessage);
    }

    @Test
    public void testEtlConfigWithOutSourcePath() {
        List<EtlDescriptor> etlDescriptors = new ArrayList<>();
        EtlDescriptor etlDescriptor = new EtlDescriptor();
        etlDescriptor.setName("abc");
        etlDescriptor.setScriptLocation("fakeScriptPath");
        etlDescriptor.setSourcePath("");
        etlDescriptors.add(etlDescriptor);

        when(etlConfig.getEtlDescriptors()).thenReturn(etlDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl s3 source path cannot be empty", errorMessage);
    }

    @Test
    public void testEtlConfigWithOutDestinationPath() {
        List<EtlDescriptor> etlDescriptors = new ArrayList<>();
        EtlDescriptor etlDescriptor = new EtlDescriptor();
        etlDescriptor.setName("abc");
        etlDescriptor.setScriptLocation("fakeScriptPath");
        etlDescriptor.setSourcePath("sourcePath");
        etlDescriptor.setDestinationPath("");
        etlDescriptors.add(etlDescriptor);

        when(etlConfig.getEtlDescriptors()).thenReturn(etlDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl s3 destination path cannot be empty", errorMessage);
    }

    @Test
    public void testEtlConfigWithOutDestinationFileFormat() {
        List<EtlDescriptor> etlDescriptors = new ArrayList<>();
        EtlDescriptor etlDescriptor = new EtlDescriptor();
        etlDescriptor.setName("abc");
        etlDescriptor.setScriptLocation("fakeScriptPath");
        etlDescriptor.setSourcePath("sourcePath");
        etlDescriptor.setDestinationPath("destinationPath");
        etlDescriptor.setDestinationFileFormat("");
        etlDescriptors.add(etlDescriptor);

        when(etlConfig.getEtlDescriptors()).thenReturn(etlDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl s3 destination file format cannot be empty", errorMessage);
    }

    @Test
    public void testEtlConfigWithOutBucket() {
        List<EtlDescriptor> etlDescriptors = new ArrayList<>();
        EtlDescriptor etlDescriptor = new EtlDescriptor();
        etlDescriptor.setName("abc");
        etlDescriptor.setScriptLocation("fakeScriptPath");
        etlDescriptor.setSourcePath("sourcePath");
        etlDescriptor.setDestinationPath("destinationPath");
        etlDescriptor.setDestinationFileFormat("json");
        etlDescriptor.setBuckets(new HashSet<>());
        etlDescriptors.add(etlDescriptor);

        when(etlConfig.getEtlDescriptors()).thenReturn(etlDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl job bucket list cannot be empty", errorMessage);
    }
}
