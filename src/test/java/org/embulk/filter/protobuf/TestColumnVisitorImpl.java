package org.embulk.filter.protobuf;

import org.embulk.EmbulkTestRuntime;
import org.embulk.filter.protobuf.ProtobufFilterPlugin.PageOutputImpl;
import org.embulk.filter.protobuf.ProtobufFilterPlugin.PluginTask;
import org.embulk.spi.Page;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.PageTestUtils;
import org.embulk.spi.Schema;
import org.embulk.spi.TestPageBuilderReader.MockPageOutput;
import org.embulk.spi.type.Types;
import org.embulk.spi.util.Pages;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.embulk.filter.protobuf.TestProtobufFilterPlugin.taskFromYamlString;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

public class TestColumnVisitorImpl
{
    @Rule
    public EmbulkTestRuntime runtime;

    private String protobufJarPath;

    public TestColumnVisitorImpl()
    {
        this.runtime = new EmbulkTestRuntime();

        String pluginBasePath = new File(".").getAbsoluteFile().getParent();
        this.protobufJarPath = String.format(
            "%s/example/AddressBookProtosProto3Syntax.jar",
            pluginBasePath);
    }

    private List<Object[]> filter(
            PluginTask task, Schema inputSchema, Object... objects)
    {
        MockPageOutput output = new MockPageOutput();
        Schema outputSchema = inputSchema;
        PageBuilder pageBuilder = new PageBuilder(
            runtime.getBufferAllocator(), outputSchema, output);
        PageReader pageReader = new PageReader(inputSchema);
        ColumnVisitorImpl visitor = new ColumnVisitorImpl(
            task, pageReader, pageBuilder);

        List<Page> pages = PageTestUtils.buildPage(
            runtime.getBufferAllocator(), inputSchema, objects);
        PageOutput mockPageOutput = new PageOutputImpl(
            pageReader, pageBuilder, outputSchema, visitor);
        for (Page page : pages) {
            mockPageOutput.add(page);
        }
        mockPageOutput.finish();
        mockPageOutput.close();
        return Pages.toObjects(outputSchema, output.pages);
    }

    @Test
    public void testExecuteTask_serialize()
    {
        PluginTask task = taskFromYamlString(
            "type: protobuf",
            "serialize: true",
            "encoding: Base64",
            "protobuf_jar_path: " + protobufJarPath,
            "columns:",
            "  - {name: to serialize, message: com.example.tutorial.AddressBookProtos$Person}"
        );
        Schema inputSchema = Schema.builder()
            .add("to serialize", Types.STRING)
            .build();
        List<Object[]> records = filter(task, inputSchema,
            // generated from proto2-syntax .proto
            "{\"name\":\"John Doe\",\"id\":1234,\"email\":\"jdoe@example.com\",\"phone\":[{\"number\":\"111-0000\",\"type\":\"MOBILE\"},{\"number\":\"555-4321\",\"type\":\"HOME\"}]}",
            // generated from proto3-syntax .proto
            "{\"name\":\"John Doe\",\"id\":1234,\"email\":\"jdoe@example.com\",\"phone\":[{\"number\":\"111-0000\"},{\"number\":\"555-4321\",\"type\":\"HOME\"}]}"
        );
        assertEquals(2, records.size());
        String expected = "CghKb2huIERvZRDSCRoQamRvZUBleGFtcGxlLmNvbSIKCggxMTEtMDAwMCIMCgg1NTUtNDMyMRAB";
        assertEquals(expected, records.get(0)[0]);
        assertEquals(expected, records.get(1)[0]);
    }

    @Test
    public void testExecuteTask_deserialize()
    {
        PluginTask task = taskFromYamlString(
            "type: protobuf",
            "deserialize: true",
            "encoding: Base64",
            "protobuf_jar_path: " + protobufJarPath,
            "columns:",
            "  - {name: to deserialize, message: com.example.tutorial.AddressBookProtos$Person}"
        );
        Schema inputSchema = Schema.builder()
            .add("to deserialize", Types.STRING)
            .build();
        List<Object[]> records = filter(
            task, inputSchema,
            // generated from proto2-syntax .proto
            "CghKb2huIERvZRDSCRoQamRvZUBleGFtcGxlLmNvbSIMCggxMTEtMDAwMBAAIgwKCDU1NS00MzIxEAE=",
            // generated from proto3-syntax .proto
            "CghKb2huIERvZRDSCRoQamRvZUBleGFtcGxlLmNvbSIKCggxMTEtMDAwMCIMCgg1NTUtNDMyMRAB"
        );
        assertEquals(2, records.size());
        String expected = "{\"name\":\"John Doe\",\"id\":1234,\"email\":\"jdoe@example.com\",\"phone\":[{\"number\":\"111-0000\"},{\"number\":\"555-4321\",\"type\":\"HOME\"}]}";
        assertEquals(expected, records.get(0)[0]);
        assertEquals(expected, records.get(1)[0]);
    }
}
