package org.embulk.filter.protobuf;

import com.google.common.base.Optional;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.Exec;
import org.embulk.spi.FilterPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;

import java.util.List;

public class ProtobufFilterPlugin implements FilterPlugin
{
    public interface PluginTask extends Task
    {
        @Config("serialize")
        @ConfigDefault("false")
        public Optional<Boolean> getDoSerialize();

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

    // TODO
    public void validate(PluginTask task, Schema inputSchema)
    {
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
