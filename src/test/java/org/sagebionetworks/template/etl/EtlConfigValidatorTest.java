package org.sagebionetworks.template.etl;

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
        etlDescriptor.setDescription("test");
        etlDescriptor.setName("test");
        etlDescriptor.setScriptLocation("S3://fakeBucket/");
        etlDescriptor.setDestinationPath("S3://destination/");
        etlDescriptor.setSourcePath("S3://source/");
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
        etlDescriptor.setDescription("test");
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
        etlDescriptor.setDescription("test");
        etlDescriptor.setScriptLocation("fakeScriptPath");
        etlDescriptor.setScriptName("abc.py");
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
        etlDescriptor.setDescription("test");
        etlDescriptor.setScriptLocation("fakeScriptPath");
        etlDescriptor.setScriptName("abc.py");
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
        etlDescriptor.setDescription("test");
        etlDescriptor.setScriptLocation("fakeScriptPath");
        etlDescriptor.setScriptName("abc.py");
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
    public void testEtlConfigWithOutDescription() {
        List<EtlDescriptor> etlDescriptors = new ArrayList<>();
        EtlDescriptor etlDescriptor = new EtlDescriptor();
        etlDescriptor.setName("abc");
        etlDescriptor.setScriptLocation("fakeScriptPath");
        etlDescriptor.setSourcePath("sourcePath");
        etlDescriptor.setDestinationPath("destinationPath");
        etlDescriptor.setDestinationFileFormat("json");
        etlDescriptor.setDescription(null);
        etlDescriptors.add(etlDescriptor);

        when(etlConfig.getEtlDescriptors()).thenReturn(etlDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl job description cannot be empty", errorMessage);
    }

    @Test
    public void testEtlConfigWithOutScriptName() {
        List<EtlDescriptor> etlDescriptors = new ArrayList<>();
        EtlDescriptor etlDescriptor = new EtlDescriptor();
        etlDescriptor.setName("abc");
        etlDescriptor.setDescription("test");
        etlDescriptor.setScriptLocation("fakeScriptPath");
        etlDescriptor.setScriptName("");

        etlDescriptors.add(etlDescriptor);

        when(etlConfig.getEtlDescriptors()).thenReturn(etlDescriptors);

        //call under test
        String errorMessage = assertThrows(IllegalStateException.class, () -> {
            etlConfigValidator.validate();
        }).getMessage();
        assertEquals("The etl job script name cannot be empty", errorMessage);
    }
}
