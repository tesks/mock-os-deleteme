# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: alarmed_channel_value.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


import abstract_message_pb2 as abstract__message__pb2
from primitives import time_primitives_pb2 as primitives_dot_time__primitives__pb2
from primitives import eha_primitives_pb2 as primitives_dot_eha__primitives__pb2
from primitives import alarm_primitives_pb2 as primitives_dot_alarm__primitives__pb2
import alarm_value_set_pb2 as alarm__value__set__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='alarmed_channel_value.proto',
  package='eha',
  syntax='proto3',
  serialized_options=_b('\n%jpl.gds.eha.api.channel.serializationB(ProtoBufAlarmedChannelValueSerializationH\001P\001'),
  serialized_pb=_b('\n\x1b\x61larmed_channel_value.proto\x12\x03\x65ha\x1a\x16\x61\x62stract_message.proto\x1a primitives/time_primitives.proto\x1a\x1fprimitives/eha_primitives.proto\x1a!primitives/alarm_primitives.proto\x1a\x15\x61larm_value_set.proto\"\x9f\x01\n Proto3AlarmedChannelValueMessage\x12.\n\x05super\x18\x01 \x01(\x0b\x32\x1f.messages.Proto3AbstractMessage\x12(\n\x07\x63hanVal\x18\x02 \x01(\x0b\x32\x17.eha.Proto3ChannelValue\x12\x12\n\x08streamId\x18\x03 \x01(\tH\x00\x42\r\n\x0bhasStreamId\"D\n\x17Proto3ServiceCollection\x12)\n\x08\x63hannels\x18\x01 \x03(\x0b\x32\x17.eha.Proto3ChannelValue\"\xba\x04\n\x12Proto3ChannelValue\x12\r\n\x05title\x18\x01 \x01(\t\x12\x11\n\tchannelId\x18\x02 \x01(\t\x12+\n\x0b\x63hanDefType\x18\x03 \x01(\x0e\x32\x16.eha.Proto3ChanDefType\x12\x19\n\x02\x64n\x18\x04 \x01(\x0b\x32\r.eha.Proto3Dn\x12&\n\x06\x61larms\x18\x05 \x01(\x0b\x32\x14.Proto3AlarmValueSetH\x00\x12\x0c\n\x02\x65u\x18\x06 \x01(\x01H\x01\x12\r\n\x05\x64ssId\x18\x07 \x01(\x05\x12\x0e\n\x04vcid\x18\x08 \x01(\x05H\x02\x12\x12\n\nisRealtime\x18\t \x01(\x08\x12\x19\n\x03\x65rt\x18\n \x01(\x0b\x32\n.Proto3AdtH\x03\x12\x19\n\x03rct\x18\x0b \x01(\x0b\x32\n.Proto3AdtH\x04\x12\x19\n\x03lst\x18\x0c \x01(\x0b\x32\n.Proto3LstH\x05\x12\x1a\n\x04scet\x18\r \x01(\x0b\x32\n.Proto3AdtH\x06\x12\x1b\n\x04sclk\x18\x0e \x01(\x0b\x32\x0b.Proto3SclkH\x07\x12\x10\n\x06status\x18\x0f \x01(\tH\x08\x12\x0f\n\x07\x64nUnits\x18\x10 \x01(\t\x12\x0f\n\x07\x65uUnits\x18\x11 \x01(\t\x12\x11\n\tsubsystem\x18\x12 \x01(\t\x12\x0e\n\x06opsCat\x18\x13 \x01(\t\x12\x0e\n\x06module\x18\x14 \x01(\tB\x0b\n\thasAlarmsB\x07\n\x05hasEuB\t\n\x07hasVcidB\x08\n\x06hasErtB\x08\n\x06hasRctB\x08\n\x06hasLstB\t\n\x07hasScetB\t\n\x07hasSclkB\x0b\n\thasStatusBU\n%jpl.gds.eha.api.channel.serializationB(ProtoBufAlarmedChannelValueSerializationH\x01P\x01\x62\x06proto3')
  ,
  dependencies=[abstract__message__pb2.DESCRIPTOR,primitives_dot_time__primitives__pb2.DESCRIPTOR,primitives_dot_eha__primitives__pb2.DESCRIPTOR,primitives_dot_alarm__primitives__pb2.DESCRIPTOR,alarm__value__set__pb2.DESCRIPTOR,])




_PROTO3ALARMEDCHANNELVALUEMESSAGE = _descriptor.Descriptor(
  name='Proto3AlarmedChannelValueMessage',
  full_name='eha.Proto3AlarmedChannelValueMessage',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='super', full_name='eha.Proto3AlarmedChannelValueMessage.super', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='chanVal', full_name='eha.Proto3AlarmedChannelValueMessage.chanVal', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='streamId', full_name='eha.Proto3AlarmedChannelValueMessage.streamId', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
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
      name='hasStreamId', full_name='eha.Proto3AlarmedChannelValueMessage.hasStreamId',
      index=0, containing_type=None, fields=[]),
  ],
  serialized_start=186,
  serialized_end=345,
)


