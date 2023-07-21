# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: evr.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf.internal import enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from primitives import time_primitives_pb2 as primitives_dot_time__primitives__pb2
import abstract_message_pb2 as abstract__message__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='evr.proto',
  package='evr',
  syntax='proto3',
  serialized_options=_b('\n\031jpl.gds.serialization.evrB\037ProtoBufEvrMessageSerializationH\001P\001'),
  serialized_pb=_b('\n\tevr.proto\x12\x03\x65vr\x1a primitives/time_primitives.proto\x1a\x16\x61\x62stract_message.proto\"\xcd\x03\n\x10Proto3EvrMessage\x12.\n\x05super\x18\x01 \x01(\x0b\x32\x1f.messages.Proto3AbstractMessage\x12/\n\revrDefinition\x18\x02 \x01(\x0b\x32\x18.evr.Proto3EvrDefinition\x12\x12\n\nevrMessage\x18\x03 \x01(\t\x12+\n\x0b\x65vrMetadata\x18\x04 \x01(\x0b\x32\x16.evr.Proto3EvrMetadata\x12\x10\n\x08realtime\x18\x05 \x01(\x08\x12\x0f\n\x07usesSol\x18\x06 \x01(\x08\x12\x19\n\x03\x65rt\x18\x07 \x01(\x0b\x32\n.Proto3AdtH\x00\x12\x1b\n\x04sclk\x18\x08 \x01(\x0b\x32\x0b.Proto3SclkH\x01\x12\x1a\n\x04scet\x18\t \x01(\x0b\x32\n.Proto3AdtH\x02\x12\x19\n\x03sol\x18\n \x01(\x0b\x32\n.Proto3LstH\x03\x12\x19\n\x03rct\x18\x0b \x01(\x0b\x32\n.Proto3AdtH\x04\x12\x0f\n\x05\x64ssId\x18\x0c \x01(\x05H\x05\x12\x0e\n\x04vcid\x18\r \x01(\x05H\x06\x42\x08\n\x06hasErtB\t\n\x07hasSclkB\t\n\x07hasScetB\x08\n\x06hasSolB\x08\n\x06hasRctB\n\n\x08hasDssIdB\t\n\x07hasVcid\"\x8e\x01\n\x13Proto3EvrDefinition\x12\r\n\x05level\x18\x01 \x01(\t\x12\x10\n\x06module\x18\x02 \x01(\tH\x00\x12\x10\n\x06opsCat\x18\x03 \x01(\tH\x01\x12\x0e\n\x04name\x18\x04 \x01(\tH\x02\x12\x0f\n\x07\x65ventId\x18\x05 \x01(\x03\x42\x0b\n\thasModuleB\x0b\n\thasOpsCatB\t\n\x07hasName\"I\n\x11Proto3EvrMetadata\x12\x34\n\rmetadataEntry\x18\x01 \x03(\x0b\x32\x1d.evr.Proto3EvrMetadataElement\"Q\n\x18Proto3EvrMetadataElement\x12&\n\x03key\x18\x01 \x01(\x0e\x32\x19.evr.Proto3EvrMetadataKey\x12\r\n\x05value\x18\x02 \x01(\t*\x8e\x01\n\x14Proto3EvrMetadataKey\x12\x0b\n\x07UNKNOWN\x10\x00\x12\x0c\n\x08TASKNAME\x10\x01\x12\x0e\n\nSEQUENCEID\x10\x02\x12\x16\n\x12\x43\x41TEGORYSEQUENCEID\x10\x03\x12\x10\n\x0c\x41\x44\x44RESSSTACK\x10\x04\x12\n\n\x06SOURCE\x10\x05\x12\n\n\x06TASKID\x10\x06\x12\t\n\x05\x45RRNO\x10\x07\x42@\n\x19jpl.gds.serialization.evrB\x1fProtoBufEvrMessageSerializationH\x01P\x01\x62\x06proto3')
  ,
  dependencies=[primitives_dot_time__primitives__pb2.DESCRIPTOR,abstract__message__pb2.DESCRIPTOR,])

