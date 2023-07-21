#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
This module does all the setup for the MPCS perspective python library
and contains global variables and constants for perspective file generation.
"""

from __future__ import (absolute_import, division, print_function)

import mpcsutil
import os


# The GDS configuration object
_gdsConfig = mpcsutil.config.GdsConfig()

# Fixed view default constants
DEFAULT_FIXED_WIDTH = 800
DEFAULT_FIXED_HEIGHT = 660
FIXED_LAYOUT_VERSION = "2"
FIXED_VIEW_CLASS = "jpl.gds.monitor.guiapp.gui.views.FixedLayoutComposite"

# The current default FSW dictionary directory
fswDefaultDictDir = None

# The current default FSW dictionary version
fswDefaultVersion = 'generic'

# The current default SSE dictionary directory
sseDefaultDictDir = None

# The current default SSE dictionary version
sseDefaultVersion = 'genericsse'

def setDictionaryDefaults():
    ''' Sets the global variables for default dictionary directories and versions
    by scanning the configured dictionary directories and getting the default
    versions.'''

    global fswDefaultDictDir, sseDefaultDictDir, fswDefaultVersion, sseDefaultVersion, PIXEL, CHARACTER

    fswDefaultDictDir = os.path.join(_gdsConfig.getDefaultFswDictDir(), _gdsConfig.getMission())
    _defaultVer = _gdsConfig.getDefaultFswVersion()
    #fswDefaultVersion = _getDefaultDictVersion(fswDefaultDictDir, _defaultVer)

    if (_gdsConfig.hasSse()):
        sseDefaultVersion = _gdsConfig.getDefaultSseVersion()
        sseDefaultDictDir = os.path.join(_gdsConfig.getDefaultSseDictDir(), "{}sse".format(_gdsConfig.getMission()))
        _defaultVer = _gdsConfig.getDefaultSseVersion()
        #sseDefaultVersion = _getDefaultDictVersion(sseDefaultDictDir, _defaultVer)


def _getDefaultDictVersion(directory, defaultDir):
    '''Gets the default version directory from the specified dictionary directory.

        Args
        -----
        directory - The dictionary directory path to search, including the terminating mission name. (string)
        defaultDir - The default version. If this version is found, it will be returned.  May be None. (string)

        Returns
        --------
        The default dictionary version. May be None.'''

    if directory is None:
        raise mpcsutil.err.InvalidInitError('The directory name cannot be None.')

    _productDir = _gdsConfig.getProperty('dictionary.product.flight.fileName', 'products')

    _subDirs = os.listdir(directory)
    _goodSubDirs = []

    for _versionDir in _subDirs:

        if _versionDir == defaultDir:
            return defaultDir

        _wholeDir = directory + '/' + _versionDir
        if str(_versionDir).startswith('.') or _versionDir == 'CVS' or  _versionDir == '.svn' or \
            _versionDir == _productDir or os.path.isdir(_wholeDir) == False:
            continue

        _goodSubDirs.append(_versionDir)
        _goodSubDirs.sort()
        _goodSubDirs.reverse()

    if len(_goodSubDirs) == 0:
        return None
    else:
        return _goodSubDirs[0]



# Set the default dictionary global variables
setDictionaryDefaults()

# import all modules in the package so the user does not have to
from perspective import actiontypes, channelfieldtypes, coordinatetypes, fixedfields, fixedfieldtypes, fixedlayout, graphics, linestyletypes, timetypes
__all__ = ['actiontypes','channelfieldtypes','coordinatetypes','fixedfields','fixedfieldtypes','fixedlayout','graphics','linestyletypes','timetypes']

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
