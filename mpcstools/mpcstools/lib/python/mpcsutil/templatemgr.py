#! /usr/bin/env python
# -*- coding: utf-8 -*-

from __future__ import (absolute_import, division, print_function)

import os
import mpcsutil.config
import mpcsutil.filesystem

class TemplateManager(object):
    """
    Convenience class to manage the lookup of templates at all levels in the GdsDirectory.
    """

    TEMPLATE_SUFFIX = ".tmpl"

    def __init__(self, template_subdir, template_suffix=TEMPLATE_SUFFIX):
        """
        template_subdir -- str naming the subdirectory of each template directory to be checked.

        Keyword Arguments:
        template_suffix -- the file extension expected for templates.  Defaults to ".tmpl"
        """

        self.config = mpcsutil.config.GdsConfig()
        self.templateSubdir = template_subdir
        self.templateSuffix = template_suffix

        self.available_templates = self._findAvailableTemplates()

    def _findAvailableTemplates(self):
        available = set()

        file_list = []

        template_dirs = [os.path.join(dir, self.templateSubdir) for dir in self.config.getTemplateDirs()]
        for template_dir in template_dirs:
            if (os.path.exists(template_dir)):
                file_list.extend([template_file for template_file in os.listdir(template_dir)])
        for file in file_list:
            if file.endswith(self.templateSuffix):
                available.add(str(file[0:-1*len(self.templateSuffix)]))

        return list(available)

    def getAvailableTemplates(self):
        """
        Returns a list of templates available in the subdirectories
        this manager is configured for. (File stem only)
        """

        return self.available_templates

    def getMostLocalTemplate(self, templateStem):
        """
        For a given template stem, return a the path to a matching file that occurs in the most "local"
        directory of the GdsDirectory (e.g., user, then FSW config, etc.).
        """

        dirList = self.config.getTemplateDirs()
        dirList = mpcsutil.filesystem.files_from_dirs(dirList, self.templateSubdir)
        return mpcsutil.filesystem.get_file(dirList, templateStem + self.templateSuffix)

def test():
    pass

def main():
    return test()

if __name__ == "__main__":
    main()

################################################################################
# vim:set sr et ts=4 sw=4 ft=python fenc=utf-8: // See Vim, :help 'modeline'