_PROTO3SERVICECOLLECTION = _descriptor.Descriptor(
  name='Proto3ServiceCollection',
  full_name='eha.Proto3ServiceCollection',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='channels', full_name='eha.Proto3ServiceCollection.channels', index=0,
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
  serialized_start=347,
  serialized_end=415,
)


_PROTO3CHANNELVALUE = _descriptor.Descriptor(
  name='Proto3ChannelValue',
  full_name='eha.Proto3ChannelValue',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='title', full_name='eha.Proto3ChannelValue.title', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='channelId', full_name='eha.Proto3ChannelValue.channelId', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='chanDefType', full_name='eha.Proto3ChannelValue.chanDefType', index=2,
      number=3, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='dn', full_name='eha.Proto3ChannelValue.dn', index=3,
      number=4, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='alarms', full_name='eha.Proto3ChannelValue.alarms', index=4,
      number=5, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='eu', full_name='eha.Proto3ChannelValue.eu', index=5,
      number=6, type=1, cpp_type=5, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='dssId', full_name='eha.Proto3ChannelValue.dssId', index=6,
      number=7, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='vcid', full_name='eha.Proto3ChannelValue.vcid', index=7,
      number=8, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='isRealtime', full_name='eha.Proto3ChannelValue.isRealtime', index=8,
      number=9, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='ert', full_name='eha.Proto3ChannelValue.ert', index=9,
      number=10, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='rct', full_name='eha.Proto3ChannelValue.rct', index=10,
      number=11, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='lst', full_name='eha.Proto3ChannelValue.lst', index=11,
      number=12, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='scet', full_name='eha.Proto3ChannelValue.scet', index=12,
      number=13, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='sclk', full_name='eha.Proto3ChannelValue.sclk', index=13,
      number=14, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='status', full_name='eha.Proto3ChannelValue.status', index=14,
      number=15, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='dnUnits', full_name='eha.Proto3ChannelValue.dnUnits', index=15,
      number=16, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='euUnits', full_name='eha.Proto3ChannelValue.euUnits', index=16,
      number=17, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='subsystem', full_name='eha.Proto3ChannelValue.subsystem', index=17,
      number=18, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='opsCat', full_name='eha.Proto3ChannelValue.opsCat', index=18,
      number=19, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='module', full_name='eha.Proto3ChannelValue.module', index=19,
      number=20, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
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
      name='hasAlarms', full_name='eha.Proto3ChannelValue.hasAlarms',
      index=0, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasEu', full_name='eha.Proto3ChannelValue.hasEu',
      index=1, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasVcid', full_name='eha.Proto3ChannelValue.hasVcid',
      index=2, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasErt', full_name='eha.Proto3ChannelValue.hasErt',
      index=3, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasRct', full_name='eha.Proto3ChannelValue.hasRct',
      index=4, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasLst', full_name='eha.Proto3ChannelValue.hasLst',
      index=5, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasScet', full_name='eha.Proto3ChannelValue.hasScet',
      index=6, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasSclk', full_name='eha.Proto3ChannelValue.hasSclk',
      index=7, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasStatus', full_name='eha.Proto3ChannelValue.hasStatus',
      index=8, containing_type=None, fields=[]),
  ],
  serialized_start=418,
  serialized_end=988,
)

_PROTO3ALARMEDCHANNELVALUEMESSAGE.fields_by_name['super'].message_type = abstract__message__pb2._PROTO3ABSTRACTMESSAGE
_PROTO3ALARMEDCHANNELVALUEMESSAGE.fields_by_name['chanVal'].message_type = _PROTO3CHANNELVALUE
_PROTO3ALARMEDCHANNELVALUEMESSAGE.oneofs_by_name['hasStreamId'].fields.append(
  _PROTO3ALARMEDCHANNELVALUEMESSAGE.fields_by_name['streamId'])
_PROTO3ALARMEDCHANNELVALUEMESSAGE.fields_by_name['streamId'].containing_oneof = _PROTO3ALARMEDCHANNELVALUEMESSAGE.oneofs_by_name['hasStreamId']
_PROTO3SERVICECOLLECTION.fields_by_name['channels'].message_type = _PROTO3CHANNELVALUE
_PROTO3CHANNELVALUE.fields_by_name['chanDefType'].enum_type = primitives_dot_eha__primitives__pb2._PROTO3CHANDEFTYPE
_PROTO3CHANNELVALUE.fields_by_name['dn'].message_type = primitives_dot_eha__primitives__pb2._PROTO3DN
_PROTO3CHANNELVALUE.fields_by_name['alarms'].message_type = alarm__value__set__pb2._PROTO3ALARMVALUESET
_PROTO3CHANNELVALUE.fields_by_name['ert'].message_type = primitives_dot_time__primitives__pb2._PROTO3ADT
_PROTO3CHANNELVALUE.fields_by_name['rct'].message_type = primitives_dot_time__primitives__pb2._PROTO3ADT
_PROTO3CHANNELVALUE.fields_by_name['lst'].message_type = primitives_dot_time__primitives__pb2._PROTO3LST
_PROTO3CHANNELVALUE.fields_by_name['scet'].message_type = primitives_dot_time__primitives__pb2._PROTO3ADT
_PROTO3CHANNELVALUE.fields_by_name['sclk'].message_type = primitives_dot_time__primitives__pb2._PROTO3SCLK
_PROTO3CHANNELVALUE.oneofs_by_name['hasAlarms'].fields.append(
  _PROTO3CHANNELVALUE.fields_by_name['alarms'])
