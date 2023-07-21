#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import print_function
import six
import threading
import time

if six.PY2:
    import Tkinter as tk
    import tkFont
else:
    import tkinter as tk
    from tkinter import font as tkFont

class InfiniteBarDialog(tk.Toplevel):
    """
    Dialog bar that indefinitely shows progress.
    This will intentionally block the main Tkinter loop upon calling the start method.
    """

    def __init__(self,parent,width=400,height=50,label='Waiting for task to complete...',bar_color='blue'):

        self.top_label_text = label
        self.width = width
        self.height = height

        self.label = label
        self.bar_color = bar_color
        self.top_label = None
        self.progress_bar = None
        self.rectangle = None
        self.active = False

        self.base_coords = [0,0,0,self.height+5]

        tk.Toplevel.__init__(self,parent)
        self.transient(parent)
        self.title = "Progress"

        self.parent = parent
        self.result = None

        self.frame = tk.Frame(self,width=width,height=height,borderwidth=2)
        self.initial_focus = self.body(self.frame)

        self.set_bindings()

        self.grab_set()

        if not self.initial_focus:
            self.initial_focus = self

        self.protocol('WM_DELETE_WINDOW',self.cancel)

        self.geometry("+%d+%d" % ((parent.winfo_rootx()+parent.winfo_width())/2,
                                  (parent.winfo_rooty()+parent.winfo_height())/2))

        self.initial_focus.focus_set()

    def body(self,master):

        master.grid()

        grid_row = 0
        self.top_label = tk.Label(master, text="%s" % (self.label))
        f = tkFont.Font(font=self.top_label['font'])
        f['weight'] = 'bold'
        f['size'] = 12
        self.top_label['font'] = f.name
        self.top_label.grid(row=grid_row,sticky=tk.W)
        grid_row += 1

        self.progress_bar = tk.Canvas(master, relief="sunken", borderwidth=2, width=self.width, height=self.height/2)
        self.progress_bar.grid(row=grid_row,column=0,sticky=tk.W + tk.E,padx=5)
        grid_row += 1

        self.rectangle = self.progress_bar.create_rectangle(*self.base_coords,**{'fill':self.bar_color})
        self.frame.update()

    def set_bindings(self):

        self.bind('<Escape>',self.cancel)

    def set_top_label(self,text):

        self.top_label.config(**{'text':text})
        self.frame.update()

    def step(self):

        coords = self.progress_bar.coords(self.rectangle)
        coords[2] = (coords[2] + 1) % (self.width + 5)
        self.progress_bar.coords(self.rectangle,*coords)
        self.frame.update()

    def reset(self):

        self.progress_bar.coords(self.rectangle,*self.base_coords)
        self.frame.update()

    def complete(self):

        self.progress_bar.coords(self.rectangle,*self.base_coords)
        complete_coords = self.base_coords[:]
        complete_coords[3] = self.width + 5
        self.frame.update()

    def cancel(self,event=None):

        self.parent.focus_set()
        self.destroy()

    def validate(self):

        return 1

    def apply(self):

        pass

    def toggle_state(self,event=None):

        if not self.active:
            self.start()
        else:
            self.stop()

    def start(self):
        """
        This is a blocking method. Do not call it unless another thread is executing and will call "stop"
        on this object.
        """

        self.active = True
        self._animate()

    def stop(self):
        """ Another thread may call this in order to stop and destroy the dialog. """

        self.active = False

    def _animate(self):

        while self.active:
            self.step()
            time.sleep(.01)
        self.cancel()

def test():

    def display():
        d = InfiniteBarDialog(parent=root)
        d.start()

    root = tk.Tk()
    root.wm_title('Progress Bar')
    tk.Button(root,text='Do it...',command=display).pack()
    root.update()

    root.mainloop()

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
