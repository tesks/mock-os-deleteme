#!/bin/bash

lineNo=1
while IFS='' read -r line1; do
	lineNo+=1
   
    read -r line2
	lineNo+=1

	read -r line3
	lineNo+=1
    
    echo "COMPARING:"
    echo "   $line1"
    echo "   $line2"
    if [[ -n "$line3" ]]; then
    	echo "   $line3"
    	read -r line4
		lineNo+=1
    	if [[ -n "$line4" ]]; then
    		echo "ERROR at line #$lineNo"
    	fi
    fi
    bcomp $line1 $line2 $line3
done < "duplicate_java_files.txt"