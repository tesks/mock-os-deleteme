#! /usr/bin/env python
# -*- coding: utf-8 -*-



from __future__ import (absolute_import, division, print_function)
import sys
# sys.stdin = open('/dev/tty')
# import bdb
import pdb
import os.path
import Pmw
# import sys
import threading
import time
import traceback, sys
import six

if six.PY2:
    import Tkinter as tk
else:
    import tkinter as tk

import mpcsutil
import mpcsutil.gui.tabbedpages as tabbedpages
import mpcsutil.gui.colorizer as colorizer
import mpcsutil.gui.console as console

#Turn this on for debugging output
DEBUG = False

# class ScriptExecutor(bdb.Bdb):
class ScriptExecutor(pdb.Pdb):

    def __init__(self,filename,gui):

        global DEBUG
        self._debug = DEBUG

        # bdb.Bdb.__init__(self)
        super(ScriptExecutor, self).__init__(stdout=sys.__stdout__)

        self.running = False
        self.action_sync = threading.Condition()
        self.filename = filename
        self.gui = gui

        self.curfile = None
        self.curline = None
        self.prevfile = None
        self.prevline = None
        self.curframe = None
        self.curtraceback = None

        self.run_block = True
        self.run_until_line = None
        self.run_until_file = None

        self.session_config = None
        self.previous_button_state = 'stopped'

        self.quit_called = False
        self.mtak_running = False


    def reset(self):

        # import bdb
        import pdb

        self._remove_previous_highlight()

        pdb.Pdb.reset(self)
        # self.reset()

        self.curfile = None
        self.curline = None
        self.prevfile = None
        self.prevline = None
        self.curframe = None
        self.curtraceback = None

        self.run_block = True
        self.run_until_line = None
        self.run_until_file = None

        self.session_config = None
        self.quit_called = False

    def _run(self):
        # The script has to run in __main__ namespace (or imports from
        # __main__ will break).
        #
        # So we clear up the __main__ and set several special variables
        # (this gets rid of pdb's globals and cleans old variables on restarts).
        #
        # Be aware that this has the wonderful side effect of this class not being able
        # to see any of the global imports that we use elsewhere in this module, so we have
        # to dynamically do imports in other methods as we need them. (brn)

        if six.PY2:
            import __main__, __builtin__
            __main__.__dict__.clear()
            __main__.__dict__.update({"__name__"    : "__main__",
                                      "__file__"    : self.filename,
                                      "__builtins__": __builtin__,
                                     })
        else:
            import __main__, builtins
            __main__.__dict__.clear()
            __main__.__dict__.update({"__name__"    : "__main__",
                                      "__file__"    : self.filename,
                                      "__builtins__": builtins,
                                     })

        # When bdb sets tracing, a number of call and line events happens
        # BEFORE debugger even reaches user's code (and the exact sequence of
        # events depends on python version). So we take special measures to
        # avoid stopping before we reach the main script (see user_line and
        # user_call for details).
        self._wait_for_mainpyfile = 1
        self.mainpyfile = self.canonic(self.filename)
        self._user_requested_quit = 0
        statement = 'exec(open("{}").read())'.format(self.filename)
        self.run_block = True
        self.quit_called = False

        self.gui.console_text.clear()
        self.gui.log_text.clear()

        self.gui.console_text.start()
        self.gui.set_button_state('stopped')

        try:
            self.run(statement)
        except Exception:
            sys.stderr.write('\nUnexpected exception encountered...\n')
            traceback.print_exc(file=sys.stderr)

        #When BdbQuit is raised from the "interaction" method, it will propagate up into
        #the "bdb.Bdb" class where it will be caught in the Bdb.run method and will then
        #return from that method and end up here. (brn)


    # def user_call(self, frame, argument_list):
    #     """This method is called when there is the remote possibility
    #     that we ever need to stop in this function."""
    #
    #     if self._debug:
    #         import sys
    #         sys.__stderr__.write("user_call(%s:%s)\n" % (frame.f_code.co_filename,frame.f_lineno))
    #
    def user_line(self, frame):
        """This function is called when we stop or break at this line."""

        self._user_current(frame,None)

        if self._debug:
            import sys
            sys.__stderr__.write("user_line(%s:%s)\n" % (frame.f_code.co_filename,frame.f_lineno))

        super(ScriptExecutor, self).user_line(frame)
        self.interaction(frame)
    #
    def user_return(self, frame, return_value):
        """This function is called when a return trap is set here."""

        import six
        if six.PY2:
            import Tkinter as tk
        else:
            import tkinter as tk

        if self._debug:
            import sys
            sys.__stderr__.write("user_return(%s:%s)\n" % (frame.f_code.co_filename,frame.f_lineno))

        # frame.f_locals['__return__'] = return_value

        super(ScriptExecutor, self).user_return(frame, return_value)

        if frame.f_back == self.botframe:

            print('===================Script Ended===================')

            #Set initial button states
            self.gui.set_button_state('stopped')
            self.gui.stop_button.config(state=tk.DISABLED)

            self._script_thread_quit()


    def user_exception(self, frame, exc_info):
        """This function is called if an exception occurs,
        but only if we are to stop at or just below this level."""
        super(ScriptExecutor, self).user_exception(frame, exc_info)
        import sys, traceback, six
        if six.PY2:
            import Tkinter as tk
            import tkMessageBox
        else:
            import tkinter as tk
            from tkinter import messagebox as tkMessageBox

        exc_type, exc_value, exc_traceback = exc_info
        # frame.f_locals['__exception__'] = exc_type, exc_value
        if type(exc_type) == type(''):
            exc_type_name = exc_type
        else: exc_type_name = exc_type.__name__

        if self._debug:
            sys.__stderr__.write("user_exception(%s,%s): %s\n" % (frame.f_code.co_filename,frame.f_lineno,exc_info))

        if frame.f_back == self.botframe:

            print('===================Script Ended===================\nUnexpected exception encountered...', file=sys.stderr)
            traceback.print_exception(etype=exc_type,value=exc_value,tb=exc_traceback,file=sys.__stderr__)
            tkMessageBox.showerror(title='Exception Encountered',
                                   message='The current script encountered an uncaught exception:\n\n"%s: %s".\n\n' % (exc_type_name,exc_value,) +\
                                   'Your script will now terminate. See the console for details.')

            #Set initial button states
            self.gui.set_button_state('stopped')
            self.gui.stop_button.config(state=tk.DISABLED)

            self._script_thread_quit()
    #
    def _user_current(self,frame,traceback):

        #Store the current line and current executing file
        self.curframe = frame
        self.curtraceback = traceback
        self.curfile = self.curframe.f_code.co_filename
        self.curline = self.curframe.f_lineno

        #TODO: When we hit the play button, keep track of the current file and only update line numbers
        #for current file or things above it in the stack frame?
        self.gui.current_file_label.var.set('%-128s' % self.curfile)#os.path.basename(filename))
        self.gui.current_line_label.var.set('%-5d' % self.curline)

    def _check_running(self):

        import threading, time

        if not self.running:
            runner = threading.Thread(target=self._run,name='user_event_thread',args=())
            runner.setDaemon(True)
            runner.start()
            self.running = True

            time.sleep(1)

    # def do_clear(self, arg):
    #
    #     self.clear_bpbynumber(arg)

    def _handle_mtak_started(self):

        self.gui.root.title('MTAK GUI: %s (%d)' % (self.session_config.name,self.session_config.key))
        self.gui.log_text.filename = self.session_config.mtak_module.wrapper.get_log_file()
        self.gui.log_text.start()
        self.mtak_running = True

    def _handle_mtak_stopped(self):

        self.gui.root.title('MTAK GUI: No Active Session')
        self.gui.log_text.filename = None
        self.gui.log_text.stop()
        self.mtak_running = False

    def interaction(self, frame=None, traceback=None):
        # super(ScriptExecutor, self).interaction(frame, traceback)
        import sys
        frame=frame if frame else self.curframe
        try:

            if self._debug:
                sys.__stderr__.write('INTERACTION: Currently at %s:%d\n' % (self.curfile,self.curline))
                sys.__stderr__.write('wait_for_mainpyfile = %s\n' % (self._wait_for_mainpyfile))

            if frame is not None:
                import mtak
                if mtak.__name__ in self.curframe.f_globals or mtak.__name__ in self.curframe.f_locals:
                    mod = self.curframe.f_globals.get(mtak.__name__,None)
                    if mod is None:
                        mod = self.curframe.f_locals.get(mtak.__name__,None)
                    new_session_config = mod.wrapper.get_session_config()
                    if self.session_config is None and new_session_config is not None:
                        log_file = mod.wrapper.get_log_file()
                        if log_file is not None:
                            self.session_config = new_session_config
                            self.session_config.mtak_module = mod
                            self._handle_mtak_started()
                    elif self.session_config is not None and new_session_config is None:
                        self.session_config = None
                        self._handle_mtak_stopped()

            if self.run_block:

                if self._debug:
                    sys.__stderr__.write('self.run_block is True\n')
                self._update_script_view()

                self.gui.set_button_state(self.previous_button_state)
                self.previous_button_state = None

                #Wait for user to click a button
                if self._debug:
                    sys.__stderr__.write('Waiting for user input...\n')
                self.action_sync.acquire()
                self.action_sync.wait()
                self.action_sync.release()
            else:
                if self._debug:
                    sys.__stderr__.write('self.run_block is False\n')

            if self.quit_called:
                self._script_thread_quit()
            elif self._debug:
                sys.__stderr__.write('self.quit_called is False!\n')


            #Store the previous line and file...it's not always going to be current_line - 1
            # or the current file because running code jumps around
            self.prevfile = self.curfile
            self.prevline = self.curline

        except RuntimeError:
            #TODO: Is this the best solution here?
            import time
            time.sleep(500)
            sys.__stderr__.write('Interaction runtime error: %s' % (sys.exc_info(),))

    def _script_thread_quit(self):

        import bdb, sys

        if self._debug:
            sys.__stderr__.write('self.quit_called is True!\n')

        if self.mtak_running:
            if self.curframe is not None:
                import mtak
                if mtak.__name__ in self.curframe.f_globals or mtak.__name__ in self.curframe.f_locals:
                    mod = self.curframe.f_globals.get(mtak.__name__,None)
                    if mod is None:
                        mod = self.curframe.f_locals.get(mtak.__name__,None)
                    import sys
                    mod.wrapper.shutdown()

        self.gui.console_text.stop()
        self._handle_mtak_stopped()

        self.running = False
        self._user_requested_quit = 1
        self.set_quit()

        self.reset()

        self.gui.current_file_label.var.set('%-128s' % (self.filename,))#os.path.basename(filename))
        self.gui.current_line_label.var.set('%-5d' % (0,))

        self.gui.set_button_state('load_only')

        raise bdb.BdbQuit

    def _remove_current_highlight(self):

        if self.curline is not None:
            self.gui.script_line_numbers.tag_remove('HIGHLIGHT','%d.0' % (self.curline),'%d.end' % (self.curline))
            self.gui.script_line_numbers.tag_add('LINE_NUMBER','%d.0' % (self.curline),'%d.end' % (self.curline))

    def _remove_previous_highlight(self):

        #Remove previous line highlight (if there is one)
        if self.prevline is not None:
            self.gui.script_line_numbers.tag_remove('HIGHLIGHT','%d.0' % (self.prevline),'%d.end' % (self.prevline))
            self.gui.script_line_numbers.tag_add('LINE_NUMBER','%d.0' % (self.prevline),'%d.end' % (self.prevline))

    def _update_script_view(self):

        import sys
        if self._debug:
            sys.__stderr__.write('Update script view...\n')
            sys.__stderr__.write('Current = %s:%s\n' % (self.curfile,self.curline))
        #Remove previous line highlight (if there is one)
        self._remove_previous_highlight()

        #Load new file if different than current one (e.g. user did
        #'Step Into' on a method defined in a different file)
        if self.curfile != self.prevfile and self.prevfile is not None:
            self.gui._load_file(self.curfile)

        #Highlight the current line number
        self.gui.script_line_numbers.tag_remove('LINE_NUMBER','%d.0' % (self.curline),'%d.end' % (self.curline))
        self.gui.script_line_numbers.tag_add('HIGHLIGHT','%d.0' % (self.curline),'%d.end' % (self.curline))

        #Auto-scroll the view to make sure the current line number is in view
        self.gui.script_text.mark_set("insert", "%d.0" % (self.curline))
        self.gui.script_line_numbers.mark_set("insert", "%d.0" % (self.curline))
        if self._debug:
            sys.__stderr__.write('Set "insert" mark at line %s\n' % (self.curline,))
        self.gui.script_text.see("insert")
        self.gui.script_line_numbers.see("insert")

    def _is_executable(self,line,lineno):
        '''Check if a line of code is valid for the handle_run_until function. As far as we're concerned,
        the basic strategy is to take the current line of code and then remove all the strings and comments
        in that line and see if there is any text left on that line.  Sounds easy, but it has a lot of
        annoying little fringe cases.  This uses the lexer and the TK text field to figure this information out,
        so it really only makes sense to use this function in the context of this class.

        line - The line we're checking to see is executable.
        lineno - The actual line number of "line" in the file.'''

        #Line is whitespace...shortcut to quickly dismiss whitespace-only lines
        if not line.strip():
            return False

        #Generate pairs of (start,end) indexes for strings and comments in the form of
        #(lower_bound line:col,upper_bound line:col) and put them into a giant list.  We actually
        #don't really care if the list is sorted.  We could speed up the algorithm a bit if it was, but this
        #code is run so rarely thatit's not a big deal and it's doing very little visually, so we can rely
        #on the user's slow perception and take our time here. (brn)
        ranges = []
        string_ranges = self.gui.script_text.tag_ranges('STRING')
        comment_ranges = self.gui.script_text.tag_ranges('COMMENT')
        ranges.extend([(string_ranges[i],string_ranges[i+1]) for i in range(0,len(string_ranges),2)])
        ranges.extend([(comment_ranges[i],comment_ranges[i+1]) for i in range(0,len(comment_ranges),2)])

        #inline_ranges are the ranges of values for a string or comment that are completely contained within
        #a single line of the file.  They'll be of the form (start_column,end_column) and are essentially
        #indexes into the string "line"
        inline_ranges = []
        for start,end in ranges:

            #Entire line is part of a bigger multi-line string that encompasses this line
            if float(start) < float(lineno) and float(end) > float(lineno):
                return False

            #Technically "prefix" is really "row" in the file and "suffix" is really "column" in the file
            #...We're essentially grabbing the start and end points for each range
            start_prefix,start_suffix = start.split('.')
            start_prefix = int(start_prefix)
            start_suffix = int(start_suffix)
            end_prefix,end_suffix = end.split('.')
            end_prefix = int(end_prefix)
            end_suffix = int(end_suffix)

            #If the current line contains the start of a string/comment
            if start_prefix == lineno:

                #String/Comment is embedded on a single line.  Add it to our inline_ranges.
                if end_prefix == lineno:
                    if start_prefix == 0 and end_prefix == len(line):
                        return False
                    inline_ranges.append(start_suffix)
                    inline_ranges.append(end_suffix)

                #Multi-line string that starts on this line. Strip out the string and check
                #to see if the rest of the line is executable
                elif end_prefix > lineno:
                    new_str = line[0:start_suffix-1]
                    new_str = new_str.strip()
                    if not new_str:
                        return False

        #Now we're left with only strings/comments that were embedded in "line".  We have the
        #start/end index for each of these in "inline_ranges" so what we're going to do is reconstruct
        #"line" with all these ranges removed.  Then if the string still has some stuff in it, we'll
        #say it looks executable.
        if inline_ranges:
            inline_ranges.insert(0,0)
            inline_ranges.append(len(line))
            new_str = ''
            for i in range(0,len(inline_ranges),2):
                lower = inline_ranges[i]
                upper = inline_ranges[i+1]
                new_str += line[lower:upper]
            new_str = new_str.strip()
            if not new_str:
                return False

        return True

    def handle_pause(self,*args):

        #This is seriously the only way I could find to see if the button
        #is disabled.  I hate you Tkinter.  This check has to be here so that
        #the keyboard shortcuts don't still work when the button is disabled.
        if self.gui.pause_button.config('state')[4] == 'disabled':
            return

        import sys

        if self._debug:
                sys.__stderr__.write('Called handle_pause\n')

        #self.previous_button_state = self.gui.button_state
        #self.gui.set_button_state('disable_all')

        #self._check_running()

        #This is a little bit of an abuse of how this paradigm normally works
        #for the rest of the buttons
        #self.gui.set_button_state('stopped')
        #self.previous_button_state = self.gui.button_state

        self.run_block = True

    def handle_stop(self,*args):

        #This is seriously the only way I could find to see if the button
        #is disabled.  I hate you Tkinter.  This check has to be here so that
        #the keyboard shortcuts don't still work when the button is disabled.
        if self.gui.stop_button.config('state')[4] == 'disabled':
            return

        import sys, six
        if six.PY2:
            import Tkinter as tk
            import tkMessageBox
        else:
            import tkinter as tk
            from tkinter import messagebox as tkMessageBox

        if self._debug:
                sys.__stderr__.write('Called handle_stop\n')

        if not tkMessageBox.askyesno(title="Stop Script",
                                     message="This will terminate your current script run.  Are you sure this is what you want to do?",
                                     default=tkMessageBox.NO):
            return

        #Set initial button states
        self.gui.set_button_state('stopped')
        self.gui.stop_button.config(state=tk.DISABLED)

        self.handle_quit()

    def handle_play(self,*args):

        #This is seriously the only way I could find to see if the button
        #is disabled.  I hate you Tkinter.  This check has to be here so that
        #the keyboard shortcuts don't still work when the button is disabled.
        if self.gui.play_button.config('state')[4] == 'disabled':
            return

        import sys

        if self._debug:
                sys.__stderr__.write('Called handle_play\n')

        self.previous_button_state = self.gui.button_state
        self.gui.set_button_state('disable_all')

        #Remove the current highlight
        if self.curline is not None:
            self.gui.script_line_numbers.tag_remove('HIGHLIGHT','%d.0' % (self.curline),'%d.end' % (self.curline))
            self.gui.script_line_numbers.tag_add('LINE_NUMBER','%d.0' % (self.curline),'%d.end' % (self.curline))

        #self._check_running()

        self.gui.set_button_state('running')
        self.set_next(self.curframe)
        # self.do_continue(self.curframe)
        self.run_block = False

        self.action_sync.acquire()
        self.action_sync.notify()
        self.action_sync.release()

    def handle_step_next(self,*args):

        #This is seriously the only way I could find to see if the button
        #is disabled.  I hate you Tkinter.  This check has to be here so that
        #the keyboard shortcuts don't still work when the button is disabled.
        if self.gui.step_next_button.config('state')[4] == 'disabled':
            return

        import sys

        if self._debug:
                sys.__stderr__.write('Called handle_step_next\n')

        self.previous_button_state = self.gui.button_state
        self.gui.set_button_state('disable_all')

        #self._check_running()

        self.set_next(self.curframe)

        self.action_sync.acquire()
        self.action_sync.notify()
        self.action_sync.release()

    def handle_step_into(self,*args):

        #This is seriously the only way I could find to see if the button
        #is disabled.  I hate you Tkinter.  This check has to be here so that
        #the keyboard shortcuts don't still work when the button is disabled.
        if self.gui.step_into_button.config('state')[4] == 'disabled':
            return

        import sys

        if self._debug:
                sys.__stderr__.write('Called handle_step_into\n')

        self.previous_button_state = self.gui.button_state
        self.gui.set_button_state('disable_all')

        #self._check_running()

        self.set_step()

        self.action_sync.acquire()
        self.action_sync.notify()
        self.action_sync.release()

    def handle_step_return(self,*args):

        #This is seriously the only way I could find to see if the button
        #is disabled.  I hate you Tkinter.  This check has to be here so that
        #the keyboard shortcuts don't still work when the button is disabled.
        if self.gui.step_return_button.config('state')[4] == 'disabled':
            return

        import sys

        if self._debug:
                sys.__stderr__.write('Called handle_step_return\n')

        self.previous_button_state = self.gui.button_state
        self.gui.set_button_state('disable_all')

        #self._check_running()

        self.set_return(self.curframe)

        self.action_sync.acquire()
        self.action_sync.notify()
        self.action_sync.release()

    def handle_run_until(self,*args):

        if self.gui.run_until_button.config('state')[4] == 'disabled':
            return
        import sys, six
        if six.PY2:
            import tkSimpleDialog
            import tkMessageBox
        else:
            from tkinter import simpledialog as tkSimpleDialog
            from tkinter import messagebox as tkMessageBox

        if self._debug:
                sys.__stderr__.write('Called handle_run_until\n')

        #self._check_running()

        valid = False
        while not valid:

            valid = True

            desired_line = tkSimpleDialog.askinteger(title='Run Until Line',
                                                     prompt='Run until line number: ',
                                                     minvalue=0,
                                                     maxvalue=self.gui.total_lines)

            if desired_line is None:
                return

            try:
                val = int(desired_line)
                if val < 0:
                    valid = False
            except ValueError:
                valid = False

            if valid:
                text = self.gui.script_text.get('%d.0' % desired_line,'%d.end' % desired_line)
                valid = self._is_executable(text,desired_line)

            if not valid:
                tkMessageBox.showerror(title='Invalid line number',
                                       message='The line number you entered does not appear to be'+\
                                       ' an executable line of code.  Please choose a different line'+\
                                       ' number.')

        self.previous_button_state = self.gui.button_state
        self.gui.set_button_state('disable_all')

        #This works well, but we have no way to animate the line number highlight in the GUI when this is happening
        #(Though it seems trying to animate causes us some segmentation faults)
        self.set_break(self.curfile,desired_line,temporary=True)
        self.set_continue()

        self.action_sync.acquire()
        self.action_sync.notify()
        self.action_sync.release()

    def handle_jump(self,*args):

        #This is seriously the only way I could find to see if the button
        #is disabled.  I hate you Tkinter.  This check has to be here so that
        #the keyboard shortcuts don't still work when the button is disabled.
        if self.gui.jump_button.config('state')[4] == 'disabled':
            return
        import sys, six
        if six.PY2:
            import tkSimpleDialog
            import tkMessageBox
        else:
            from tkinter import simpledialog as tkSimpleDialog
            from tkinter import messagebox as tkMessageBox

        if self._debug:
                sys.__stderr__.write('Called handle_jump\n')

        #self._check_running()

        valid = False
        while not valid:

            valid = True

            desired_line = tkSimpleDialog.askinteger(title='Jump To Line',
                                                 prompt='Enter the line number that should be executed next (%d-%d): ' % (1,self.gui.total_lines),
                                                 minvalue=1,
                                                 maxvalue=self.gui.total_lines)
            if desired_line is None:
                return

            try:
                val = int(desired_line)
                if val < 0:
                    valid = False
            except ValueError:
                valid = False

            if valid:
                text = self.gui.script_text.get('%d.0' % desired_line,'%d.end' % desired_line)
                valid = self._is_executable(text,desired_line)

            if not valid:
                tkMessageBox.showerror(title='Invalid line number',
                                       message='The line number you entered does not appear to be'+\
                                       ' an executable line of code.  Please choose a different line'+\
                                       ' number.')


        stack, curindex = self.get_stack(self.curframe,self.curtraceback)
        if curindex + 1 != len(stack):
            tkMessageBox.showerror(title='Jump Error',
                                       message="Can't jump in this context. Try again from a different place in the script.")
            return

        try:
            self.curframe.f_lineno = desired_line
            stack[curindex] = stack[curindex][0],desired_line
            self.set_next(self.curframe)
        except ValueError as e:
            tkMessageBox.showerror(title='Jump Error', message="Jump Failed: %s" % (e))
            return

        self.previous_button_state = self.gui.button_state
        self.gui.set_button_state('disable_all')

        self.action_sync.acquire()
        self.action_sync.notify()
        self.action_sync.release()

    def handle_exec_cmd(self,*args):

        #This is seriously the only way I could find to see if the button
        #is disabled.  I hate you Tkinter.  This check has to be here so that
        #the keyboard shortcuts don't still work when the button is disabled.
        if self.gui.cmd_button.config('state')[4] == 'disabled':
            return

        import sys, six
        if six.PY2:
            import tkinter as tk
        else:
            import tkinter as tk

        if self._debug:
                sys.__stderr__.write('Called handle_exec_cmd\n')

        self.previous_button_state = self.gui.button_state
        self.gui.set_button_state('disable_all')

        #self._check_running()

        cmd = self.gui.cmd_entry.get()
        if not cmd.endswith('\n'):
            cmd += '\n'

        if self._debug:
            sys.__stderr__.write("Command = '%s'\n" % (cmd))

        #This chunk of code was taken almost entirely from pdb.Pdb.default(self,line)
        locals = self.curframe.f_locals
        globals = self.curframe.f_globals

        cmd_worked = False
        if cmd.strip() and self.curframe is not None:
            try:
                code = compile(cmd, 'Input Command', 'single')
                exec(code, globals, locals)
                cmd_worked = True
            except:
                t, v = sys.exc_info()[:2]
                if type(t) == type(''):
                    exc_type_name = t
                else: exc_type_name = t.__name__
                print('***Command failed: "{}" ---> {}: {}'.format(cmd.strip(), exc_type_name, v), file=sys.stderr, flush=True)

        self.gui.set_button_state(self.previous_button_state)
        self.previous_button_state = None

        if cmd_worked:
            self.gui.cmd_entry.delete(0, tk.END)
            print('***Command successful: "{}"'.format('{}'.format(cmd).strip()))

    def handle_quit(self,*args):

        self._remove_previous_highlight()
        self._remove_current_highlight()

        self.gui.current_line_label.var.set('%-5d' % (0,))

        self.quit_called = True

        self.action_sync.acquire()
        self.action_sync.notify()
        self.action_sync.release()


