# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: alarm_value_set.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


import alarm_value_pb2 as alarm__value__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='alarm_value_set.proto',
  package='',
  syntax='proto3',
  serialized_options=_b('\n\033jpl.gds.alarm.serializationH\001P\001'),
  serialized_pb=_b('\n\x15\x61larm_value_set.proto\x1a\x11\x61larm_value.proto\"8\n\x13Proto3AlarmValueSet\x12!\n\x06\x61larms\x18\x01 \x03(\x0b\x32\x11.Proto3AlarmValueB!\n\x1bjpl.gds.alarm.serializationH\x01P\x01\x62\x06proto3')
  ,
  dependencies=[alarm__value__pb2.DESCRIPTOR,])




_PROTO3ALARMVALUESET = _descriptor.Descriptor(
  name='Proto3AlarmValueSet',
  full_name='Proto3AlarmValueSet',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='alarms', full_name='Proto3AlarmValueSet.alarms', index=0,
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
  serialized_start=44,
  serialized_end=100,
)

_PROTO3ALARMVALUESET.fields_by_name['alarms'].message_type = alarm__value__pb2._PROTO3ALARMVALUE
DESCRIPTOR.message_types_by_name['Proto3AlarmValueSet'] = _PROTO3ALARMVALUESET
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

Proto3AlarmValueSet = _reflection.GeneratedProtocolMessageType('Proto3AlarmValueSet', (_message.Message,), dict(
  DESCRIPTOR = _PROTO3ALARMVALUESET,
  __module__ = 'alarm_value_set_pb2'
  # @@protoc_insertion_point(class_scope:Proto3AlarmValueSet)
  ))
_sym_db.RegisterMessage(Proto3AlarmValueSet)


DESCRIPTOR._options = None
# @@protoc_insertion_point(module_scope)
