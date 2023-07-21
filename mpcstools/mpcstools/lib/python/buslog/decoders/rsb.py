import mpcsutil
import buslog.decoders
import buslog.err
import os.path
import xml.parsers.expat
import sys

CONFIG_NAME = "1553_config_RSB.xml"

class RsbConfig(object):

    def __init__(self):

        self.string_id_map = {}
        self.address_mode_map = {}
        self.address_map = {}
        self.load_switch_map = {}
        self.pyro_switch_map = {}
        self.function_map = {}
        self.subfunction_map = {}
        self.function_to_table_map = {}
        self.subfunction_to_table_map = {}

    def _startElement(self,name,attrs):

        if name == 'entry':

            bank = int(attrs['bank'])
            bit_and_mask = int(attrs['bit_and_mask'])
            value = attrs['value']

            if value:
                if hasattr(self,'current_subfunction'):
                    self.current_subfunction_table[bank][bit_and_mask] = value
                else:
                    self.current_function_table[bank][bit_and_mask] = value

        elif name == 'switch':

            abbrev = attrs['abbreviation']
            full_name = attrs['name']
            self.load_switch_map[abbrev] = full_name

        elif name == 'pyro':

            abbrev = attrs['abbreviation']
            full_name = attrs['name']
            self.pyro_switch_map[abbrev] = full_name

        elif name == 'stringId':

            number = int(attrs['number'])
            value = attrs['value']
            self.string_id_map[number] = value

        elif name == 'addressMode':

            number = int(attrs['number'])
            value = attrs['value']
            self.address_mode_map[number] = value

        elif name == 'address':

            number = int(attrs['number'])
            value = attrs['value']
            self.address_map[number] = value

        elif name == 'function':

            number = int(attrs['number'])
            value = attrs['value']
            self.function_map[number] = value

            self.current_function = number
            self.current_function_table = []
            for dummy_i in range(0,8):
                self.current_function_table.append(['']*8)

        elif name == 'subfunction':

            number = int(attrs['number'])
            value = attrs['value']
            self.subfunction_map[(self.current_function,number)] = value

            self.current_subfunction = number
            self.current_subfunction_table = []
            for dummy_i in range(0,8):
                self.current_subfunction_table.append(['']*8)


    def _endElement(self,name):

        if name == 'function':

            self.function_to_table_map[self.current_function] = self.current_function_table
            del self.current_function
            del self.current_function_table

        elif name == 'subfunction':

            self.subfunction_to_table_map[(self.current_function,self.current_subfunction)] = self.current_subfunction_table
            del self.current_subfunction
            del self.current_subfunction_table

    def parse_config(self):

        global CONFIG_NAME

        gds_config = mpcsutil.config.GdsConfig()
        # MPCS-6683 - 10/15/15: Config file now not only in the mission config directory.
        # Use standard config hierarchy.
        log1553_rsb_config = mpcsutil.filesystem.get_file(gds_config.getConfigDirs(), CONFIG_NAME)

        try:
            parser = xml.parsers.expat.ParserCreate()

            parser.StartElementHandler = self._startElement
            parser.EndElementHandler = self._endElement

            outFile = open(log1553_rsb_config,'rb')
            parser.ParseFile(outFile)
            outFile.close()

        except xml.parsers.expat.ExpatError as e:

            outFile.close()
            raise buslog.err.LogParseException('Error parsing 1553 configuration properties from XML (row %d, col %d): %s' % (e.lineno,e.offset,sys.exc_info()[1]))

rsb_config = RsbConfig()
rsb_config.parse_config()

def int_to_binary(val,length=None):
    '''NOTE: this is not a full hex-to-binary capability.  It will convert
    whatever you give it to an unsigned number.'''

    value = int(val)
    bits = bin(value)

    if length is not None:
        prefix = bits[:2]
        suffix = bits[2:]
        return prefix + ('0' * (length-len(suffix))) + suffix

    return bits