_PROTO3CHANNELVALUE.fields_by_name['alarms'].containing_oneof = _PROTO3CHANNELVALUE.oneofs_by_name['hasAlarms']
_PROTO3CHANNELVALUE.oneofs_by_name['hasEu'].fields.append(
  _PROTO3CHANNELVALUE.fields_by_name['eu'])
_PROTO3CHANNELVALUE.fields_by_name['eu'].containing_oneof = _PROTO3CHANNELVALUE.oneofs_by_name['hasEu']
_PROTO3CHANNELVALUE.oneofs_by_name['hasVcid'].fields.append(
  _PROTO3CHANNELVALUE.fields_by_name['vcid'])
_PROTO3CHANNELVALUE.fields_by_name['vcid'].containing_oneof = _PROTO3CHANNELVALUE.oneofs_by_name['hasVcid']
_PROTO3CHANNELVALUE.oneofs_by_name['hasErt'].fields.append(
  _PROTO3CHANNELVALUE.fields_by_name['ert'])
_PROTO3CHANNELVALUE.fields_by_name['ert'].containing_oneof = _PROTO3CHANNELVALUE.oneofs_by_name['hasErt']
_PROTO3CHANNELVALUE.oneofs_by_name['hasRct'].fields.append(
  _PROTO3CHANNELVALUE.fields_by_name['rct'])
_PROTO3CHANNELVALUE.fields_by_name['rct'].containing_oneof = _PROTO3CHANNELVALUE.oneofs_by_name['hasRct']
_PROTO3CHANNELVALUE.oneofs_by_name['hasLst'].fields.append(
  _PROTO3CHANNELVALUE.fields_by_name['lst'])
_PROTO3CHANNELVALUE.fields_by_name['lst'].containing_oneof = _PROTO3CHANNELVALUE.oneofs_by_name['hasLst']
_PROTO3CHANNELVALUE.oneofs_by_name['hasScet'].fields.append(
  _PROTO3CHANNELVALUE.fields_by_name['scet'])
_PROTO3CHANNELVALUE.fields_by_name['scet'].containing_oneof = _PROTO3CHANNELVALUE.oneofs_by_name['hasScet']
_PROTO3CHANNELVALUE.oneofs_by_name['hasSclk'].fields.append(
  _PROTO3CHANNELVALUE.fields_by_name['sclk'])
_PROTO3CHANNELVALUE.fields_by_name['sclk'].containing_oneof = _PROTO3CHANNELVALUE.oneofs_by_name['hasSclk']
_PROTO3CHANNELVALUE.oneofs_by_name['hasStatus'].fields.append(
  _PROTO3CHANNELVALUE.fields_by_name['status'])
_PROTO3CHANNELVALUE.fields_by_name['status'].containing_oneof = _PROTO3CHANNELVALUE.oneofs_by_name['hasStatus']
DESCRIPTOR.message_types_by_name['Proto3AlarmedChannelValueMessage'] = _PROTO3ALARMEDCHANNELVALUEMESSAGE
DESCRIPTOR.message_types_by_name['Proto3ServiceCollection'] = _PROTO3SERVICECOLLECTION
DESCRIPTOR.message_types_by_name['Proto3ChannelValue'] = _PROTO3CHANNELVALUE
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

Proto3AlarmedChannelValueMessage = _reflection.GeneratedProtocolMessageType('Proto3AlarmedChannelValueMessage', (_message.Message,), dict(
  DESCRIPTOR = _PROTO3ALARMEDCHANNELVALUEMESSAGE,
  __module__ = 'alarmed_channel_value_pb2'
  # @@protoc_insertion_point(class_scope:eha.Proto3AlarmedChannelValueMessage)
  ))
_sym_db.RegisterMessage(Proto3AlarmedChannelValueMessage)

Proto3ServiceCollection = _reflection.GeneratedProtocolMessageType('Proto3ServiceCollection', (_message.Message,), dict(
  DESCRIPTOR = _PROTO3SERVICECOLLECTION,
  __module__ = 'alarmed_channel_value_pb2'
  # @@protoc_insertion_point(class_scope:eha.Proto3ServiceCollection)
  ))
_sym_db.RegisterMessage(Proto3ServiceCollection)

Proto3ChannelValue = _reflection.GeneratedProtocolMessageType('Proto3ChannelValue', (_message.Message,), dict(
  DESCRIPTOR = _PROTO3CHANNELVALUE,
  __module__ = 'alarmed_channel_value_pb2'
  # @@protoc_insertion_point(class_scope:eha.Proto3ChannelValue)
  ))
_sym_db.RegisterMessage(Proto3ChannelValue)


DESCRIPTOR._options = None
# @@protoc_insertion_point(module_scope)
