# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: metadata_map.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from primitives import metadata_primitives_pb2 as primitives_dot_metadata__primitives__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='metadata_map.proto',
  package='metadata',
  syntax='proto3',
  serialized_options=_b('\n\036jpl.gds.serialization.metadataB\032ProtoMetadataSerializationH\001P\001'),
  serialized_pb=_b('\n\x12metadata_map.proto\x12\x08metadata\x1a$primitives/metadata_primitives.proto\"I\n\x11Proto3MetadataMap\x12\x34\n\x08mapEntry\x18\x01 \x03(\x0b\x32\".metadata.Proto3MetadataMapElementB@\n\x1ejpl.gds.serialization.metadataB\x1aProtoMetadataSerializationH\x01P\x01\x62\x06proto3')
  ,
  dependencies=[primitives_dot_metadata__primitives__pb2.DESCRIPTOR,])




_PROTO3METADATAMAP = _descriptor.Descriptor(
  name='Proto3MetadataMap',
  full_name='metadata.Proto3MetadataMap',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='mapEntry', full_name='metadata.Proto3MetadataMap.mapEntry', index=0,
      number=1, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=70,
  serialized_end=143,
)

_PROTO3METADATAMAP.fields_by_name['mapEntry'].message_type = primitives_dot_metadata__primitives__pb2._PROTO3METADATAMAPELEMENT
DESCRIPTOR.message_types_by_name['Proto3MetadataMap'] = _PROTO3METADATAMAP
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

Proto3MetadataMap = _reflection.GeneratedProtocolMessageType('Proto3MetadataMap', (_message.Message,), dict(
  DESCRIPTOR = _PROTO3METADATAMAP,
  __module__ = 'metadata_map_pb2'
  # @@protoc_insertion_point(class_scope:metadata.Proto3MetadataMap)
  ))
_sym_db.RegisterMessage(Proto3MetadataMap)


DESCRIPTOR._options = None
# @@protoc_insertion_point(module_scope)