watchdog_request = [0x4c0a,0x0000,0x0206,0x0602,0xa9bc]
watchdog_response = [0x8c0a,0x0000,0x0206,0x0602,0xe9bc]

class RsbEntry(buslog.decoders.DataEntry):

    def __init__(self,remote_terminal,sub_address):

        buslog.decoders.DataEntry.__init__(self, remote_terminal, sub_address)

        #Data words
        self.word0 = None
        self.word1 = None
        self.word2 = None
        self.word3 = None
        self.word4 = None

        #Word 0 stuff
        self.string_id_num = None
        self.string_id = None
        self.function_num = None
        self.function = None
        self.address_mode_num = None
        self.address_mode = None
        self.address0 = None
        self.address0_valid = True

        #Word 1 stuff
        self.address1 = None
        self.address1_valid = False
        self.address2 = None
        self.address2_valid = False

        #Word 2 stuff
        self.bank0 = None
        self.var_e = None

        #Word 3 stuff
        self.bank1 = None
        self.var_sw = None

        #Word 4 stuff
        self.checksum = None

        #Function-Specific Data
        self.status = None
        self.bit_select = None
        self.bank_select = None
        self.bit_set = None
        self.duration_millis = None
        self.delay_millis = None
        self.sub_function = None

    def is_checksum_valid(self):

        calculated_checksum = self.word0 + self.word1 + self.word2 + self.word3 + 0x55AA
        if calculated_checksum > 0xFFFF:
            top_half = (calculated_checksum & 0xFFFF0000) >> 16
            bottom_half = (calculated_checksum & 0x0000FFFF)
            calculated_checksum = top_half + bottom_half
        return calculated_checksum == self.checksum

    def parse_data(self,data):

        global rsb_config

        #c24f 8e00 1040 1040 c67a

        #Decode RSB word 0
        self.word0 = data[0]
        self.string_id_num = (self.word0 >> 14) & 0x0003
        self.string_id = rsb_config.string_id_map[self.string_id_num]
        self.function_num = (self.word0 >> 9) & 0x000F
        self.function = rsb_config.function_map[self.function_num] if self.function_num in rsb_config.function_map else 'Spare'
        self.address_mode_num = (self.word0 >> 6) & 0x0003
        self.address_mode = rsb_config.address_mode_map[self.address_mode_num]
        self.address0 = self.word0 & 0x001F

        #Decode RSB word 1
        self.word1 = data[1]
        self.address1_valid = ((self.word1 >> 15) & 0x0001) == 1
        self.address1 = ((self.word1 >> 8) & 0x001F) if self.address1_valid else None
        self.address2_valid = ((self.word1 >> 7) & 0x0001) == 1
        self.address2 = (self.word1 & 0x001F) if self.address2_valid else None

        #Decode RSB word 2
        self.word2 = data[2]
        self.bank0 = (self.word2 >> 8) & 0x00FF
        self.var_e = self.word2 & 0x00FF

        #Decode RSB word 3
        self.word3 = data[3]
        self.bank1 = (self.word3 >> 8) & 0x00FF
        self.var_sw = self.word3 & 0x00FF

        #Decode RSB word 4
        self.word4 = data[4]
        self.checksum = self.word4

    def generate_status(self):

        ###############################
        # Health Check = Status Flags #
        ###############################
        # XXXXXXXX_b
        # ||||||||_ { XS Command to non-XS Device
        # |||||||__ {
        # ||||||___ { Address Mode = Broadcast or Reserved
        # |||||____ { CheckSum Invalid
        # ||||_____ { XS Command to XS Device
        # |||______ {
        # ||_______ {
        # |________ {

        global rsb_config

        self.status = 0x00

        if not self.is_checksum_valid():
            self.status += 0x08

        if self.address_mode_num >= 2: #Reserved or Broadcast
            self.status += 0x04

        if self.string_id_num == 1: #A side
            if self.remote_terminal.value in [11,13,15]: #B side PAMs
                # LCCs, TMCs Cross-Strapping OK
                if self.address0 in [2,4,6,8,10,11]: #LCC 1-4 or TMC
                    self.status += 0x10
                # GID, MEDLI, PFCs Cross-Strapping NOT OK
                else:
                    self.status += 0x01

        if self.string_id_num == 2: #B side
            if self.remote_terminal.value in [10,12,14]: #A side PAMs
                # LCCs, TMCs Cross-Strapping OK
                if self.address0 in [2,4,6,8,10,11]:
                    self.status += 0x10
                # GID, MEDLI, PFCs Cross-Strapping NOT OK
                else:
                    self.status += 0x01

        if self.string_id_num == 3: #AB (both sides)
            # LCCs, TMCs Cross-Strapping OK
            if self.address0 in [2,4,6,8,10,11]:
                self.status += 0x10
            else:
                self.status += 0x01

    def parse_function(self):

        self.translated_values = [self.function]

        if self.function_num == 0: #Power switch
            self.bit_select = self.var_e
            self.bank_select = self.bank0
            self.bit_set = self.var_sw
            self.decode_power_switch()
            #self.filtered_out = False
        elif self.function_num == 1: #pyro
            self.bit_select = self.var_e & self.var_sw
            self.bank_select = self.bank0 & self.bank1
            self.decode_pyro()
            #self.filtered_out = False
        elif self.function_num == 2: #relay
            self.bit_select = self.var_sw
            self.bank_select = self.bank0
            self.duration_millis = self.bank1*40
            self.decode_relay()
            #self.filtered_out = False
        elif self.function_num == 3: #thruster
            self.bit_select = self.var_e
            self.bank_select = self.bank0
            self.duration_millis = self.var_sw * 40
            self.delay_millis = self.bank1*40
            self.decode_thruster()
            #self.filtered_out = False
        elif self.function_num == 6: #Housekeeping
            self.bit_select = self.var_e
            self.bank_select = self.bank0
            self.bit_set = self.bank1
            self.sub_function = self.var_sw
            self.decode_housekeeping()
            #self.filtered_out = False
        elif self.function_num == 7: #Discrete
            self.bit_select = self.var_e
            self.bank_select = self.bank0
            self.bit_set = self.var_sw
            self.decode_discrete()
            #self.filtered_out = False
        elif self.function_num == 8:

            if self.verbose:
                self.translated_values.extend(['','','',
                                  'VALID' if self.is_checksum_valid() else 'INVALID',
                                  'OK' if self.status == 0 else '0x%02x' % (self.status),
                                  'NO OPERATION'])
            #self.filtered_out = False
        else:
            if self.verbose:
                self.translated_values = ['','','',
                                  'VALID' if self.is_checksum_valid() else 'INVALID',
                                  'OK' if self.status == 0 else '0x%02x' % (self.status),
                                  'RESERVED OPERATION (function = %d)' % (self.function_num)]
            else:
                self.translated_values = ['RESERVED OPERATION (function = %d)' % (self.function_num)]
            #self.filtered_out = False

    def create_shorthand_prefixes(self,remote_terminal,string_id,address):

        global rsb_config

        prefixes = []
        for letter in string_id:
            prefix = remote_terminal.name[0] + letter + (rsb_config.address_map[address][-1] if address is not None else '')
            prefixes.append(prefix)
        return prefixes

    def decode_power_switch(self):

        global rsb_config

        if self.verbose:
            self.translated_values.extend([int_to_binary(self.bit_select,length=8),
                                  int_to_binary(self.bank_select,length=8),
                                  int_to_binary(self.bit_set,length=8),
                                  'VALID' if self.is_checksum_valid() else 'INVALID',
                                  'OK' if self.status == 0 else '0x%02x' % (self.status)])

        for address in (self.address0,self.address1,self.address2):
            if address is not None:
                if self.verbose:
                    self.translated_values.append(rsb_config.address_map[address])
                prefixes = self.create_shorthand_prefixes(self.remote_terminal,self.string_id,address)
                for prefix in prefixes:
                    for i in range(0,8):
                        for j in range(0,8):
                            if ((self.bank_select>>i) & 0x0001) == 1 and ((self.bit_select>>j) & 0x0001) == 1:
                                bit_bank_value = (self.bit_set>>j) & 0x0001
                                bit_bank_item = '%s-%s' % (prefix,rsb_config.function_to_table_map[self.function_num][i][j])

                                key = bit_bank_item[:-1] #Strip off the H or L...they map to the same thing
                                value = '%s (%s) = %s' % (rsb_config.load_switch_map[key] if key in rsb_config.load_switch_map else 'N/A',
                                                          bit_bank_item, bit_bank_value)

                                self.translated_values.append(value)

    def decode_pyro(self):

        global rsb_config

        if self.verbose:
            self.translated_values.extend([int_to_binary(self.bit_select,length=8),
                              int_to_binary(self.bank_select,length=8),
                              '',
                              'VALID' if self.is_checksum_valid() else 'INVALID',
                              'OK' if self.status == 0 else ('0x%02x' % (self.status))])

        #for address in (self.address0,self.address1,self.address2):
        for address in (self.address0,):
            if address is not None:
                if self.verbose:
                    self.translated_values.append(rsb_config.address_map[address])
                prefixes = self.create_shorthand_prefixes(self.remote_terminal,self.string_id,address)
                short_prefixes = self.create_shorthand_prefixes(self.remote_terminal,self.string_id,None)
                for a in range(0,len(prefixes)):
                    prefix = prefixes[a]
                    short_prefix = short_prefixes[a]
                    for i in range(8):
                        for j in range(8):
                            if ((self.bank_select>>i) & 0x0001) == 1 and ((self.bit_select>>j) & 0x0001) == 1:

                                bit_bank_item = '%s-%s' % (prefix,rsb_config.function_to_table_map[self.function_num][i][j])
                                if not bit_bank_item in rsb_config.pyro_switch_map:
                                    bit_bank_item = '%s-%s' % (short_prefix,rsb_config.function_to_table_map[self.function_num][i][j])

                                key = bit_bank_item

                                value = "%s (%s)" % (rsb_config.pyro_switch_map[key] if key in rsb_config.pyro_switch_map else 'N/A',
                                                     bit_bank_item)
                                self.translated_values.append(value)

    def decode_relay(self):

        global rsb_config

        if self.verbose:
            self.translated_values.extend([int_to_binary(self.bit_select,length=8),
                                  int_to_binary(self.bank_select,length=8),
                                  '',
                                  'VALID' if self.is_checksum_valid() else 'INVALID',
                                  'OK' if self.status == 0 else '0x%02x' % (self.status)])

        for address in (self.address0,self.address1,self.address2):
            if address is not None:
                if self.verbose:
                    self.translated_values.append(rsb_config.address_map[address])
                prefixes = self.create_shorthand_prefixes(self.remote_terminal,self.string_id,address)
                for prefix in prefixes:
                    for i in range(0,8):
                        for j in range(0,8):
                            if ((self.bank_select>>i) & 0x0001) == 1 and ((self.bit_select>>j) & 0x0001) == 1:
                                bit_bank_item = '%s-%s' % (prefix,rsb_config.function_to_table_map[self.function_num][i][j])

                                key = bit_bank_item[:-1] #Strip off the H or L...they map to the same thing
                                value = '%s (%s) for %s ms' % (rsb_config.load_switch_map[key] if key in rsb_config.load_switch_map else 'N/A',
                                                          bit_bank_item, self.duration_millis)

                                self.translated_values.append(value)

    def decode_thruster(self):

        global rsb_config

        if self.verbose:
            self.translated_values.extend([int_to_binary(self.bit_select,length=8),
                              int_to_binary(self.bank_select,length=8),
                              '',
                              'VALID' if self.is_checksum_valid() else 'INVALID',
                              'OK' if self.status == 0 else '0x%02x' % (self.status)])

        #Added address loop
        for address in (self.address0,):
            if address is not None:
                if self.verbose:
                    self.translated_values.append(rsb_config.address_map[address])
                for i in range(8):
                    for j in range(8):
                        if ((self.bank_select>>i) & 0x0001) == 1 and ((self.bit_select>>j) & 0x0001) == 1:
                            bit_bank_item = rsb_config.function_to_table_map[self.function_num][i][j]
                            bit_bank_str = '%s for %s ms after %s ms' % (bit_bank_item,self.duration_millis,self.delay_millis)
                            self.translated_values.append(bit_bank_str)

    def decode_housekeeping(self):

        global rsb_config

        #Added address loop
        for address in (self.address0,):
            if address is not None:
                if self.verbose:
                    self.translated_values.append(rsb_config.address_map[address])
                if self.sub_function in (0,2):
                    for i in range(0,8):
                        for j in range(0,8):
                            if ((self.bank_select>>i) & 0x0001) == 1 and ((self.bit_select>>j) & 0x0001) == 1:
                                bit_bank_value = (self.bit_set>>j) & 0x0001
                                bit_bank_item = rsb_config.subfunction_to_table_map[(self.function_num,self.sub_function)][i][j]
                                value = '%s = %s' % (bit_bank_item,bit_bank_value)
                                self.translated_values.append(value)

    def decode_discrete(self):

        global rsb_config

        if self.verbose:
            self.translated_values.extend([int_to_binary(self.bit_select,length=8),
                                  int_to_binary(self.bank_select,length=8),
                                  int_to_binary(self.bit_set,length=8),
                                  'VALID' if self.is_checksum_valid() else 'INVALID',
                                  'OK' if self.status == 0 else '0x%02x' % (self.status)])

        is_lcc = self.address0 in (2,4,6,8)

        for address in (self.address0,self.address1,self.address2):
            if address is not None:
                if self.verbose:
                    self.translated_values.append(rsb_config.address_map[address])
                prefixes = self.create_shorthand_prefixes(self.remote_terminal,self.string_id,address)
                for prefix in prefixes:
                    for i in range(0,8):
                        for j in range(0,8):
                            if ((self.bank_select>>i) & 0x0001) == 1 and ((self.bit_select>>j) & 0x0001) == 1:

                                bit_bank_value = (self.bit_set>>j) & 0x0001
                                bit_bank_item = rsb_config.function_to_table_map[self.function_num][i][j]
                                if is_lcc:
                                    bit_bank_item = '%s-D%s' % (prefix,bit_bank_item[-1])
                                    value = '%s (%s) = %s' % (rsb_config.load_switch_map[bit_bank_item] if bit_bank_item in rsb_config.load_switch_map else 'N/A',
                                                              bit_bank_item, bit_bank_value)
                                else:
                                    value = '%s = %s' % (bit_bank_item,bit_bank_value)

                                self.translated_values.append(value)

class RsbDecoder(buslog.decoders.GenericDecoder):

    def __init__(self):

        buslog.decoders.GenericDecoder.__init__(self)

    def decode(self,remote_terminal,sub_address,data,verbose=False):

        global watchdog_request, watchdog_response, num_to_string_id_map, num_to_function_map

        entry = RsbEntry(remote_terminal,sub_address)

        #Reject watchdog timer entries
        if data == watchdog_request or data == watchdog_response:
            entry.filtered_out = True
            return entry

        entry.verbose = verbose

        #Temporarily reject everything we don't care about for debugging
        #entry.filtered_out = True

        entry.parse_data(data)
        entry.generate_status()
        entry.parse_function()

        #Temporary rejection part #2 for debugging
        #if entry.filtered_out:
            #return entry


        if not entry.translated_values:
            return buslog.decoders.GenericDecoder.decode(self,remote_terminal,sub_address,data)

        return entry
