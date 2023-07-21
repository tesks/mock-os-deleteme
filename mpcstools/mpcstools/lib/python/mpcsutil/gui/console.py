#! /usr/bin/env python
# -*- coding: utf-8 -*-

"""
Created on May 14, 2010

"""

from __future__ import (absolute_import, division, print_function)
import six
if six.PY2:
    import Tkinter as tk
    import Queue
else:
    import tkinter as tk
    import queue as Queue

import sys
import os
import mpcsutil.tail
import threading

class ReadOnlyText(tk.Text):
    '''A Tkinter Text widget that provides a scrolling display of console
    stderr and stdout.'''

    def __init__(self, master=None, cnf={}, **kw):
        '''See the __init__ for Tkinter.Text for most of this stuff.'''

        tk.Text.__init__(self, master, cnf, **kw)

        self.started = False

        self.tag_configure('NORMAL',background='white',foreground='black')
        self.tag_configure('ERROR',background='white',foreground='red')

        self.config(state=tk.NORMAL) #leave the field editable
        self.bind('<Key>',lambda e: 'break') #ignore all key presses

    def start(self):

        if self.started:
            return

        self.started = True
        runner = threading.Thread(target=self._print_loop,name='display_thread',args=())
        runner.start()

    def stop(self):

        if not self.started:
            return

        self.started = False

    def clear(self):

        self.delete("1.0", "end")
        self.mark_set("insert", "1.0")
        self.see("end")

    def write(self,val,is_error=False):

        pass

    def _insert_text(self,val,is_error=False):

        #IMPORTANT: Leave the self.config(state=...) statements
        #commented out.  They cause segmentation faults on RHEL5.

        #self.config(state=tk.NORMAL)
        self.insert('end',val,'ERROR' if is_error else 'NORMAL')
        self.see('end')
        #self.config(state=tk.DISABLED)

    def _print_loop(self):

        pass

class Tee(object):
    def __init__(self, queue, is_stde):
        self.queue=queue
        self.is_stde = is_stde
        self.file=open(os.path.expanduser('~/CHILL/console.stdeo.log'),'w')
        self.file.fileno()
        if is_stde:
            self.stream = sys.stderr
            sys.stderr = self

        else:
            self.stream = sys.stdout
            sys.stdout=self

    def __del__(self):
        if self.is_stde:
            sys.stderr = self.stream
        else:
            sys.stdout = self.stream
        self.file.close()

    def write(self, data):
        self.stream.write(data)
        self.queue.put_nowait((data, self.is_stde))
        self.file.write(data)
        self.flush()

    def fileno(self):
        return self.file.fileno()

    def flush(self):
        self.stream.flush()
        self.file.flush()

class ConsoleText(ReadOnlyText):
    '''A Tkinter Text widget that provides a scrolling display of console
    stderr and stdout.'''

    def __init__(self, master=None, cnf={}, **kw):
        '''See the __init__ for Tkinter.Text for most of this stuff.  The only
        thing I've added is a keyword option called "dual_mode".  By default this
        is false and when the "start()" method is called stdout/stderr will be stolen from
        the console and sent ONLY to this GUI.  If "dual_mode" is True, then stdout/stderr will
        be written to this widget, but will still go to the console as well.'''

        ReadOnlyText.__init__(self, master, cnf, **kw)

        self.input_queue = Queue.Queue()

    def start(self):
        
        ReadOnlyText.start(self)
        self.stdo_tee = Tee(self.input_queue, False)
        self.stde_tee = Tee(self.input_queue, True)

    def write(self,val,is_error=False):

        self.input_queue.put_nowait((val,is_error))

    def _print_loop(self):

        while self.started:

            val,is_error = self.input_queue.get()
            self._insert_text(val, is_error)
            self.input_queue.task_done()

        while not self.input_queue.empty():
            val,is_error = self.input_queue.get()
            self._insert_text(val, is_error)


class FileText(ReadOnlyText):
    '''A Tkinter Text widget that provides a scrolling display of a file.'''

    def __init__(self, master=None, cnf={}, **kw):

        ReadOnlyText.__init__(self, master, cnf, **kw)

        self.filename = None

    def _print_loop(self):

        if self.filename is None:
            self.started = False
            return

        t = mpcsutil.tail.Tail(self.filename)
        while self.started:
            line = t.nextline()
            test_line = line.strip().upper()
            self._insert_text(line,
                              is_error=test_line.startswith('CRITICAL') or
                              test_line.startswith('ERROR') or
                              test_line.startswith('WARN'))


def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
