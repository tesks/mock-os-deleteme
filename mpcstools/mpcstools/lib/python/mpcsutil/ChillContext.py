#! /usr/bin/env python
# -*- coding: utf-8 -*-
# from __future__ import (absolute_import, division, print_function)
from __future__ import print_function
import os, sys, re, json, datetime, getpass, six, traceback, logging, time, collections
# import xml.etree.ElementTree as ET

from mpcsutil import (config, ChillBase)

class ChillBase(object):
    def __init__(self, *args, **kwargs):
        [self.__setattr__(kk,vv) for kk,vv in kwargs.items()]
    def __call__(self, *args, **kwargs):
        [self.__setattr__(kk,vv) for kk,vv in kwargs.items()]
        return self
    def __getattr__(self, name):
        return self.__dict__.get(name, None)
    def _asdict(self):
        _props = list(filter(lambda x: isinstance(getattr(self.__class__,x),property), dir(self.__class__)))
        _keys = list(filter(lambda x: not(x.startswith('_')), self.__dict__.keys()))
        _dd=collections.OrderedDict([(kk,getattr(self, kk)) for kk in _keys + _props])
        __dd=collections.OrderedDict([(kk,(lambda x:x._asdict() if hasattr(x,'_asdict') else (x.name if hasattr(x, 'name') else x))(getattr(self, kk))) for kk in sorted(_dd.keys()) if (not(kk.startswith('_')) and (getattr(self, kk) is not(None)))])
        return __dd
    def _asjson(self): return json.dumps(self._asdict(), indent=4)
    def __format__(self, spec): return '{}'.format(self._asjson())
    def makedirs(self, *dirs):
        _umask = os.umask(0)
        try:
            [os.makedirs(_dir, 0o0775) for _dir in dirs if (_dir and not(os.path.exists(_dir)))]
        finally:
            os.umask(_umask)
            return dirs[0] if (len(dirs)==1) else dirs


