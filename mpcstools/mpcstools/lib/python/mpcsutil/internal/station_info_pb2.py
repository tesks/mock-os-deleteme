# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: station_info.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from primitives import time_primitives_pb2 as primitives_dot_time__primitives__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='station_info.proto',
  package='station',
  syntax='proto3',
  serialized_options=_b('\n\035jpl.gds.serialization.stationB%ProtoBufStationPrimitiveSerializationH\001P\001'),
  serialized_pb=_b('\n\x12station_info.proto\x12\x07station\x1a primitives/time_primitives.proto\"\x8f\x01\n\x16Proto3StationTelemInfo\x12\x19\n\x03\x65rt\x18\x01 \x01(\x0b\x32\n.Proto3AdtH\x00\x12\x0f\n\x07\x62itRate\x18\x02 \x01(\x01\x12\x0f\n\x07numBits\x18\x03 \x01(\x05\x12\x11\n\trelayScid\x18\x04 \x01(\x05\x12\x0f\n\x05\x64ssId\x18\x05 \x01(\x05H\x01\x42\x08\n\x06hasErtB\n\n\x08hasDssIdBJ\n\x1djpl.gds.serialization.stationB%ProtoBufStationPrimitiveSerializationH\x01P\x01\x62\x06proto3')
  ,
  dependencies=[primitives_dot_time__primitives__pb2.DESCRIPTOR,])




_PROTO3STATIONTELEMINFO = _descriptor.Descriptor(
  name='Proto3StationTelemInfo',
  full_name='station.Proto3StationTelemInfo',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='ert', full_name='station.Proto3StationTelemInfo.ert', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='bitRate', full_name='station.Proto3StationTelemInfo.bitRate', index=1,
      number=2, type=1, cpp_type=5, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='numBits', full_name='station.Proto3StationTelemInfo.numBits', index=2,
      number=3, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='relayScid', full_name='station.Proto3StationTelemInfo.relayScid', index=3,
      number=4, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='dssId', full_name='station.Proto3StationTelemInfo.dssId', index=4,
      number=5, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
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
    _descriptor.OneofDescriptor(
      name='hasErt', full_name='station.Proto3StationTelemInfo.hasErt',
      index=0, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasDssId', full_name='station.Proto3StationTelemInfo.hasDssId',
      index=1, containing_type=None, fields=[]),
  ],
  serialized_start=66,
  serialized_end=209,
)

_PROTO3STATIONTELEMINFO.fields_by_name['ert'].message_type = primitives_dot_time__primitives__pb2._PROTO3ADT
_PROTO3STATIONTELEMINFO.oneofs_by_name['hasErt'].fields.append(
  _PROTO3STATIONTELEMINFO.fields_by_name['ert'])
_PROTO3STATIONTELEMINFO.fields_by_name['ert'].containing_oneof = _PROTO3STATIONTELEMINFO.oneofs_by_name['hasErt']
_PROTO3STATIONTELEMINFO.oneofs_by_name['hasDssId'].fields.append(
  _PROTO3STATIONTELEMINFO.fields_by_name['dssId'])
_PROTO3STATIONTELEMINFO.fields_by_name['dssId'].containing_oneof = _PROTO3STATIONTELEMINFO.oneofs_by_name['hasDssId']
DESCRIPTOR.message_types_by_name['Proto3StationTelemInfo'] = _PROTO3STATIONTELEMINFO
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

Proto3StationTelemInfo = _reflection.GeneratedProtocolMessageType('Proto3StationTelemInfo', (_message.Message,), dict(
  DESCRIPTOR = _PROTO3STATIONTELEMINFO,
  __module__ = 'station_info_pb2'
  # @@protoc_insertion_point(class_scope:station.Proto3StationTelemInfo)
  ))
_sym_db.RegisterMessage(Proto3StationTelemInfo)


DESCRIPTOR._options = None
# @@protoc_insertion_point(module_scope)