class MtakGui(tk.Frame):
    '''A graphical debugging window for MTAK scripts based on TKinter. It inherits
    from the Tkinter Frame object.'''

    def __init__(self,master,filename=None):
        '''Initialize the GUI by setting up all the various pieces and laying them out properly.  There's a row
        of buttons across the top, a large section in the middle for a script view with line numbers and some tabs
        at the bottom for display various types of output.'''

        tk.Frame.__init__(self,master,bg='black')

        self.root = master
        self.root.protocol("WM_DELETE_WINDOW",self._exit)

        self.button_state = 'stopped'
        self.filename = filename
        self.executor = ScriptExecutor(self.filename,self)
        self._last_load_dir = None

        #A paned window is a window that holds multiple panes and the borders between
        #each of the panes are draggable so the panes can be resized
        self.paned_window = tk.PanedWindow(self,orient=tk.VERTICAL)

        self._setup_script_frame()
        self._setup_output_frame()
        self._setup_menus()

        #The "place" layout manager lets you place things by percentages in the overall
        #GUI...kinda like the SWT FormLayout.  This is probably unnecessary here since it's
        #set to 100% everywhere, but it wasn't originally and it works now so I'm not changing it.
        self.paned_window.place(relx=0,rely=0,relwidth=1.0,relheight=1.0)
        self.place(relx=0,rely=0,relwidth=1.0,relheight=1.0)

    def set_button_state(self,val):
        '''Sets whether all the buttons on the GUI should be enabled
        or disabled based on whether the debugger is actively running or
        whether it's paused and in "step" mode.'''
        import six
        if six.PY2:
            import Tkinter as tk
        else:
            import tkinter as tk

        if val == 'stopped':
            self.load_button.config(state=tk.NORMAL)
            self.reload_button.config(state=tk.NORMAL)
            self.play_button.config(state=tk.NORMAL)
            self.pause_button.config(state=tk.DISABLED)
            self.stop_button.config(state=tk.NORMAL)
            self.step_next_button.config(state=tk.NORMAL)
            self.step_into_button.config(state=tk.NORMAL)
            self.step_return_button.config(state=tk.NORMAL)
            self.run_until_button.config(state=tk.NORMAL)
            self.jump_button.config(state=tk.NORMAL)
            self.cmd_entry.config(state=tk.NORMAL)
            self.cmd_button.config(state=tk.NORMAL)
        elif val == 'running':
            self.load_button.config(state=tk.DISABLED)
            self.reload_button.config(state=tk.DISABLED)
            self.play_button.config(state=tk.DISABLED)
            self.pause_button.config(state=tk.NORMAL)
            self.stop_button.config(state=tk.NORMAL)
            self.step_next_button.config(state=tk.DISABLED)
            self.step_into_button.config(state=tk.DISABLED)
            self.step_return_button.config(state=tk.DISABLED)
            self.run_until_button.config(state=tk.DISABLED)
            self.jump_button.config(state=tk.DISABLED)
            self.cmd_entry.config(state=tk.DISABLED)
            self.cmd_button.config(state=tk.DISABLED)
        elif val == 'disable_all':
            self.load_button.config(state=tk.DISABLED)
            self.reload_button.config(state=tk.DISABLED)
            self.play_button.config(state=tk.DISABLED)
            self.pause_button.config(state=tk.DISABLED)
            self.stop_button.config(state=tk.DISABLED)
            self.step_next_button.config(state=tk.DISABLED)
            self.step_into_button.config(state=tk.DISABLED)
            self.step_return_button.config(state=tk.DISABLED)
            self.run_until_button.config(state=tk.DISABLED)
            self.jump_button.config(state=tk.DISABLED)
            self.cmd_entry.config(state=tk.DISABLED)
            self.cmd_button.config(state=tk.DISABLED)
        elif val == 'load_only':
            self.load_button.config(state=tk.NORMAL)
            self.reload_button.config(state=tk.NORMAL)
            self.play_button.config(state=tk.DISABLED)
            self.pause_button.config(state=tk.DISABLED)
            self.stop_button.config(state=tk.DISABLED)
            self.step_next_button.config(state=tk.DISABLED)
            self.step_into_button.config(state=tk.DISABLED)
            self.step_return_button.config(state=tk.DISABLED)
            self.run_until_button.config(state=tk.DISABLED)
            self.jump_button.config(state=tk.DISABLED)
            self.cmd_entry.config(state=tk.DISABLED)
            self.cmd_button.config(state=tk.DISABLED)
        else:
            #Ignore invalid button state entries
            return

        self.button_state = val

    def _setup_menus(self):
        '''Setup the menus across the top of the screen.'''

        #Currently this has nothing more than a file menu with an exit button

        self._menu_bar = tk.Menu(master=self.root)

        self._file_menu = tk.Menu(self._menu_bar,tearoff=0)
        self._file_menu.add_command(label='Exit (Ctrl-q)',command=self._exit)
        self.root.bind('<Control-Key-q>',self._exit)
        self._menu_bar.add_cascade(label='File',menu=self._file_menu)

        self.root.config(menu=self._menu_bar)

    def _create_button(self,parent,image_file,short_text,long_text,handler,control_letter):
        '''Utility method for creating a button with an image.  This is used for
        creating all the buttons across the top of the display.'''

        #Load the image for the button
        image = tk.PhotoImage(master=parent,file=image_file)

        #Create the actually Tkinter Button object
        button = tk.Button(master=parent,image=image,pady=5,command=handler,
                           bg='black',bd=1,width=24,height=24,state=tk.DISABLED)
        button._ntimage = image
        button.pack(side=tk.LEFT,anchor=tk.CENTER,fill=tk.NONE,expand=False,pady=10)

        if handler is not None:
            self.root.bind("<Control-Key-%s>" % (control_letter),handler)

        #Balloons  are mouseover popup help text for the button
        balloon = Pmw.Balloon(button)
        short_text = short_text + ' (Ctrl-%s)' % (control_letter)
        balloon.bind(button,short_text,long_text)
        button.balloon = balloon

        return button

    def _add_vertical_separator(self,parent):
        '''Utility method for inserting vertical separators between buttons.'''

        vertical_separator = tk.Frame(master=parent,width=5,bd=2,
                                      relief=tk.SUNKEN,background='black')
        vertical_separator.pack(side=tk.LEFT,anchor=tk.CENTER,fill=tk.Y,expand=False,padx=5,pady=2)
        return vertical_separator

    def _setup_buttons(self,parent=None):
        '''Sets up the row of buttons across the top of the GUI.  All the buttons are just placed side by side
        with an occasional vertical separator between them.  At the far right is a label/entry/button combination
        where the user can enter and execute arbitrary Python commands.'''

        if not parent:
            parent = self

        self.button_frame = tk.Frame(parent,borderwidth=1,relief=tk.RAISED)

        ###########Script Load Section###########

        self.load_button = self._create_button(self.button_frame,mpcsutil.chillImageDirectory + '/load.gif',
                                               'Load Script...','Load a new script to run.',self.handle_load,'o')
        self.load_button.config(state=tk.NORMAL)

        self.reload_button = self._create_button(self.button_frame,mpcsutil.chillImageDirectory + '/reload.gif',
                                               'Reload Script','Stop and restart the current script.',self.handle_reload,'d')

        self._add_vertical_separator(self.button_frame)

        ###########Control Section###########

        self.play_button = self._create_button(self.button_frame,mpcsutil.chillImageDirectory + '/play.gif',
                                               'Play/Resume','Start the script running from the current point.',
                                               self.handle_play,'p')

        self.pause_button = self._create_button(self.button_frame,mpcsutil.chillImageDirectory + '/pause.gif',
                                                'Suspend','Pause execution of the script.',
                                               self.handle_pause,'a')

        self.stop_button = self._create_button(self.button_frame,mpcsutil.chillImageDirectory + '/stop.gif',
                                               'Terminate','Terminate the current script.',
                                               self.handle_stop,'s')

        self._add_vertical_separator(self.button_frame)

        ###########Step Section###########

        self.step_next_button = self._create_button(self.button_frame,mpcsutil.chillImageDirectory + '/step-next.gif',
                                                    'Step Next','Step to the next line of code in the current function.',
                                                    self.handle_step_next,'n')

        self.step_into_button = self._create_button(self.button_frame,mpcsutil.chillImageDirectory + '/step-into.gif',
                                                    'Step Into','Step to the next line of code to be executed.',
                                                    self.handle_step_into,'i')

        self.step_return_button = self._create_button(self.button_frame,mpcsutil.chillImageDirectory + '/step-return.gif',
                                                      'Step Return','Step to the end of the current function.',
                                                      self.handle_step_return,'r')

        self._add_vertical_separator(self.button_frame)

        self.run_until_button = self._create_button(self.button_frame,mpcsutil.chillImageDirectory + '/run-until.gif',
                                                    'Run Until...','Run until a particular line of code is reached.',
                                                    self.handle_run_until,'u')

        self.jump_button = self._create_button(self.button_frame,mpcsutil.chillImageDirectory + '/jump.gif',
                                               'Jump...','Set the next line of code to be executed.',
                                               self.handle_jump,'j')

        self._add_vertical_separator(self.button_frame)

        ###########Command Entry Section###########

        #TODO: Should the command stuff go in its own little frame? Probably...
        cmd_label = tk.Label(self.button_frame,text='Command: ')
        cmd_label.pack(side=tk.LEFT,anchor=tk.CENTER,fill=tk.NONE,expand=False)

        self.cmd_entry = tk.Entry(self.button_frame,state=tk.DISABLED)
        self.cmd_entry.pack(side=tk.LEFT,anchor=tk.CENTER,fill=tk.X,expand=True)
        self.cmd_entry.bind("<Return>",self.handle_exec_cmd)
        self.cmd_button = tk.Button(self.button_frame,text='Execute',state=tk.DISABLED,
                                    command=self.handle_exec_cmd)
        self.cmd_button.pack(side=tk.LEFT,anchor=tk.CENTER,fill=tk.NONE,expand=False,padx=10)

    def _scroll_script_frame_y(self,*args):
        '''A vertical scrollbar can only be assigned one Text.yview method to call, but when our scrollbar is moved, we
        actually need it to scroll the script window and the line number window.  The way we do that is to make the scrollbar
        call this method which then scrolls each of the views that need to move.'''

        self.script_text.yview(*args)
        self.script_line_numbers.yview(*args)

    def _scroll_text_areas(self,*args):
        '''Scrollbars are weird.  By default, scrolls will do just fine scrolling both of our text areas, but in the case
        of the user using the mousewheel, only the text area with focus scrolls and the one other one stays in place.  To
        get around that we override this method and make it just set the values on both text areas every time in addition
        to setting the location of the scrollbar itself.'''

        #Set the scrollbar position
        self.script_text_vscrollbar.set(*args)

        #Move both text areas accordingly
        text_area_args = ('moveto',args[0]) #Yes, "moveto" is actually a TK keyword with no associated constant
        self.script_text.yview(*text_area_args)
        self.script_line_numbers.yview(*text_area_args)

    def _setup_script_frame(self):
        '''Setup the frame that contains the actual script as well as the line numbers for the script.  This is actually done by having
        two separate Tkinter Text objects side by side.  The left hand Text only resizes vertically and contains all the line numbers
        for each line in the script.  The right hand Text resizes horizontally and vertically and contains the actual script contents.

        UPDATE:  The script_frame now contains the frame that holds all of the buttons too.'''

        #The script frames holds all the pieces related to displaying the script...the container isn't really necessary anymore, but it
        #lets us easily put padding inside the script_frame if we need to
        self.script_frame = tk.Frame(self.paned_window,borderwidth=1,relief=tk.GROOVE)
        self.script_area_container = tk.Frame(self.script_frame)

        self.script_info_frame = tk.Frame(self.script_area_container,borderwidth=1,relief=tk.RAISED,
                                          bg='white')
        self.current_file_label_prefix = tk.Label(self.script_info_frame,bg='white',font=("Helvetica","12","bold"),text='File:')
        current_file_label = tk.StringVar()
        self.current_file_label = tk.Label(self.script_info_frame,bg='white',font=("Helvetica","12"),textvariable=current_file_label)
        self.current_file_label.var = current_file_label
        self.current_file_label.var.set('%-128s' % ('',))

        self.current_line_label_prefix = tk.Label(self.script_info_frame,bg='white',font=("Helvetica","12","bold"),text='Line:')
        current_line_label = tk.StringVar()
        self.current_line_label = tk.Label(self.script_info_frame,bg='white',font=("Helvetica","12"),textvariable=current_line_label)
        self.current_line_label.var = current_line_label
        self.current_line_label.var.set('%-5d' % (0,))

        self.current_file_label_prefix.pack(side=tk.LEFT,padx=2)
        self.current_file_label.pack(side=tk.LEFT)
        self.current_line_label.pack(side=tk.RIGHT)
        self.current_line_label_prefix.pack(side=tk.RIGHT,padx=2)

        #Scrollbars.  Good times.
        self.script_text_hscrollbar = tk.Scrollbar(self.script_area_container, name='script_area_hscrollbar', orient=tk.HORIZONTAL)
        self.script_text_vscrollbar = tk.Scrollbar(self.script_area_container, name='script_area_vscrollbar')

        #The Text field that will contain the actual script contents.  The "highlight" settings border the Text with a thin
        #line of dark gray.  The text doesn't wrap and the x/y scrollcommand settings attached the scrollbars to this field.
        self.script_text = tk.Text(self.script_area_container, wrap=tk.NONE, state=tk.NORMAL,
                                   yscrollcommand=self._scroll_text_areas,
                                   xscrollcommand=self.script_text_hscrollbar.set,
                                   background='white',padx=4,pady=0,
                                   highlightbackground='darkgray',highlightcolor='darkgray',
                                   font=("Helvetica", "12"))
        self.script_text.bind('<Key>',lambda e: 'break') #ignore all key presses

        #Add syntax highlighting to the Text field.  I lifted this straight from IDLE.  See the colorizer module
        #for more details.
        self.percolator = colorizer.Percolator(self.script_text)
        self.colorizer = colorizer.ColorDelegator()
        self.percolator.insertfilter(self.colorizer)

        #The Text field that will contain the line numbers for the script.  This field is all dark gray and only scrolls
        #vertically, not horizontally.  It's about 6 characters wide (so as long as your script is less than 1,000,000 lines,
        #we're OK)
        self.script_line_numbers = tk.Text(self.script_area_container,wrap=tk.NONE,width=6, state=tk.NORMAL,
                                   yscrollcommand=self._scroll_text_areas,
                                   padx=0,pady=0,bd=0,background='darkgray',
                                   highlightbackground='darkgray',highlightcolor='darkgray',
                                   font=("Helvetica","12","bold"))
        self.script_line_numbers.bind('<Key>',lambda e: 'break') #ignore all key presses

        #Tags for assisting in highlighting the currently selected line in yellow
        self.script_line_numbers.tag_configure('LINE_NUMBER',background='darkgray',justify=tk.RIGHT,rmargin=0,wrap=tk.NONE)
        self.script_line_numbers.tag_configure('HIGHLIGHT',background='yellow',justify=tk.RIGHT,rmargin=0,wrap=tk.NONE)

        #Attach the scrollbars to the proper fields
        self.script_text_vscrollbar.config(command=self._scroll_script_frame_y) #Scroll both Text fields
        self.script_text_hscrollbar.config(command=self.script_text.xview) #Only scroll the script Text field (not line #s)

        #Create all the buttons on the GUI
        self._setup_buttons(self.script_area_container)

        #Lay everything out relative to each other in the GUI
        self.button_frame.pack(side=tk.TOP,fill=tk.BOTH,anchor=tk.N,expand=False,padx=0,pady=0)
        #self.script_info_frame.pack(side=tk.TOP,fill=tk.X,anchor=tk.N,expand=False,padx=0,pady=0)
        self.script_info_frame.pack(side=tk.BOTTOM,fill=tk.X,anchor=tk.S,expand=False,padx=0,pady=0)
        self.script_text_vscrollbar.pack(anchor=tk.E,side=tk.RIGHT,fill=tk.Y,expand=False)
        self.script_text_hscrollbar.pack(anchor=tk.S,side=tk.BOTTOM,fill=tk.X,expand=False)
        self.script_line_numbers.pack(side=tk.LEFT,fill=tk.Y,anchor=tk.W,expand=False,padx=0,pady=0)
        self.script_text.pack(side=tk.RIGHT,fill=tk.BOTH,anchor=tk.E,expand=True,padx=0,pady=0)
        self.script_area_container.pack(side=tk.TOP,anchor=tk.W,expand=True,fill=tk.BOTH,padx=0,pady=0)
        self.script_frame.place(relx=0,rely=0,relwidth=1.0,relheight=.70)
        self.paned_window.add(self.script_frame)

    def _ask_open_file(self):
        '''Prompt the user for a file to open.'''

        import os, os.path, six
        if six.PY2:
            import tkFileDialog
        else:
            from tkinter import filedialog as tkFileDialog

        if self._last_load_dir is None:
            self._last_load_dir = os.getcwd()

        result = tkFileDialog.askopenfilename(parent=self,
                                     filetypes=[('Python', '.py'),('All Files', '*')],
                                     initialdir=self._last_load_dir,
                                     title='Choose an MTAK script to load')

        if result:
            self._last_load_dir = os.path.dirname(result)

        return result

    def delayed_handle_load(self,*args):

        import time
        time.sleep(1)
        self.handle_load(*args)

    def handle_load(self,*args):
        '''Load a Python script from a file into the debugger. This is the handler for clicking the Load button.'''

        if self.load_button.config('state')[4] == 'disabled':
            return

        import sys, six
        if six.PY2:
            import Tkinter as tk
            import tkMessageBox
        else:
            import tkinter as tk
            from tkinter import messagebox as tkMessageBox

        filename = None
        if args and not isinstance(args[0],tk.Event):
                filename = args[0]

        #If we don't have a file in mind already, ask the user what file they want
        if filename is None:
            filename = self._ask_open_file()
            if not filename:
                return

            if self.executor.filename is not None:
                #Prompt for termination of current run (if there is one)
                result = tkMessageBox.askyesno(parent=self,
                                             title="Load New Script",
                                             message="Loading a new script will terminate the current script.  Are you sure this is what you want to do?",
                                             default=tkMessageBox.NO)
                #MPCS-59: 04/25/2011: For some reason, when separating the test for the return value
                #from the assignment, this bug simply disappeared. Could this be
                #a bug in the current Python interpreter?
                if not result:
                    return

        try:

            self._load_file(filename)

            #Wipe out any existing output
            self.console_text.delete("1.0", "end")
            self.log_text.delete("1.0","end")

        except:

            sys.__stderr__.write('Could not load file: %s' % (sys.exc_info(),)) #TODO:
            return

        #Set initial button states
        self.set_button_state('stopped')
        self.stop_button.config(state=tk.DISABLED)

        #Reset our executor (the thing that actually does the debugging)
        #if self.executor.running:
        self.executor.handle_quit()
        #self.executor.filename = filename

        #Rather than reuse the existing ScriptExecutor, just make
        #a whole new one...this seems much cleaner.
        import mtak.chill_mtak_gui
        self.filename = filename
        self.executor = mtak.chill_mtak_gui.ScriptExecutor(self.filename,self)

        self.current_file_label.var.set('%-128s' % filename)#os.path.basename(filename))
        self.current_line_label.var.set('%-5d' % (0,))

        self.executor._check_running()

    def _load_file(self,filename):
        '''This method does the dirty work of actually reading a file from disk and loading
        it into the display.'''

        import sys, six
        if six.PY2:
            import Tkinter as tk
        else:
            import tkinter as tk

        #Make the text fields writable
        #self.script_line_numbers.config(state=tk.NORMAL)
        #self.script_text.config(state=tk.NORMAL)

        #Empty out the current text field contents
        self.script_line_numbers.delete("1.0","end")
        self.script_text.delete("1.0","end")

        try:
            with open(filename,'r') as script:

                self.total_lines = 0
                line_count = 1

                #For each line in the script, insert it and its corresponding line number into the GUI
                for line in script:
                    self.script_line_numbers.insert('%s.0' % (line_count),'%5d\n' % line_count,'LINE_NUMBER')
                    self.script_text.insert('%s.0' % (line_count),line)
                    line_count += 1

                self.total_lines = line_count - 1

        except:
            #TODO: fix this to be better
            sys.__stderr__.write('Could not load file: %s' % (sys.exc_info(),))
            return False

        #Scroll the text field to the top
        self.script_text.mark_set("insert", "1.0")
        self.script_text.see("insert")

        #Make the text fields read only again
        #self.script_line_numbers.config(state=tk.DISABLED)
        #self.script_text.config(state=tk.DISABLED)

        return True

    def handle_reload(self,*args):

        if not self.executor:
            return

        if self.executor.running:
            self.executor.handle_stop(*args)

        self.handle_load(self.filename)

    def handle_play(self,*args):

        if not self.executor:
            return

        self.executor.handle_play(*args)

    def handle_pause(self,*args):

        if not self.executor:
            return

        self.executor.handle_pause(*args)

    def handle_stop(self,*args):

        if not self.executor:
            return

        self.executor.handle_stop(*args)

    def handle_step_next(self,*args):

        if not self.executor:
            return

        self.executor.handle_step_next(*args)

    def handle_step_into(self,*args):

        if not self.executor:
            return

        self.executor.handle_step_into(*args)

    def handle_step_return(self,*args):

        if not self.executor:
            return

        self.executor.handle_step_return(*args)

    def handle_run_until(self,*args):

        if not self.executor:
            return

        self.executor.handle_run_until(*args)

    def handle_jump(self,*args):

        if not self.executor:
            return

        self.executor.handle_jump(*args)

    def handle_exec_cmd(self,*args):

        if not self.executor:
            return

        self.executor.handle_exec_cmd(*args)

    def _setup_output_frame(self):
        '''Builds the output frame which is a set of tabs across the bottom of the display that can show output from various
        sources (e.g. stdout, stderr, MTAK Log, etc.).'''

        self.output_frame = tk.Frame(self.paned_window,borderwidth=1,relief=tk.GROOVE)

        #Stole the TabbedPageSet straight ouf of IDLE.  See the "tabbedpages" module for details.
        self.output_tab_page = tabbedpages.TabbedPageSet(self.output_frame,
                                                     page_names=['Console','MTAK Log'],
                                                     n_rows=0,expand_tabs=False)
        #Make the various output frames
        self._setup_console_output_tab(self.output_tab_page.pages['Console'].frame)
        self._setup_log_output_tab(self.output_tab_page.pages['MTAK Log'].frame)

        self.output_tab_page.pack(anchor=tk.NW,fill=tk.BOTH,expand=True)

        #To start, the output frame takes up 30% of the vertical real estate
        self.output_frame.place(relx=0,rely=.70,relwidth=1.0,relheight=.30)
        self.paned_window.add(self.output_frame)

    def _setup_console_output_tab(self,frame):
        '''Setup the tab that displays stdout and stderr to the GUI.'''

        self.console_output_tab = frame

        #Horizontal scrollbar for the display
        self.console_text_hscrollbar = tk.Scrollbar(self.console_output_tab, name='console_text_hscrollbar', orient=tk.HORIZONTAL)
        self.console_text_hscrollbar.pack(anchor=tk.S,side=tk.BOTTOM,fill=tk.X,expand=False)

        #Vertical scrollbar for the display
        self.console_text_vscrollbar = tk.Scrollbar(self.console_output_tab, name='console_text_vscrollbar')
        self.console_text_vscrollbar.pack(anchor=tk.E,side=tk.RIGHT,fill=tk.Y,expand=False)

        #A custom Text widget that displays STDOUT and STDERR
        self.console_text = console.ConsoleText(self.console_output_tab, wrap=tk.NONE,
                                   yscrollcommand=self.console_text_vscrollbar.set,
                                   xscrollcommand=self.console_text_hscrollbar.set,
                                   background='white',
                                   highlightbackground='darkgray',highlightcolor='darkgray')
        self.console_text.pack(side=tk.RIGHT,fill=tk.BOTH,anchor=tk.E,expand=True,padx=0,pady=0)

        #Attach the scrollbars to the Text
        self.console_text_vscrollbar.config(command=self.console_text.yview)
        self.console_text_hscrollbar.config(command=self.console_text.xview)

        #Empty out the Text to start
        self.console_text.clear()

    def _setup_log_output_tab(self,frame):

        self.log_output_tab = frame

        #Horizontal scrollbar for the display
        self.log_text_hscrollbar = tk.Scrollbar(self.log_output_tab, name='log_text_hscrollbar', orient=tk.HORIZONTAL)
        self.log_text_hscrollbar.pack(anchor=tk.S,side=tk.BOTTOM,fill=tk.X,expand=False)

        #Vertical scrollbar for the display
        self.log_text_vscrollbar = tk.Scrollbar(self.log_output_tab, name='log_text_vscrollbar')
        self.log_text_vscrollbar.pack(anchor=tk.E,side=tk.RIGHT,fill=tk.Y,expand=False)

        #A custom text widget that follows a file and displays to the GUI as the file is written to
        self.log_text = console.FileText(self.log_output_tab, wrap=tk.NONE,
                                   yscrollcommand=self.log_text_vscrollbar.set,
                                   xscrollcommand=self.log_text_hscrollbar.set,
                                   background='white',
                                   highlightbackground='darkgray',highlightcolor='darkgray')
        self.log_text.pack(side=tk.RIGHT,fill=tk.BOTH,anchor=tk.E,expand=True,padx=0,pady=0)

        #Attach the scrollbars to the Text
        self.log_text_vscrollbar.config(command=self.log_text.yview)
        self.log_text_hscrollbar.config(command=self.log_text.xview)

        #Empty out the Text to start
        self.log_text.clear()

    def _exit(self,*args):
        '''Handler for closing the GUI.  Called either by the File->Exit option or by
        clicking the close button on the window.'''

        import sys, threading

        #Shutdown the debugging run
        def _kill_exec():
            try:
                if self.executor.running:
                    self.executor.handle_quit()
                self.executor.reset()
            except:
                pass #Don't care
        def _kill_gui():
            #Destroy the GUI
            try:
                self.root.quit()
            except:
                pass #Don't care

        _threads=[threading.Thread(target=_func) for _func in [_kill_exec, _kill_gui]]
        [_tt.setDaemon(True) for _tt in _threads]
        [_tt.start() for  _tt in _threads]
        [_tt.join(3) for _tt in _threads]
        #Killing Python
        sys.exit(0)

def create_options():

    usageText = '%s [options] [python_script_name]' % (mpcsutil.get_script_name())
    parser = mpcsutil.create_option_parser(usageText=usageText)
    return parser

def test():

    parser = create_options()
    parser.parse_args()

    #Create the main window and give it a reasonable default size
    root = tk.Tk()
    root.title('MTAK GUI (No Active Session)')
    root.config(width=800,height=600)
    root.minsize(width=800,height=600)
    root.focus_force()

    filename = None
    if len(sys.argv) == 2:
        filename = sys.argv[1].strip()
    elif len(sys.argv) > 2:
        print('\nUnexpected extra arguments found on the command line: {}\n\nSee the --help text for usage instructions.'.format(sys.argv[2:]), file=sys.stderr, flush=True)
        sys.exit(1)

    if filename and not os.path.exists(filename):
        print('\nThe input file "{}" does not exist!'.format(filename), file=sys.stderr, flush=True)
        sys.exit(1)

    gui = MtakGui(root,filename)

    if filename:
        runner = threading.Thread(target=gui.delayed_handle_load,name='startup_loader_thread',args=(filename,))
        runner.setDaemon(True)
        runner.start()

    root.mainloop()

    sys.exit(0)

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
