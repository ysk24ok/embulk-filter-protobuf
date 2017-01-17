package org.embulk.filter.protobuf;

import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigSource;
import org.embulk.filter.protobuf.ProtobufFilterPlugin.PluginTask;
import org.embulk.spi.Exec;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfigException;
import org.embulk.spi.type.Types;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;

public class TestProtobufFilterPlugin
{
    @Rule
    public EmbulkTestRuntime runtime;

    @Rule
    public ExpectedException thrown;

    private String protobufJarPath;
    private ProtobufFilterPlugin plugin;
    private Schema defaultInputSchema;

    public TestProtobufFilterPlugin()
    {
        this.runtime = new EmbulkTestRuntime();
        this.thrown = ExpectedException.none();

        String pluginBasePath = new File(".").getAbsoluteFile().getParent();
        this.protobufJarPath = String.format(
            "%s/example/AddressBookProtosProto3Syntax.jar",
            pluginBasePath);
    }

    @Before
    public void createResources()
    {
        this.plugin = new ProtobufFilterPlugin();
        Schema defaultInputSchema = Schema.builder()
            .add("to serialize", Types.STRING)
            .build();
    }

    public static PluginTask taskFromYamlString(String... lines)
    {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append("\n");
        }
        String yamlString = builder.toString();

        ConfigLoader loader = new ConfigLoader(Exec.getModelManager());
        ConfigSource config = loader.fromYamlString(yamlString);
        return config.loadConfig(PluginTask.class);
    }

    @Test
    public void testValidate_bothTrue()
    {
        // 'serialize: true' and 'deserilize: true'
        // TODO: expect ConfigException,
        //       but somehow get java.util.concurrent.ExecutionException
        //thrown.expect(ConfigException.class);
        thrown.expectMessage("Specify either 'serialize: true' or 'deserialize: true'.");
        PluginTask task = taskFromYamlString(
            "type: protobuf",
            "serialize: true",
            "deserialize: true",
            "encoding: Base64",
            "protobuf_jar_path: " + protobufJarPath,
            "columns:",
            "  - {name: to serialize, message: com.example.tutorial.AddressBookProtos$Person}"
        );
        plugin.validate(task, defaultInputSchema);
    }

    @Test
    public void testValidate_bothFalse()
    {
        // 'serialize: false' and 'deserilize: false'
        //thrown.expect(ConfigException.class);
        thrown.expectMessage("Specify either 'serialize: true' or 'deserialize: true'.");
        PluginTask task = taskFromYamlString(
            "type: protobuf",
            "encoding: Base64",
            "protobuf_jar_path: " + protobufJarPath,
            "columns:",
            "  - {name: to serialize, message: com.example.tutorial.AddressBookProtos$Person}"
        );
        plugin.validate(task, defaultInputSchema);
    }

    @Test
    public void testValidate_encoding()
    {
        //thrown.expect(ConfigException.class);
        thrown.expectMessage("Specify 'encoding: Base64'.");
        PluginTask task = taskFromYamlString(
            "type: protobuf",
            "serialize: true",
            "encoding: Base1",
            "protobuf_jar_path: " + protobufJarPath,
            "columns:",
            "  - {name: to serialize, message: com.example.tutorial.AddressBookProtos$Person}"
        );
        plugin.validate(task, defaultInputSchema);
    }

    @Test
    public void testValidate_protobufJarPathDoesNotExist()
    {
        //thrown.expect(ConfigException.class);
        thrown.expectMessage("The jar file does not exist.");
        PluginTask task = taskFromYamlString(
            "type: protobuf",
            "serialize: true",
            "encoding: Base64",
            "protobuf_jar_path: ./A.jar",
            "columns:",
            "  - {name: to serialize, message: com.example.tutorial.AddressBookProtos$Person}"
        );
        plugin.validate(task, defaultInputSchema);
    }

    @Test
    public void testValidate_inputColumnDoesNotExist()
    {
        String colName = "to serialize";
        //thrown.expect(SchemaConfigException.class);
        thrown.expectMessage(String.format(
            "Column '%s' is not found", colName));
        PluginTask task = taskFromYamlString(
            "type: protobuf",
            "serialize: true",
            "encoding: Base64",
            "protobuf_jar_path: " + protobufJarPath,
            "columns:",
            "  - {name: " + colName + ", message: com.example.tutorial.AddressBookProtos$Person}"
        );
        Schema customInputSchema = Schema.builder()
            .add("to deserialize", Types.STRING)
            .build();
        plugin.validate(task, customInputSchema);
    }

    @Test
    public void testValidate_invalidTypeOfInputColumn()
    {
        //thrown.expect(ConfigException.class);
        thrown.expectMessage("Type of input columns must be string.");
        PluginTask task = taskFromYamlString(
            "type: protobuf",
            "serialize: true",
            "encoding: Base64",
            "protobuf_jar_path: " + protobufJarPath,
            "columns:",
            "  - {name: to serialize, message: com.example.tutorial.AddressBookProtos$Person}"
        );
        Schema customInputSchema = Schema.builder()
            .add("to serialize", Types.BOOLEAN)
            .build();
        plugin.validate(task, customInputSchema);
    }
}
