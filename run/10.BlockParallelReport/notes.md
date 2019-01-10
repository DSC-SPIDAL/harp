learned good practices

### 1. development process 
general
+ setup a standard repo dir structure, initialization script
+ use scripts to automation everything if possible
+ start a task from a document, draft ideas, recording all activities and information in evernote
+ end a task with summary in order to make the task repeatable.  Add a new run subdirectory reo, write readme.md as a summary. 
+ frequent commit with informational commit messages
+ google doc is cool to write slides, and do the analysis of experimental results in spreadsheets, and insertable table and figures are convenient to maintain an updated version, and share with others of course.
+

coding
+ vim, add plugins for productivity,  including ndtree, ctags, and more to explore
+ an extendable TimeInfo mechanism to log timing information for different parts of the code (TimeInfo)
+ an extendable configure file and command line parser is important to test new features (xgboost:: conf file and cmd parser)
+ a configurable mechanism to run different core modules (xgboost:: updater_xxx)

compile
+ first, need to support lots of compiling flags in Makefile
+ tag the bin name by the flags to indicate the settings included and new features which is going to be tested
+ debug info and debug message version is important for debugging
+ recompiling is a frequent operation, therefore, add incremental support by building different features into different subdirectories is important to use incremental compiling effectively 

deployment 
+ avoid working with shared networking directory, such as NFS home dir. Even load .so from those places may incur performance unstable.
+ repo in ssd to accelerate compiling. 

debugging
+ LOG system, enable to insert debug message freely and no performance overhead in the release version. (LOG, debug.h)
+ asserts system, enable to raise critical error without much performance overhead (CHECK_XX)
+ use 'diffthis' in vim to compare the debug message log files for two versions is helpful.
+ gdb runtime debugging is useful sometimes, close all -O optimizations in compiling and add debug info -g

validation&testing
+ setup reference system, auto installation, versioning, compiling scripts
+ auto patch the TimeInfo code into the reference system according to the version number
+  run standard tests for the reference systems, and record the 'ground truth' 
+ auto testing scripts to validate the correctness by comparing with the 'ground truth' before starting any rigorous performance evaluation
+ log everything, including the screen log messages, the resulting model file, name it with a long tagname which can uniquely identify the running settings


### 2. New features added to the project

+ add global env variables: _sharedir_ , _gbtproject_
```
#
# project level global ENV variables
#
# sharedir is used to exchange files between different accounts on juliet cluster
#export _sharedir_ = /N/u/pengb/tmp/optgbt/
export _sharedir_ = /share/jproject/fg474/share/optgbt/

# home dir of the repo
export _gbtproject_=$DIR/../
```

