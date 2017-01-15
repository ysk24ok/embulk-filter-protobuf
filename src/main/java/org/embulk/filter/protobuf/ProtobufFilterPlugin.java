package org.embulk.filter.protobuf;

import com.google.common.base.Optional;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.Exec;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.type.Types;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ProtobufFilterPlugin implements FilterPlugin
{
    public interface PluginTask extends Task
    {
        @Config("serialize")
        @ConfigDefault("false")
        public Optional<Boolean> getDoSerialize();

        @Config("deserialize")
        @ConfigDefault("false")
        public Optional<Boolean> getDoDeserialize();

        @Config("encoding")
        public String getEncoding();

        @Config("protobuf_jar_path")
        public String getProtobufJarPath();

        @Config("columns")
        public List<ColumnTask> getColumns();
    }

    public interface ColumnTask extends Task
    {
        @Config("name")
        public String getName();

        @Config("message")
        public String getMessage();
    }

    public void validate(PluginTask pluginTask, Schema inputSchema)
    {
        // validate 'serialize' and 'deserialize' in PluginTask
        boolean doSerialize = pluginTask.getDoSerialize().get();
        boolean doDeserialize = pluginTask.getDoDeserialize().get();
        boolean bothTrue = doSerialize && doDeserialize;
        boolean bothFalse = !doSerialize && !doDeserialize;
        if (bothTrue || bothFalse) {
            String errMsg = "Specify either 'serialize: true' or 'deserialize: true'.";
            throw new ConfigException(errMsg);
        }
        // validate 'encoding' in PluginTask
        String[] allowedEncordings = {"Base64"};
        String encoding = pluginTask.getEncoding();
        if (!Arrays.asList(allowedEncordings).contains(encoding)) {
            String errMsg = "Specify 'encoding: Base64'.";
            throw new ConfigException(errMsg);
        }
        // validate 'protobuf_jar_path' in PluginTask
        Path protobufJarPath = Paths.get(pluginTask.getProtobufJarPath());
        if (!protobufJarPath.toFile().exists()) {
            String errMsg = "The jar file does not exist.";
            throw new ConfigException(errMsg);
        }
        // validate 'name' in ColumnTask
        for (ColumnTask colTask : pluginTask.getColumns()) {
            // throws exception when the column does not exist
            Column column = inputSchema.lookupColumn(colTask.getName());
            // TODO: accept both STRING and JSON type when 'serilialize': true
            if (!Types.STRING.equals(column.getType())) {
                String errMsg = "Type of input columns must be string.";
                throw new ConfigException(errMsg);
            }
        }
    }

    @Override
    public void transaction(ConfigSource config, Schema inputSchema,
            FilterPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        validate(task, inputSchema);
        Schema outputSchema = inputSchema;
        control.run(task.dump(), outputSchema);
    }

    @Override
    public PageOutput open(TaskSource taskSource, Schema inputSchema,
            Schema outputSchema, PageOutput output)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        PageBuilder pageBuilder = new PageBuilder(
            Exec.getBufferAllocator(), outputSchema, output);
        PageReader pageReader = new PageReader(inputSchema);
        ColumnVisitorImpl visitor = new ColumnVisitorImpl(
            task, pageReader, pageBuilder);

        return new PageOutputImpl(
            pageReader, pageBuilder, outputSchema, visitor);
    }

    public static class PageOutputImpl implements PageOutput
    {
        private PageReader pageReader;
        private PageBuilder pageBuilder;
        private Schema outputSchema;
        private ColumnVisitorImpl visitor;

        PageOutputImpl(PageReader pageReader, PageBuilder pageBuilder, Schema outputSchema, ColumnVisitorImpl visitor)
        {
            this.pageReader = pageReader;
            this.pageBuilder = pageBuilder;
            this.outputSchema = outputSchema;
            this.visitor = visitor;
        }

        @Override
        public void add(Page page)
        {
            pageReader.setPage(page);
            while (pageReader.nextRecord()) {
                outputSchema.visitColumns(visitor);
                pageBuilder.addRecord();
            }
        }

        @Override
        public void finish()
        {
            pageBuilder.finish();
        }

        @Override
        public void close()
        {
            pageBuilder.close();
        }
    };
}
