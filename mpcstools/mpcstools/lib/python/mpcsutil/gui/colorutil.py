#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)
import types
import six
import matplotlib.colors as colors
from mpcsutil.kdtree import KDTree

class IllegalArgumentError(ValueError):
    pass

def strToRgb(arg):
    if arg is None:
        return None

    if isinstance(arg, six.string_types):
        if arg.startswith('#'):
            return hexToRgb(arg)
        else:
            rgb = strToRgb(colors.cnames.get(arg))
            if rgb is None:
                rgb = [int(x) for x in arg.split(',')]
    elif isinstance(rgb, tuple):
        rgb = [int(x) for x in arg]
    else:
        raise IllegalArgumentError("Invalid argument type: {}. Must be 'string', 'unicode' or 'tuple'.".format(type(arg)))
    return rgb

def rgbToHex(rgb):
    return "#%02X%02X%02X" % strToRgb(rgb)

def hexToRgb(rgb):
    if not(isinstance(rgb, six.string_types)):
        raise IllegalArgumentError("Invalid argument type: {}. Must be 'string', 'unicode' or 'tuple'.".format(type(rgb)))
    if not rgb.startswith('#'):
        raise IllegalArgumentError("Invalid argument format: {}. Must begin with a '#' character.".format(rgb))
    try:
        return int(rgb[1:3], 16), int(rgb[3:5], 16), int(rgb[5:7], 16)
    except:
        raise IllegalArgumentError("Invalid argument format: {}. Must contain three, 8-bit, hexadecimal values preceeded by a '#' character.".format(rgb))

def rgbHexToName(arg):
    return rgbToName(strToRgb(arg))

def rgbToName(rgb):
    adjusted_rgb, name, distance = KD_TREE.nearest_neighbor(rgb)
    return name

KD_TREE = KDTree(3, [(hexToRgb(v), k) for k, v in colors.cnames.items()])

def test():
    for name in colors.cnames.keys():
        rgb_hex = colors.cnames[name]
        print(strToRgb(rgb_hex))
        retrieved_name = rgbToName(hexToRgb(rgb_hex))
        print('{} = {}'.format(retrieved_name, name))

    names = set()
    for r in range(0, 255):
        print('r={}'.format(r))
        for g in range(0, 255):
            for b in range(0, 255):
                names.add(rgbToName((r, g, b)))

    print(len(names) == len(colors.cnames))
    assert len(names) == len(colors.cnames)

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
