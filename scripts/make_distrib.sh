#!/bin/sh
VERSION=SNAPSHOT
DISTRIB_DIR=build/hybridvis
EXAMPLE_PROJECT_DIR=${DISTRIB_DIR}/example-project
mkdir -p build
mkdir -p ${DISTRIB_DIR}
mkdir -p ${EXAMPLE_PROJECT_DIR}
mkdir -p ${EXAMPLE_PROJECT_DIR}/maps
cp README ${DISTRIB_DIR}/
cp build/jar/hybridvis-with-dependencies.jar ${DISTRIB_DIR}/
cp -r resources/displays ${DISTRIB_DIR}/
# Example project
cp data/maps/paris-hires.png ${EXAMPLE_PROJECT_DIR}/maps/paris-near.png
cp data/maps/paris-lowres2.png ${EXAMPLE_PROJECT_DIR}/maps/paris-far.png
mkdir -p ${EXAMPLE_PROJECT_DIR}/display
mkdir -p ${EXAMPLE_PROJECT_DIR}/wall-images
cp resources/displays/KonstanzWall.properties ${EXAMPLE_PROJECT_DIR}/display/
cp resources/example-projects/project.properties ${EXAMPLE_PROJECT_DIR}
# Archive
cp scripts/hybridvis.sh ${DISTRIB_DIR}/
cd build && zip -x *.swp -r hybridvis-${VERSION}.zip hybridvis && cd -