class ChillContext(ChillBase):
    gds_dir=property(
        lambda x:x.__dict__.setdefault('_gds_dir', os.environ.setdefault(x.gds_env_var, os.path.abspath(os.path.join(os.path.dirname(__file__), '..','..'))).rstrip('/')),
        lambda x,y: x.__dict__.update({'_gds_dir':y}),
        doc = 'GDS Base Directory')
    home_dir=property(
        lambda x:x.__dict__.setdefault('_home_dir', os.path.expanduser('~/')),
        lambda x,y: x.__dict__.update({'_home_dir':y}),
        doc='User home directory')
    user=property(
        lambda x:x.__dict__.setdefault('_user', getpass.getuser()),
        lambda x,y: x.__dict__.update({'_user':y}))
    gds_env_var = property(
        lambda x:x.__dict__.setdefault('_gds_env_var', 'CHILL_GDS'),
        lambda x,y: x.__dict__.update({'_gds_env_var':y}))
    user_config_env_var = property(
        lambda x:x.__dict__.setdefault('_user_config_env_var', 'GdsUserConfigDir'),
        lambda x,y: x.__dict__.update({'_user_config_env_var':y}))
    log_config_filename=property(
        lambda x:x.__dict__.setdefault('_log_config_filename', 'python-logging.properties'),
        lambda x,y: x.__dict__.update({'_log_config_filename':y}))
    msg=property(
        lambda x:x.__dict__.setdefault('_msg', ''),
        lambda x,y: x.__dict__.update({'_msg':y}))
    release_filename=property(
        lambda x:x.__dict__.setdefault('_release_filename', 'release.properties'),
        lambda x,y: x.__dict__.update({'_release_filename':y}))
    release_file=property(
        lambda x:x.__dict__.setdefault('_release_file', os.path.join(x.config_dir, x.release_filename)),
        lambda x,y: x.__dict__.update({'_release_file':y}),
        doc='Release properties file')
    release=property(
        lambda x:x.__dict__.setdefault('_release', config.ReleaseProperties()),
        lambda x,y: x.__dict__.update({'_release':y}))
    prop_dump_app=property(
        lambda x:x.__dict__.setdefault('_prop_dump_app', 'chill_property_dump'),
        lambda x,y: x.__dict__.update({'_prop_dump_app':y}))

    bin_dir =           property( lambda x: os.path.join(x.gds_dir, 'bin') , doc='MPCS bin directory')
    config_dir =        property( lambda x: os.path.join(x.gds_dir, 'config') , doc='MPCS configuration directory')
    image_dir =         property( lambda x: os.path.join(x.config_dir, 'images') , doc = 'MPCS images directory')
    admin_dir =         property( lambda x: os.path.join(x.bin_dir, 'admin') , doc = 'MPCS admin directory')
    tools_dir =         property( lambda x: os.path.join(x.bin_dir, 'tools') , doc='MPCS tools directory')
    internal_dir =      property( lambda x: os.path.join(x.bin_dir, 'internal') , doc='MPCS internal directory')
    lib_dir =           property( lambda x: os.path.join(x.gds_dir, 'lib') , doc='MPCS lib directory')
    python_lib_dir =    property( lambda x: os.path.join(x.lib_dir, 'python') , doc='MPCS python lib directory')
    template_dir =      property( lambda x: os.path.join(x.gds_dir, 'templates') , doc = 'MPCS template directory')
    schema_dir =        property( lambda x: os.path.join(x.gds_dir, 'schema') , doc='MPCS schema directory')

    @property
    def mpcs_version(self):
        if self._mpcs_version is None:
            self.mpcs_version = self.release.version.split('.')
        return self._mpcs_version
    @mpcs_version.setter
    def mpcs_version(self, value):

        _m_digit = lambda x: re.match(r'^(?P<digit>\d+).*$', x)
        _x_digit = lambda y: int(y.groupdict().get('digit')) if y else None
        _z_digit = lambda z: _x_digit(_m_digit(z))
        self._mpcs_version = list(filter(lambda x: x is not(None), map(_z_digit, value)))


    @property
    def user_config_dir(self):
        """User config directory"""
        if self._user_config_dir is(None):
            _default = os.path.join(self.home_dir, 'CHILL')
            _override = os.environ.get(self.user_config_env_var)
            if _override:
                self.msg = '{}The environment variable {} has been set!\n'.format(self.msg, self.user_config_env_var)
            else:
                self.msg = '{}The environment variable {} has not been set.\n'.format(self.msg, self.user_config_env_var)
            self.user_config_dir = _override if _override else _default
            self.msg = '{}GDS User Config Directory set to {}\n'.format(self.msg, self._user_config_dir)
        return self._user_config_dir
    @user_config_dir.setter
    def user_config_dir(self, value): self._user_config_dir = self.makedirs(value)

    @property
    def log_config_file(self):
        """User logging file"""
        if self._log_config_file is(None):
            _default = os.path.join(self.user_config_dir, self.log_config_filename)
            if os.path.exists(_default):
                self.log_config_file = _default
            else:
                self.msg = "{}Cannot find the log config '{}'. Searching system config directory {}\n".format(self.msg,self.log_config_filename, self.config_dir)
                _default = os.path.join(self.config_dir, self.log_config_filename)
                if not os.path.exists(_default):
                    raise RuntimeError('Cannot find the required file: {}'.format(self.log_config_filename))
                self.log_config_file = _default

            logging.addLevelName(100, 'MAXIMUM')
            logging.config.fileConfig(self._log_config_file)
            logging.getLogger('mpcs.util').debug("{}Using log config {}".format(self.msg, self.log_config_file))
            self.msg = ''

        return self._log_config_file
    @log_config_file.setter
    def log_config_file(self, value): self._log_config_file = value

    @property
    def prop_dump_script(self):
        """MPCS property dump script"""
        if self._prop_dump_script is None: self.prop_dump_script=os.path.join(self.internal_dir, self.prop_dump_app)
        return self._prop_dump_script
    @prop_dump_script.setter
    def prop_dump_script(self, value):
        self._prop_dump_script=value
        if not(os.path.exists(value)):
            _msg = 'Cannot find the required script: {}'.format(value)
            logging.getLogger('mpcs.util').critical(_msg)
            raise RuntimeError(_msg)
    def _log_config(self):
        _props=list(filter(lambda x: isinstance(getattr(self.__class__,x),property), dir(self.__class__)))
        [logging.getLogger('mpcs.util').debug('{} is {}'.format(getattr(self.__class__, _pp).__doc__, getattr(self, _pp))) for _pp in _props]


    def __init__(self, *args, **kwargs):
        super(ChillContext, self).__init__(*args, **kwargs)
        os.environ.update({'TZ':'UTC'})
        time.tzset()
        self._log_config()
        print('{}'.format(self.release))
        logging.getLogger('mpcs.util').critical('POOP')


def test(*args, **kwargs): print('{}'.format(ChillContext(*args, **kwargs)()._asdict()))
def main(*args, **kwargs): return test(*args, **kwargs)
if __name__ == "__main__": main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
