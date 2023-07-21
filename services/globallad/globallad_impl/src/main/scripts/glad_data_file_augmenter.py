import os
import tempfile
import struct

def _replace_session(input_file, adjust):
    '''Reads the data from the input file and replaces the session number of all of the global lad
    data words, writes it to a temp file and returns the file object.  Assumes the data file starts with a glad sync word and there 
    are no issues with the file.
    4 sync word
    2 word length
    33 unchanged
    4 session number.
    length - 2 - 33 - 4 : The rest of the data.
    '''
    advance = 1+8+4+4+8+4+8+4
    with tempfile.NamedTemporaryFile("w+b", delete=False) as new_data_file:
        with open(input_file, "rb") as ip:
            bcount = os.fstat(ip.fileno()).st_size
            data_count = 0
            
            while ip.tell() < bcount:
                new_data_file.write(ip.read(4))
                bytes = ip.read(2)
                size, = struct.unpack(">H", bytes)

                new_data_file.write(bytes)
                new_data_file.write(ip.read(advance))
                
                bytes = ip.read(4)
                sid, = struct.unpack(">I", bytes)
                # Write the adjusted sid.
                new_data_file.write(struct.pack(">I", sid + adjust))
                
                # Adjust the size for advance, the size bytes and the sid and write the rest of the word.
                new_data_file.write(ip.read(size - advance - 2 - 4 ))
                
                data_count += 1
            
            print "Rewrote %d records adjusting session by %d to temp file %s" % (data_count, adjust, new_data_file.name)
            return new_data_file
            
            
if __name__ == "__main__":
    _replace_session("/Users/triviski/GLAD_BACKUPS/test_samples/glad_backup_1444935340019.backup", 100)
    _replace_session("/Users/triviski/GLAD_BACKUPS/test_samples/glad_backup_1444935340019.backup", 200)
    _replace_session("/Users/triviski/GLAD_BACKUPS/test_samples/glad_backup_1444935340019.backup", 300)
    
    
