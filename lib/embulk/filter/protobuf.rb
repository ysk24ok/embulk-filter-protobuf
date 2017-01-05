Embulk::JavaPlugin.register_filter(
  "protobuf", "org.embulk.filter.protobuf.ProtobufFilterPlugin",
  File.expand_path('../../../../classpath', __FILE__))