_PROTO3EVRMETADATAKEY = _descriptor.EnumDescriptor(
  name='Proto3EvrMetadataKey',
  full_name='evr.Proto3EvrMetadataKey',
  filename=None,
  file=DESCRIPTOR,
  values=[
    _descriptor.EnumValueDescriptor(
      name='UNKNOWN', index=0, number=0,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='TASKNAME', index=1, number=1,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='SEQUENCEID', index=2, number=2,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='CATEGORYSEQUENCEID', index=3, number=3,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='ADDRESSSTACK', index=4, number=4,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='SOURCE', index=5, number=5,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='TASKID', index=6, number=6,
      serialized_options=None,
      type=None),
    _descriptor.EnumValueDescriptor(
      name='ERRNO', index=7, number=7,
      serialized_options=None,
      type=None),
  ],
  containing_type=None,
  serialized_options=None,
  serialized_start=844,
  serialized_end=986,
)
_sym_db.RegisterEnumDescriptor(_PROTO3EVRMETADATAKEY)

Proto3EvrMetadataKey = enum_type_wrapper.EnumTypeWrapper(_PROTO3EVRMETADATAKEY)
UNKNOWN = 0
TASKNAME = 1
SEQUENCEID = 2
CATEGORYSEQUENCEID = 3
ADDRESSSTACK = 4
SOURCE = 5
TASKID = 6
ERRNO = 7



_PROTO3EVRMESSAGE = _descriptor.Descriptor(
  name='Proto3EvrMessage',
  full_name='evr.Proto3EvrMessage',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='super', full_name='evr.Proto3EvrMessage.super', index=0,
      number=1, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='evrDefinition', full_name='evr.Proto3EvrMessage.evrDefinition', index=1,
      number=2, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='evrMessage', full_name='evr.Proto3EvrMessage.evrMessage', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='evrMetadata', full_name='evr.Proto3EvrMessage.evrMetadata', index=3,
      number=4, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='realtime', full_name='evr.Proto3EvrMessage.realtime', index=4,
      number=5, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='usesSol', full_name='evr.Proto3EvrMessage.usesSol', index=5,
      number=6, type=8, cpp_type=7, label=1,
      has_default_value=False, default_value=False,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='ert', full_name='evr.Proto3EvrMessage.ert', index=6,
      number=7, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='sclk', full_name='evr.Proto3EvrMessage.sclk', index=7,
      number=8, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='scet', full_name='evr.Proto3EvrMessage.scet', index=8,
      number=9, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='sol', full_name='evr.Proto3EvrMessage.sol', index=9,
      number=10, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='rct', full_name='evr.Proto3EvrMessage.rct', index=10,
      number=11, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='dssId', full_name='evr.Proto3EvrMessage.dssId', index=11,
      number=12, type=5, cpp_type=1, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='vcid', full_name='evr.Proto3EvrMessage.vcid', index=12,
      number=13, type=5, cpp_type=1, label=1,
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
      name='hasErt', full_name='evr.Proto3EvrMessage.hasErt',
      index=0, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasSclk', full_name='evr.Proto3EvrMessage.hasSclk',
      index=1, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasScet', full_name='evr.Proto3EvrMessage.hasScet',
      index=2, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasSol', full_name='evr.Proto3EvrMessage.hasSol',
      index=3, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasRct', full_name='evr.Proto3EvrMessage.hasRct',
      index=4, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasDssId', full_name='evr.Proto3EvrMessage.hasDssId',
      index=5, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasVcid', full_name='evr.Proto3EvrMessage.hasVcid',
      index=6, containing_type=None, fields=[]),
  ],
  serialized_start=77,
  serialized_end=538,
)


_PROTO3EVRDEFINITION = _descriptor.Descriptor(
  name='Proto3EvrDefinition',
  full_name='evr.Proto3EvrDefinition',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='level', full_name='evr.Proto3EvrDefinition.level', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='module', full_name='evr.Proto3EvrDefinition.module', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='opsCat', full_name='evr.Proto3EvrDefinition.opsCat', index=2,
      number=3, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='name', full_name='evr.Proto3EvrDefinition.name', index=3,
      number=4, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='eventId', full_name='evr.Proto3EvrDefinition.eventId', index=4,
      number=5, type=3, cpp_type=2, label=1,
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
      name='hasModule', full_name='evr.Proto3EvrDefinition.hasModule',
      index=0, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasOpsCat', full_name='evr.Proto3EvrDefinition.hasOpsCat',
      index=1, containing_type=None, fields=[]),
    _descriptor.OneofDescriptor(
      name='hasName', full_name='evr.Proto3EvrDefinition.hasName',
      index=2, containing_type=None, fields=[]),
  ],
  serialized_start=541,
  serialized_end=683,
)


