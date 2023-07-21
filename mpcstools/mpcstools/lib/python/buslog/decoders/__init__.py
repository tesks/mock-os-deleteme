
class GenericDecoder(object):
    
    def __init__(self):
        
        self.name = 'genericDecoder'
    
    def decode(self,remote_terminal,sub_address,data,verbose=False):
        
        global watchdog_request, watchdog_response, num_to_string_id_map, num_to_function_map

        entry = DataEntry(remote_terminal,sub_address)
        entry.verbose = verbose
        entry.parse_data(data)
        
        return_data = []
        return_data.extend(entry.translated_values)
        
        return entry
    
class DataEntry(object):
    
    def __init__(self,remote_terminal,sub_address):
        
        self.verbose = False
        self.filtered_out = False
        self.translated_values = []
        
        #Bus log entry fields
        self.remote_terminal = remote_terminal
        self.sub_address = sub_address
        
    def parse_data(self,data):
        
        self.translated_values = []
        for item in data:
            self.translated_values.append('%04x' % (item))