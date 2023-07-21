#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import bisect

def binary_search(sorted_list,search_val):

    low = 0
    high = len(sorted_list)

    while low < high:

        mid = (low + high)//2
        midval = sorted_list[mid]

        if midval < search_val:
            low = mid+1
        elif midval > search_val:
            high = mid
        else:
            return mid

    raise ValueError("Value %s does not exist in the input list.")

def binary_closest_value(sorted_list,search_val):

    index = bisect.bisect_left(sorted_list,search_val)
    #index = numpy.searchsorted(sorted_list,search_val,side='left')

    if index <= 0:
        return 0
    elif index >= len(sorted_list)-1:
        return len(sorted_list)-1

    low_value = sorted_list[index-1]
    high_value = sorted_list[index]

    if abs(search_val-low_value) <= abs(high_value-search_val):
        return index-1

    return index

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
