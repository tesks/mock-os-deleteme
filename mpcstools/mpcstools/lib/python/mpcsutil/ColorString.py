#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
**ColorString**

Adding ANSI 256 color to str.__format__

 - Example::
     print(u'{:b_Green}'.format(uColorString(u'\u2713')))

"""

from __future__ import (absolute_import, division, print_function)
import os, sys, re, json, functools, pkg_resources, six, collections, types, datetime, time, threading

from pygments import highlight
from pygments.formatters import Terminal256Formatter

import mpcsutil
import os.path

_lexers=collections.OrderedDict()

try:
    from pygments.lexers import PythonLexer
    _lexers.update({'PYTHON':PythonLexer(encoding='utf-8')})
except ImportError: pass

try:
    from pygments.lexers import PythonTracebackLexer
    _lexers.update({'TRACEBACK':PythonTracebackLexer(encoding='utf-8')})
except ImportError: pass

try:
    from pygments.lexers import YamlLexer
    _lexers.update({'YAML':YamlLexer(encoding='utf-8')})
except ImportError: pass

try:
    from pygments.lexers import JsonLexer
    _lexers.update({'JSON':JsonLexer(encoding='utf-8')})
except ImportError:
    from pygments.lexers import JavascriptLexer
    _lexers.update({'JSON':JavascriptLexer(encoding='utf-8')})

try:
    from pygments.lexers import DockerLexer
    _lexers.update({'DOCKER':DockerLexer(encoding='utf-8')})
except ImportError: pass

try:
    from pygments.lexers import BashLexer
    _lexers.update({'BASH':BashLexer(encoding='utf-8')})
except ImportError: pass



unicode = str if six.PY3 else unicode

def NamedTuple(typename, field_names, default_values=(), rename=False, mro=None):
    T = collections.namedtuple(typename, field_names, rename=rename)
    T.__new__.__defaults__ = ((None,) * len(T._fields))
    T.__new__.__defaults__ = tuple(T(**default_values) if isinstance(default_values, collections.Mapping) else T(*default_values))
    if mro: return types.ClassType(T.__name__, (mro, T), {}) if isinstance(mro,type)  else types.ClassType(mro, (T,), {})
    return T

def Enum(typename, fieldnames, shift=0, mro=None):
    _vals=[(2**vv)<<shift for vv in list(range(len(fieldnames)))]
    T = NamedTuple(typename, [ff.upper() for ff in fieldnames], _vals, mro)
    T._m=dict(zip(_vals,fieldnames))
    T._get_int=lambda x,y:y if isinstance(y, int) else x.__getattribute__(y.upper())
    T._get_str=lambda x,y:([x._m.get(kk) for kk in x._m.keys() if kk&y]+[None])[0] if isinstance(y,int) else (x._get_str(x._get_int(y)) if isinstance(y, six.string_types) else None)
    T.get=lambda x,y:x._get_str(y) if isinstance(y, int) else x._get_int(y.upper())
    T.__call__=lambda x,y:x.get(y)
    return T

PrettyTypes=Enum('PrettyTypes', _lexers.keys())()

lexer_map=collections.OrderedDict([(_type, _lexers.get(PrettyTypes(_type)) ) for _type in PrettyTypes])


def makedirs(*dirs):
    _umask = os.umask(0)
    try:
        [os.makedirs(_dir, 0o0775) for _dir in dirs if (_dir and not(os.path.exists(_dir)))]
    finally:
        os.umask(_umask)
        return dirs[0] if (len(dirs)==1) else dirs

colors={kk:ii-1 for ii,kk in enumerate(["none","black","red","green","yellow","blue","magenta","cyan","white"])}
start,stop,endmarks = "\033[","\033[0m",{8: ";", 256: ";38;5;"}

colors = ['black', 'red', 'green', 'yellow', 'blue', 'violet', 'cyan', 'white']
ansi_dict={ground:{brightness:{color:'\x1b[{bb};{gg}{cc}m'.format(bb=bb,gg=gg+3,cc=cc) for cc,color in enumerate(colors)} for bb,brightness in enumerate(['dull','bright'])} for gg,ground in enumerate(['fg','bg'])}
styles={kk:ii for ii,kk in enumerate(["normal","bold","faint","italic","underline","blink","rapid_blink","reverse","conceal"]) }

class ColorString(str):
    color_def=property(lambda x: x.__dict__.setdefault('_color_def',{kk:getattr(x.__class__,kk) for kk,vv in zip(colors+['k']+map(lambda y:y[0],colors[1:])+['m'], map(lambda y: getattr(x,y), colors*2+['violet']))}), lambda x,y: x.__dict__.update({'_color_def':y}) )
    def __format__(self, spec, *args, **kwargs): return getattr(self.__class__,spec)(self) if hasattr(self.__class__, spec) else super(ColorString, self).__format__(spec, *args, **kwargs)

class uColorString(unicode):
    def __format__(self, spec, *args, **kwargs): return getattr(self.__class__,spec)(self) if hasattr(self.__class__, spec) else super(uColorString, self).__format__(spec, *args, **kwargs)

def colorsure(color, ground='fg', ness='bright', base=8):
    _ansi = ansi_dict.get(ground.lower(),{}).get(ness.lower(),{}).get(color,'\x1b[0m')
    return lambda x: ColorString('{}{}{}'.format(_ansi, x if isinstance(x, ColorString) else ColorString(x), '\x1b[0m'))
[setattr(ColorString,kk,vv) for kk,vv in {kk: colorsure(vv) for kk,vv in zip(colors+['k']+list(map(lambda y:y[0],colors[1:]))+['m'], colors*2+['violet'])}.items()]

def ucolorsure(color, ground='fg', ness='bright', base=8):
    _ansi = ansi_dict.get(ground.lower(),{}).get(ness.lower(),{}).get(color,'\x1b[0m')
    return lambda x: uColorString('{}{}{}'.format(_ansi, x if isinstance(x, uColorString) else uColorString(x), '\x1b[0m'))
[setattr(uColorString,kk,vv) for kk,vv in {kk: ucolorsure(vv) for kk,vv in zip(colors+['k']+list(map(lambda y:y[0],colors[1:]))+['m'], colors*2+['violet'])}.items()]

_ansi_256=None
_user_ansi_json=os.path.join(makedirs(os.path.expanduser('~/CHILL')), 'ansi_256.json')
resource_path = '/'.join(['..','data', 'ansi_256.json'])
if pkg_resources.resource_exists(__name__, resource_path):
    _ansi_256=json.load(pkg_resources.resource_stream(__name__, resource_path))
else:
    if os.path.isfile(_user_ansi_json):
        # load from CHILL if present
        with open(_user_ansi_json,'r') as ff: _ansi_256=json.load(ff)
    else:
        try:
            # MPCS-12019 load from local data
            file = os.path.join(mpcsutil.config.GdsConfig().getProperty('GdsDirectory') , 'config/', 'ansi_256.json')
            with open(file,'r') as ff: _ansi_256=json.load(ff)
            # save to CHILL
            with open(_user_ansi_json, 'w') as ff:
                json.dump(_ansi_256, ff)
        except: pass

if _ansi_256:
    _ansi_by_name={dd.get('name'):dd.get('colorId') for dd in _ansi_256}
    _ansi_by_hex={dd.get('hexString').lower():dd.get('colorId') for dd in _ansi_256}
    mod_key=dict(zip(["normal","bold","faint","italic","underline","blink","rapid_blink","reverse","conceal"],
        [lambda x:x,lambda x:x.upper(),lambda x:'f_{}'.format(x),lambda x:'i_{}'.format(x),lambda x:'u_{}'.format(x),lambda x:'b_{}'.format(x),lambda x:'rb_{}'.format(x),lambda x:'r_{}'.format(x),lambda x:'c_{}'.format(x)]))
    cstr=lambda x:x if isinstance(x, ColorString) else ColorString(x)
    def from_hex(hex_string, style):
        _style=style
        def _wrap(text): return ColorString('{start}{ss}{em}{cc}m{text}{stop}'.format(**dict(start=start, ss=styles.get(_style), em=endmarks[256], cc=_ansi_by_hex.get(hex_string,238), text=text if isinstance(text, ColorString) else ColorString(text), stop=stop)))
        return _wrap
    def from_name(name, style):
        _style=style
        def _wrap(text): return ColorString('{start}{ss}{em}{cc}m{text}{stop}'.format(**dict(start=start, ss=styles.get(_style), em=endmarks[256], cc=_ansi_by_name.get(name,238), text=text if isinstance(text, ColorString) else ColorString(text), stop=stop)))
        return _wrap
    def _as_style(style='normal', dd=None):
        if dd is None: dd={}
        dd.update(dict({mod_key.get(style,lambda x:x)(kk):from_hex(kk,style) for kk in _ansi_by_hex.keys()},**{mod_key.get(style,lambda x:x)(kk):from_name(kk,style) for kk in _ansi_by_name.keys()}))
        return dd
    _ansi_256_defs=functools.reduce(lambda a,b:_as_style(b,a), styles.keys(), {})
    [setattr(ColorString,kk,staticmethod(vv)) for kk,vv in _ansi_256_defs.items()]


    ucstr=lambda x:x if isinstance(x, uColorString) else uColorString(x)
    def from_hex(hex_string, style):
        _style=style
        def _wrap(text): return uColorString(u'{start}{ss}{em}{cc}m{text}{stop}'.format(**dict(start=start, ss=styles.get(_style), em=endmarks[256], cc=_ansi_by_hex.get(hex_string,238), text=text if isinstance(text, uColorString) else uColorString(text), stop=stop)))
        return _wrap
    def from_name(name, style):
        _style=style
        def _wrap(text): return uColorString(u'{start}{ss}{em}{cc}m{text}{stop}'.format(**dict(start=start, ss=styles.get(_style), em=endmarks[256], cc=_ansi_by_name.get(name,238), text=text if isinstance(text, uColorString) else uColorString(text), stop=stop)))
        return _wrap
    def _as_style(style='normal', dd=None):
        if dd is None: dd={}
        dd.update(dict({mod_key.get(style,lambda x:x)(kk):from_hex(kk,style) for kk in _ansi_by_hex.keys()},**{mod_key.get(style,lambda x:x)(kk):from_name(kk,style) for kk in _ansi_by_name.keys()}))
        return dd
    _ansi_256_defs=functools.reduce(lambda a,b:_as_style(b,a), styles.keys(), {})
    [setattr(uColorString,kk,staticmethod(vv)) for kk,vv in _ansi_256_defs.items()]

else:
    import warnings
    warnings.warn('Did not find {}'.format(_ansi_json))





def pretty_pyg(_obj, _type=None, _stringify=None):
    _ew=lambda y:'{}'.format(json.dumps(y,default=lambda x:x.__dict__ if hasattr(x, '__dict__') else str(x),indent=4))
    _simple=lambda x:'{}'.format(x)
    _ff=Terminal256Formatter(encoding="utf-8", style='monokai')

    _ll=lexer_map.get(PrettyTypes.JSON)
    if _type:
        if isinstance(_type, int) and (_type in PrettyTypes):
            _ll=lexer_map.get(_type)
        elif isinstance(_type, six.string_types) and (_type.upper() in PrettyTypes._fields):
            _ll=lexer_map.get(PrettyTypes(_type.upper()))
        else:
            _ll=lexer_map.get(PrettyTypes.JSON)

    _stringify = _stringify if _stringify else (ColorString if isinstance(_obj, six.string_types) else _ew)
    return highlight(_stringify(_obj), _ll, _ff ).decode('utf-8')

emojis=collections.OrderedDict([
    ('strike', u"\U00002718"),
    ('check', u"\U00002714"),
    ('poop', u"\U0001F4A9"),
    ('star', u"\U00002B50"),
    ('sparkles', u"\U00002728"),
    ('dizzy', u"\U0001F4AB"),
    ('fire',u"\U0001F525"),
    ('comet',u"\U00002604"),
    ('snow',u"\U00002744"),
    ('zap',u"\U000026A1"),
    ('rainbow',u"\U0001F308"),
    ('chill',u"\U0001F32C"),
    ('luck',u"\U0001F340"),
    ('coronavirus',u"\U0001F9A0"),
    ('bang',u"\U0001F4A5"),
    ('trophy',u"\U0001F3C6"),
    ('satellite',u"\U0001F6F0"),
    ('rocket',u"\U0001F680"),
    ('ufo',u"\U0001F6F8"),
    ('spock',u"\U0001F596"),
    ('milky_way',u"\U0001F30C"),
    ('dish',u"\U0001F4E1"),
    ('party',u"\U0001F389"),
    ('clock0100',u'\U0001f550'),
    ('clock0130',u'\U0001f55c'),
    ('clock0200',u'\U0001f551'),
    ('clock0230',u'\U0001f55d'),
    ('clock0300',u'\U0001f552'),
    ('clock0330',u'\U0001f55e'),
    ('clock0400',u'\U0001f553'),
    ('clock0430',u'\U0001f55f'),
    ('clock0500',u'\U0001f554'),
    ('clock0530',u'\U0001f560'),
    ('clock0600',u'\U0001f555'),
    ('clock0630',u'\U0001f561'),
    ('clock0700',u'\U0001f556'),
    ('clock0730',u'\U0001f562'),
    ('clock0800',u'\U0001f557'),
    ('clock0830',u'\U0001f563'),
    ('clock0900',u'\U0001f558'),
    ('clock0930',u'\U0001f564'),
    ('clock1000',u'\U0001f559'),
    ('clock1030',u'\U0001f565'),
    ('clock1100',u'\U0001f55a'),
    ('clock1130',u'\U0001f566'),
    ('clock1200',u'\U0001f55b'),
    ('clock1230',u'\U0001f567'),])

def _clock(delay=None, duration=None, kill_event=None):
    delay=delay if delay else 0.3
    _sequence=[uColorString(vv) for kk,vv in emojis.items() if re.match(r'^clock.*$', kk)]
    # print(u'sequence:\n\t{}'.format(u'\n\t'.join(uColorString(_element) for _element in _sequence) ))

    if kill_event is None:
        duration=duration if duration else 30
        kill_event=threading.Event()
        _kill = lambda : kill_event.set()
        threading.Timer(duration, _kill).start()

    def _iterate():
        for _element in _sequence:
            if kill_event.is_set(): raise StopIteration
            print(u'\r{}'.format(uColorString(_element)), end='')
            time.sleep(delay)

    def _run():
        while not(kill_event.is_set()):
            try: _iterate()
            except KeyboardInterrupt:
                return print('Caught KeyboardInterrupt')
            except StopIteration:
                return
    _run()

def test():
    print('{:Orange4}'.format(ColorString('poop')))
    print(u':heavy_ballot_x: {:b_Red}'.format(uColorString(u'\u2718'))) # ballot_x :: ✗
    print(u':heavy_check: {:b_Green}'.format(uColorString(u'\u2714'))) # check :: ✓
    [print(u':{}: {}'.format(kk, uColorString(vv))) for kk,vv in emojis.items()]
    _clock()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
