#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module defines encryption-related functions and values.

Created on Sep 14, 2010

"""

from __future__ import (absolute_import, division, print_function)

import binascii
import pyDes


# MPCS-9893  6/6/18.  Revert to R7 value here.
# MPCS-12476 - 7/6/21. Update key to R8 value
#  Key must be either 16 or 24 characters long, so it must truncated
key = b'jpl.gds.shared.string.St'

def triple_des_decrypt_from_base64_str(base64str):
    '''Decrypt data that has been encrypted using Triple DES.

    Parameters
    -----------
    base64str - The data to decrypt in base-64 format. (string)

    Returns
    --------
    Decrypted data.'''

    if base64str is not None:
        k = pyDes.triple_des(key, pyDes.CBC, b'\0\0\0\0\0\0\0\0', pad=None, padmode=pyDes.PAD_PKCS5)
        # Convert Base64 string representation to binary data, but pad to match multiples of 4 bytes
        data = binascii.a2b_base64(base64str)
        decrypted = k.decrypt(data, padmode=pyDes.PAD_PKCS5)
        return decrypted

    return None

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
