import subprocess
import sys
import numpy


class HarpApplication(object):
    def __init__(self, name):
        self.name = name
        self.hadoop_path = ''
        self.hadoop_cmd = ''
        self.harp_jar = ''
        self.class_name = ''
        self.cli = ''

    def log(self, message):
        print("[{0}] {1}".format(self.name, message))

    def config_hadoop_bin(self, hadoop_path):
        self.hadoop_path = hadoop_path
        self.hadoop_cmd = hadoop_path + ' jar'

    def config_harp_jar(self, jar_path):
        self.harp_jar = jar_path

    def args(self, *args, **kwargs):
        self.cmd = ''
        for arg in args:
            self.cmd = self.cmd + str(arg) + ' '
        self.cli = "{0} {1} {2} {3}".format(self.hadoop_cmd, self.harp_jar, self.class_name, self.cmd)
        self.log("Command: " + self.cli)

    def run(self):
        self.log("{0} is running".format(self.__class__.__name__))
        self.sub_call(self.cli)
        self.log("{0} is finished".format(self.__class__.__name__))

    def sub_call(self, command):
        try:
            return_code = subprocess.call(command, shell=True)
            if return_code < 0:
                print >> sys.stderr, "Child was terminated by signal", -return_code
        except OSError as e:
            print >> sys.stderr, "Execution failed:", e

    def print_result(self, file_path):
        fs_cmd = self.hadoop_path + ' fs -cat ' + file_path
        self.log("Command: " + fs_cmd)
        self.sub_call(fs_cmd)

    def result_to_array(self, file_path):
        cat = subprocess.Popen([self.hadoop_path, "fs", "-cat", file_path], stdout=subprocess.PIPE)
        result = ''
        for line in cat.stdout:
            result = result + line
        return numpy.fromstring(result, dtype=float, sep=' ')


class KMeansApplication(HarpApplication):
    def __init__(self, name):
        super(KMeansApplication, self).__init__(name)
        self.class_name = 'edu.iu.kmeans.regroupallgather.KMeansLauncher'


class CCDApplication(HarpApplication):
    def __init__(self, name):
        super(CCDApplication, self).__init__(name)
        self.class_name = 'edu.iu.ccd.CCDLauncher'


class LDAApplication(HarpApplication):
    def __init__(self, name):
        super(LDAApplication, self).__init__(name)
        self.class_name = 'edu.iu.lda.LDALauncher'


class SGDApplication(HarpApplication):
    def __init__(self, name):
        super(SGDApplication, self).__init__(name)
        self.class_name = 'edu.iu.sgd.SGDLauncher'


class MDSApplication(HarpApplication):
    def __init__(self, name):
        super(MDSApplication, self).__init__(name)
        self.class_name = 'edu.iu.wdamds.MDSLauncher'


class SCApplication(HarpApplication):
    def __init__(self, name):
        super(SCApplication, self).__init__(name)
        self.class_name = 'edu.iu.sahad.rotation3.SCMapCollective'
