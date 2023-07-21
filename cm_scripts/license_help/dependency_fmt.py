"""

Script to help generate table 3 of the MiMTaR Rev C
release plan template.

The table asks for third party dependencies names, version, license, and purpose. The first three
can be easily generated with a build tool, the fourth cannot.

This script takes two files and merges them, where both files follow a simple format of
a <key>=<value> pair on each line.

One of the files should have the license(s) of a dependency as a value, and the other should have the
text explaining the purpose of the dependency.
"""
from __future__ import print_function
import argparse


def parse_file(filename):
    '''
    Parse a file of key-value pairs, where the '=' is a separate. Handles a special case from license-maven-plugin
    outputs.

    :param filename:  the name of the file to parse
    :return: a dict of key-value pairs where the key is a dependency name
    '''
    with open(filename) as tps_file:
        all_lines = tps_file.readlines()

        line_dict = {}
        prev_key = ''
        for line in all_lines:
            # This "if" conditional is a special case for the tomcat-servlet-api, which lists its two licenses
            # one with a new line in it. This tacks the second line onto the ifrst line.
            if not '=' in line:
               line_dict[prev_key] = line_dict[prev_key] + ' ' + line.strip()
            else:
                split_line = line.strip().split('=')
                prev_key = split_line[0]
                cleaned_licenses = split_line[1].rstrip(';').replace(';', ' + ')
                line_dict[split_line[0]] = cleaned_licenses

    return line_dict


def main():
    parser = argparse.ArgumentParser(description='Produce a table of dependencies and their purposes')
    parser.add_argument('third_party_file', type=str, help="path to a file with dependency GAVs and licenses")
    parser.add_argument('dependency_purpose_file', type=str, help='file with dependency GAVs and their purposes')

    args = parser.parse_args()
    license_dict = parse_file(args.third_party_file)
    purpose_dict = parse_file(args.dependency_purpose_file)

    if len(license_dict) != len(purpose_dict):
        print ("Warning: input files had differing length")


    all_deps = set(license_dict.keys() + purpose_dict.keys())
    for dep in all_deps:
        gav = dep.split(':')
        if len(gav) < 3:
            print ('{} is a problem'.format(dep))
        print('{}:{}\t{}\t{}\t{}'.format(gav[0], gav[1], gav[2], license_dict[dep], purpose_dict[dep]))




if __name__ == '__main__':
    main()

