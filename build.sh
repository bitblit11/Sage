#!/bin/bash

PRGDIR=`dirname "$PRG"`

echo "********************************************"
echo "*                                          *"
echo "*           Updating submodules            *"
echo "*                                          *"
echo "********************************************"
git submodule update --init --recursive
if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR:  Failed to update submodules.  Aborting build."
    exit -1
fi
echo ""
echo "Submodules updated."
echo ""

echo "********************************************"
echo "*                                          *"
echo "*             Building YAMCS               *"
echo "*                                          *"
echo "********************************************"
(cd thirdparty/yamcs && exec mvn install -Dmaven.test.skip=true)
if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR:  Failed to build YAMCS.  Aborting build."
    exit -1
fi
echo ""
echo "YAMCS build complete."


echo "********************************************"
echo "*                                          *"
echo "*      Building YAMCS Client Package       *"
echo "*                                          *"
echo "********************************************"
rm -Rf thirdparty/yamcs/*.tar.gz
rm -Rf thirdparty/yamcs/*.zip
(cd thirdparty/yamcs && exec ./make-client-package.sh)
if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR:  Failed to build YAMCS client package.  Aborting build."
    exit -1
fi
echo ""
echo "YAMCS client package build complete."

echo "********************************************"
echo "*                                          *"
echo "*          Building Yamcs Studio           *"
echo "*                                          *"
echo "********************************************"
(cd thirdparty/yamcs-studio && exec ./make-platform.sh)
if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR:  Failed to build Yamcs Studio platform.  Aborting build."
    exit -1
fi
echo ""
(cd thirdparty/yamcs-studio && exec ./make-product.sh)
if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR:  Failed to build Yamcs Studio product.  Aborting build."
    exit -1
fi
echo ""
echo "Yamcs Studio build complete."

echo "********************************************"
echo "*                                          *"
echo "*        Building the Sage plugins         *"
echo "*                                          *"
echo "********************************************"
(cd yamcs-cfs && exec mvn package)
if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR:  Failed to build the Yamcs-CFS provider.  Aborting build."
    exit -1
fi
echo ""
cp yamcs-cfs/target/*.jar live/lib/ext
echo "Sage plugins build complete."


echo "********************************************"
echo "*                                          *"
echo "*             Build Complete               *"
echo "*                                          *"
echo "********************************************"
