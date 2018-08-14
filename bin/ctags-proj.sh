#!/bin/bash
if [ -f ".gitignore" ]; then
  if [ -f ".srclist" ]; then
    ctags -R --exclude='.git' -L .srclist
  else
    ctags -R --exclude='.git' .
  fi
else
  HERE=$PWD
  cd ..
  if [ "$PWD" = "$HERE" ]; then
    echo "Got to /, have not found your project root, abort!"
    exit 1
  fi
  exec "$0"
fi
# EOF
