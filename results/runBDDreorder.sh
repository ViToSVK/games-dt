#!/bin/bash
for i in datasets/*; do
	echo $i
	./BDDreorder $i
done