_PROTO3EVRMETADATA = _descriptor.Descriptor(
  name='Proto3EvrMetadata',
  full_name='evr.Proto3EvrMetadata',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='metadataEntry', full_name='evr.Proto3EvrMetadata.metadataEntry', index=0,
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
  serialized_start=685,
  serialized_end=758,
)


_PROTO3EVRMETADATAELEMENT = _descriptor.Descriptor(
  name='Proto3EvrMetadataElement',
  full_name='evr.Proto3EvrMetadataElement',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='evr.Proto3EvrMetadataElement.key', index=0,
      number=1, type=14, cpp_type=8, label=1,
      has_default_value=False, default_value=0,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value', full_name='evr.Proto3EvrMetadataElement.value', index=1,
      number=2, type=9, cpp_type=9, label=1,
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
  ],
  serialized_start=760,
  serialized_end=841,
)

_PROTO3EVRMESSAGE.fields_by_name['super'].message_type = abstract__message__pb2._PROTO3ABSTRACTMESSAGE
_PROTO3EVRMESSAGE.fields_by_name['evrDefinition'].message_type = _PROTO3EVRDEFINITION
_PROTO3EVRMESSAGE.fields_by_name['evrMetadata'].message_type = _PROTO3EVRMETADATA
_PROTO3EVRMESSAGE.fields_by_name['ert'].message_type = primitives_dot_time__primitives__pb2._PROTO3ADT
_PROTO3EVRMESSAGE.fields_by_name['sclk'].message_type = primitives_dot_time__primitives__pb2._PROTO3SCLK
_PROTO3EVRMESSAGE.fields_by_name['scet'].message_type = primitives_dot_time__primitives__pb2._PROTO3ADT
_PROTO3EVRMESSAGE.fields_by_name['sol'].message_type = primitives_dot_time__primitives__pb2._PROTO3LST
_PROTO3EVRMESSAGE.fields_by_name['rct'].message_type = primitives_dot_time__primitives__pb2._PROTO3ADT
_PROTO3EVRMESSAGE.oneofs_by_name['hasErt'].fields.append(
  _PROTO3EVRMESSAGE.fields_by_name['ert'])
_PROTO3EVRMESSAGE.fields_by_name['ert'].containing_oneof = _PROTO3EVRMESSAGE.oneofs_by_name['hasErt']
_PROTO3EVRMESSAGE.oneofs_by_name['hasSclk'].fields.append(
  _PROTO3EVRMESSAGE.fields_by_name['sclk'])
_PROTO3EVRMESSAGE.fields_by_name['sclk'].containing_oneof = _PROTO3EVRMESSAGE.oneofs_by_name['hasSclk']
_PROTO3EVRMESSAGE.oneofs_by_name['hasScet'].fields.append(
  _PROTO3EVRMESSAGE.fields_by_name['scet'])
_PROTO3EVRMESSAGE.fields_by_name['scet'].containing_oneof = _PROTO3EVRMESSAGE.oneofs_by_name['hasScet']
_PROTO3EVRMESSAGE.oneofs_by_name['hasSol'].fields.append(
  _PROTO3EVRMESSAGE.fields_by_name['sol'])
_PROTO3EVRMESSAGE.fields_by_name['sol'].containing_oneof = _PROTO3EVRMESSAGE.oneofs_by_name['hasSol']
_PROTO3EVRMESSAGE.oneofs_by_name['hasRct'].fields.append(
  _PROTO3EVRMESSAGE.fields_by_name['rct'])
_PROTO3EVRMESSAGE.fields_by_name['rct'].containing_oneof = _PROTO3EVRMESSAGE.oneofs_by_name['hasRct']
_PROTO3EVRMESSAGE.oneofs_by_name['hasDssId'].fields.append(
  _PROTO3EVRMESSAGE.fields_by_name['dssId'])
