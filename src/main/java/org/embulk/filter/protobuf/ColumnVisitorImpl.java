package org.embulk.filter.protobuf;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;

import org.embulk.filter.protobuf.ProtobufFilterPlugin.ColumnTask;
import org.embulk.filter.protobuf.ProtobufFilterPlugin.PluginTask;

import org.embulk.plugin.PluginClassLoader;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageReader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColumnVisitorImpl implements ColumnVisitor
{
    private final PluginTask pluginTask;
    private final PageReader pageReader;
    private final PageBuilder pageBuilder;
    private final Map<String, ColumnTask> columnTaskMap;

    ColumnVisitorImpl(PluginTask task, PageReader reader, PageBuilder builder)
    {
        this.pluginTask = task;
        this.pageReader = reader;
        this.pageBuilder = builder;
        this.columnTaskMap = getColumnMap(task.getColumns());
        addProtobufJarToClasspath();
    }

    private Map<String, ColumnTask> getColumnMap(
            List<ColumnTask> columnTasks)
    {
        Map<String, ColumnTask> m = new HashMap<>();
        for (ColumnTask columnTask : columnTasks) {
            m.put(columnTask.getName(), columnTask);
        }
        return m;
    }

    private void addProtobufJarToClasspath()
    {
        PluginClassLoader loader = (PluginClassLoader) getClass().getClassLoader();
        Path protobufJarPath = Paths.get(pluginTask.getProtobufJarPath());
        loader.addPath(protobufJarPath);
    }

    private ColumnTask getColumnTask(Column column)
    {
        String colName = column.getName();
        return columnTaskMap.get(colName);
    }

    private byte[] decodeMessage(String messageAsString)
    {
        byte[] decoded = null;
        String encoding = pluginTask.getEncoding();
        if (encoding.equals("Base64")) {
            decoded = BaseEncoding.base64().decode(messageAsString);
        }
        return decoded;
    }

    private String encodeMessage(byte[] messageAsBytes)
    {
        String encoded = null;
        String encoding = pluginTask.getEncoding();
        if (encoding.equals("Base64")) {
            encoded = BaseEncoding.base64().encode(messageAsBytes);
        }
        return encoded;
    }

    private String convertMessageBytesToJson(
            byte[] messageAsBytes, String messageName)
    {
        PluginClassLoader loader = (PluginClassLoader) getClass().getClassLoader();
        // Get a message object
        // TODO: Do appropriate error handling
        Object message = null;
        try {
            Class<?> messageClass = loader.loadClass(messageName);
            Method parseFrom = messageClass.getMethod(
                "parseFrom", byte[].class);
            message = parseFrom.invoke(
                (Object) messageClass, (Object) messageAsBytes);
        }
        catch (ClassNotFoundException e) {
            System.out.println(e);
        }
        catch (NoSuchMethodException e) {
            System.out.println(e);
        }
        catch (IllegalAccessException e) {
            System.out.println(e);
        }
        catch (InvocationTargetException e) {
            System.out.println(e.getCause());
        }
        // Convert message object to json string
        String messageAsString = null;
        try {
            messageAsString = JsonFormat.printer().print(
                (MessageOrBuilder) message);
        }
        catch (InvalidProtocolBufferException e) {
            System.out.println(e);
        }
        return messageAsString;
    }

    private byte[] convertJsonToMessageBytes(
            String messageAsJson, String messageName)
    {
        PluginClassLoader loader = (PluginClassLoader) getClass().getClassLoader();
        // Get a message builder object
        // TODO: Do appropriate error handling
        Message.Builder builder = null;
        try {
            Class<?> messageClass = loader.loadClass(messageName);
            Method newBuilder = messageClass.getMethod("newBuilder");
            builder = (Message.Builder) newBuilder.invoke(
                (Object) messageClass);
        }
        catch (ClassNotFoundException e) {
            System.out.println(e);
        }
        catch (NoSuchMethodException e) {
            System.out.println(e);
        }
        catch (IllegalAccessException e) {
            System.out.println(e);
        }
        catch (InvocationTargetException e) {
            System.out.println(e.getCause());
        }
        // Convert message json to binary
        byte[] messageAsBytes = null;
        try {
            JsonFormat.parser().merge(messageAsJson, builder);
            messageAsBytes = builder.build().toByteArray();
        }
        catch (InvalidProtocolBufferException e) {
            System.out.println(e);
        }
        return messageAsBytes;
    }

    private String executeTask(ColumnTask colTask, Column column)
    {
        String messageName = colTask.getMessage();
        // serialize
        if (pluginTask.getDoSerialize().get()) {
            String messageAsJson = pageReader.getString(column);
            byte[] messageAsBytes = convertJsonToMessageBytes(
                messageAsJson, messageName);
            return encodeMessage(messageAsBytes);
        }
        // deserialize
        else {
            String messageAsString = pageReader.getString(column);
            byte[] messageAsBytes = decodeMessage(messageAsString);
            return convertMessageBytesToJson(messageAsBytes, messageName);
        }
    }

    @Override
    public void booleanColumn(Column outputColumn)
    {
        if (pageReader.isNull(outputColumn)) {
            pageBuilder.setNull(outputColumn);
        }
        else {
            pageBuilder.setBoolean(
                outputColumn, pageReader.getBoolean(outputColumn));
        }
    }

    @Override
    public void longColumn(Column outputColumn)
    {
        if (pageReader.isNull(outputColumn)) {
            pageBuilder.setNull(outputColumn);
        }
        else {
            pageBuilder.setLong(
                outputColumn, pageReader.getLong(outputColumn));
        }
    }

    @Override
    public void doubleColumn(Column outputColumn)
    {
        if (pageReader.isNull(outputColumn)) {
            pageBuilder.setNull(outputColumn);
        }
        else {
            pageBuilder.setDouble(
                outputColumn, pageReader.getDouble(outputColumn));
        }
    }

    @Override
    public void stringColumn(Column outputColumn)
    {
        if (pageReader.isNull(outputColumn)) {
            pageBuilder.setNull(outputColumn);
        }
        else {
            ColumnTask task = getColumnTask(outputColumn);
            if (task == null) {
                pageBuilder.setString(
                    outputColumn, pageReader.getString(outputColumn));
            }
            else {
                pageBuilder.setString(
                    outputColumn, executeTask(task, outputColumn));
            }
        }
    }

    @Override
    public void timestampColumn(Column outputColumn)
    {
        if (pageReader.isNull(outputColumn)) {
            pageBuilder.setNull(outputColumn);
        }
        else {
            pageBuilder.setTimestamp(
                outputColumn, pageReader.getTimestamp(outputColumn));
        }
    }

    @Override
    public void jsonColumn(Column outputColumn)
    {
        if (pageReader.isNull(outputColumn)) {
            pageBuilder.setNull(outputColumn);
        }
        else {
            pageBuilder.setJson(
                outputColumn, pageReader.getJson(outputColumn));
        }
    }
}