_PROTO3EVRMESSAGE.fields_by_name['dssId'].containing_oneof = _PROTO3EVRMESSAGE.oneofs_by_name['hasDssId']
_PROTO3EVRMESSAGE.oneofs_by_name['hasVcid'].fields.append(
  _PROTO3EVRMESSAGE.fields_by_name['vcid'])
_PROTO3EVRMESSAGE.fields_by_name['vcid'].containing_oneof = _PROTO3EVRMESSAGE.oneofs_by_name['hasVcid']
_PROTO3EVRDEFINITION.oneofs_by_name['hasModule'].fields.append(
  _PROTO3EVRDEFINITION.fields_by_name['module'])
_PROTO3EVRDEFINITION.fields_by_name['module'].containing_oneof = _PROTO3EVRDEFINITION.oneofs_by_name['hasModule']
_PROTO3EVRDEFINITION.oneofs_by_name['hasOpsCat'].fields.append(
  _PROTO3EVRDEFINITION.fields_by_name['opsCat'])
_PROTO3EVRDEFINITION.fields_by_name['opsCat'].containing_oneof = _PROTO3EVRDEFINITION.oneofs_by_name['hasOpsCat']
_PROTO3EVRDEFINITION.oneofs_by_name['hasName'].fields.append(
  _PROTO3EVRDEFINITION.fields_by_name['name'])
_PROTO3EVRDEFINITION.fields_by_name['name'].containing_oneof = _PROTO3EVRDEFINITION.oneofs_by_name['hasName']
_PROTO3EVRMETADATA.fields_by_name['metadataEntry'].message_type = _PROTO3EVRMETADATAELEMENT
_PROTO3EVRMETADATAELEMENT.fields_by_name['key'].enum_type = _PROTO3EVRMETADATAKEY
DESCRIPTOR.message_types_by_name['Proto3EvrMessage'] = _PROTO3EVRMESSAGE
DESCRIPTOR.message_types_by_name['Proto3EvrDefinition'] = _PROTO3EVRDEFINITION
DESCRIPTOR.message_types_by_name['Proto3EvrMetadata'] = _PROTO3EVRMETADATA
DESCRIPTOR.message_types_by_name['Proto3EvrMetadataElement'] = _PROTO3EVRMETADATAELEMENT
DESCRIPTOR.enum_types_by_name['Proto3EvrMetadataKey'] = _PROTO3EVRMETADATAKEY
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

Proto3EvrMessage = _reflection.GeneratedProtocolMessageType('Proto3EvrMessage', (_message.Message,), dict(
  DESCRIPTOR = _PROTO3EVRMESSAGE,
  __module__ = 'evr_pb2'
  # @@protoc_insertion_point(class_scope:evr.Proto3EvrMessage)
  ))
_sym_db.RegisterMessage(Proto3EvrMessage)

Proto3EvrDefinition = _reflection.GeneratedProtocolMessageType('Proto3EvrDefinition', (_message.Message,), dict(
  DESCRIPTOR = _PROTO3EVRDEFINITION,
  __module__ = 'evr_pb2'
  # @@protoc_insertion_point(class_scope:evr.Proto3EvrDefinition)
  ))
_sym_db.RegisterMessage(Proto3EvrDefinition)

Proto3EvrMetadata = _reflection.GeneratedProtocolMessageType('Proto3EvrMetadata', (_message.Message,), dict(
  DESCRIPTOR = _PROTO3EVRMETADATA,
  __module__ = 'evr_pb2'
  # @@protoc_insertion_point(class_scope:evr.Proto3EvrMetadata)
  ))
_sym_db.RegisterMessage(Proto3EvrMetadata)

Proto3EvrMetadataElement = _reflection.GeneratedProtocolMessageType('Proto3EvrMetadataElement', (_message.Message,), dict(
  DESCRIPTOR = _PROTO3EVRMETADATAELEMENT,
  __module__ = 'evr_pb2'
  # @@protoc_insertion_point(class_scope:evr.Proto3EvrMetadataElement)
  ))
_sym_db.RegisterMessage(Proto3EvrMetadataElement)


DESCRIPTOR._options = None
# @@protoc_insertion_point(module_scope)